/*
 * Copyright (c) 2013 Yan Pujante
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.pongasoft.glu.packaging.setup

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.glu.groovy.utils.shell.ShellImpl
import org.linkedin.groovy.util.config.Config
import org.linkedin.groovy.util.config.MissingConfigParameterException
import org.linkedin.groovy.util.log.JulToSLF4jBridge
import org.linkedin.util.io.resource.FileResource
import org.linkedin.util.io.resource.Resource
import org.linkedin.util.io.resource.ResourceChain
import org.pongasoft.glu.provisioner.core.metamodel.impl.builder.GluMetaModelBuilder

/**
 * @author yan@pongasoft.com  */
public class SetupMain
{
  public static class AbortException extends Exception
  {
    int exitValue = 0

    AbortException(String message, int exitValue)
    {
      super(message)
      this.exitValue = exitValue
    }
  }

  protected def config
  protected CliBuilder cli
  protected boolean quiet = false
  protected Resource outputFolder
  protected Shell shell = ShellImpl.createRootShell()

  SetupMain()
  {
    JulToSLF4jBridge.installBridge()
  }

  protected def init(args)
  {
    cli = new CliBuilder(usage: './bin/setup.sh [-h]')
    cli.d(longOpt: 'gen-dist', 'generate the distribution', args: 0, required: false)
    cli.k(longOpt: 'gen-keys', 'generate the keys', args: 0, required: false)
    cli.m(longOpt: 'meta-model', 'location of the meta model (multiple allowed)', args: 1, required: false)
    cli._(longOpt: 'configs-root', "location of the configs (multiple allowed) [default: ${defaultConfigsResource}]", args: 1, required: false)
    cli._(longOpt: 'packages-root', "location of the packages [default: ${defaultPackagesRootResource}]", args: 1, required: false)
    cli._(longOpt: 'keys-root', "location of the keys (if relative) [default: <outputFolder>/keys]", args: 1, required: false)
    cli._(longOpt: 'glu-root', "location of glu distribution [default: ${gluRootResource}]", args: 1, required: false)
    cli.o(longOpt: 'outputFolder', 'output folder', args: 1, required: false)
    cli._(longOpt: 'quiet', 'do not ask any question (use defaults)', args: 0, required: false)
    cli.f(longOpt: 'setupConfigFile', 'the setup config file', args: 1, required: false)
    cli.h(longOpt: 'help', 'display help')

    def options = cli.parse(args)
    if(!options)
    {
      return
    }

    if(options.h)
    {
      cli.usage()
      return null
    }
    config = getConfig(cli, options)
    return options
  }

  /**
   * Prompts for a value (with a default value... if enter is hit then the default value
   * is returned. If quiet mode, do not prompt.
   */
  public String promptForValue(String message, def defaultValue)
  {
    if(quiet)
    {
      println "${message} [${defaultValue}]: ${defaultValue}"
      return defaultValue?.toString()
    }
    else
    {
      String value = System.console().readLine("${message} [${defaultValue}]: ")
      if(!value)
        return defaultValue?.toString()
      else
        return value
    }
  }

  protected Resource getGluRootResource()
  {
    def gluRoot = Config.getOptionalString(config,
                                           'glu-root',
                                           userDirResource.createRelative('../..').file.canonicalPath)

    FileResource.create(gluRoot)
  }

  protected String getUserDir()
  {
    System.getProperty('user.dir')
  }

  protected Resource getUserDirResource()
  {
    FileResource.create(userDir)
  }

  protected Resource getDefaultConfigsResource()
  {
    userDirResource.createRelative('configs')
  }

  protected Resource getDefaultPackagesRootResource()
  {
    gluRootResource.createRelative('packages')
  }

  public void start()
  {
    quiet = config.containsKey('quiet')

    String out = Config.getOptionalString(config, 'outputFolder', null)

    if(!out)
    {
      out = promptForValue("Enter the output directory",
                           System.getProperty('user.pwd', userDir))
    }

    outputFolder = FileResource.create(new File(out))

    def actions = [
      'gen-keys', 'gen-dist'].findAll {
      Config.getOptionalString(config, it, null)
    }

    // execute each action
    actions.each { action ->
      action = action.replace('-', '_')
      properties."${action}"()
    }

  }

  def gen_keys = {
    println "Generating keys..."
    char[] masterPassword = System.console().readPassword("Enter a master password:")
    def km = new KeysGenerator(shell: shell,
                               outputFolder: outputFolder.createRelative('keys'),
                               masterPassword: new String(masterPassword))
    try
    {
      def kmm = km.generateKeys().toExternalRepresentation()

      println "Keys have been generated in the following folder: ${outputFolder.path}"

      println "Copy the following section in your meta model (see comment in meta model)"

      println "//" * 20

      println "def keys = ["
      kmm.each { storeName, store ->
        println "  ${storeName}: ["
        println store.findAll {k, v -> v != null}.collect { k, v -> "    ${k}: '${v}'"}.join(',\n')
        println "  ],"
      }
      println "]"

      println "//" * 20
    }
    catch(IllegalStateException e)
    {
      throw new AbortException("${e.message} => if you want to generate new keys, either provide another folder or delete them first",
                               1)
    }
  }

  def gen_dist = {
    println "Generating distributions"

    // meta model
    def metaModels = config.'meta-models'
    if(!metaModels)
      throw new AbortException("--meta-model <arg> required", 2)

    GluMetaModelBuilder builder = new GluMetaModelBuilder()
    metaModels.each { String metaModel ->
      builder.deserializeFromJsonResource(FileResource.create(metaModel))
    }

    // configsRoots
    def configsRoots = config.'configs-roots' ?: ['<default>']
    configsRoots = configsRoots.collect { String configRoot ->
      if(configRoot == '<default>')
        defaultConfigsResource
      else
        FileResource.create(configRoot)
    }
    configsRoots = ResourceChain.create(configsRoots)

    // packagesRoot
    def packagesRoot = Config.getOptionalString(config,
                                                'packages-root',
                                                defaultPackagesRootResource.file.canonicalPath)

    // keysRoot
    def keysRoot = Config.getOptionalString(config,
                                            'keys-root',
                                            outputFolder.createRelative('keys').file.canonicalPath)

    def packager = new GluPackager(shell: shell,
                                   configsRoot: configsRoots,
                                   packagesRoot: FileResource.create(packagesRoot),
                                   outputFolder: outputFolder,
                                   keysRoot: FileResource.create(keysRoot),
                                   gluMetaModel: builder.toGluMetaModel())

    packager.packageAll()

  }

  public static void main(String[] args)
  {
    SetupMain clientMain = new SetupMain()
    def options = clientMain.init(args)

    if(options)
    {
      try
      {
        clientMain.start()
      }
      catch (MissingConfigParameterException e)
      {
        println e
        clientMain.cli.usage()
      }
      catch(AbortException e)
      {
        System.err.println(e.message)
        System.exit(e.exitValue)
      }
    }

    System.exit(0)
  }

  protected def getConfig(cli, options)
  {
    Properties properties = new Properties()

    if(options.f)
    {
      new File(options.f).withInputStream {
        properties.load(it)
      }
    }

    cli.options.options.each { option ->
      if(options.hasOption(option.longOpt))
      {
        properties[option.longOpt] = options[option.longOpt]
        def collectionOptionName = "${option.longOpt}s".toString()
        def array = options."${collectionOptionName}"
        if(array)
          properties[collectionOptionName] = array
      }
    }

    return properties
  }

}