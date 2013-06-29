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
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.groovy.util.log.JulToSLF4jBridge
import org.linkedin.util.clock.Timespan
import org.linkedin.util.io.resource.FileResource
import org.linkedin.util.io.resource.Resource
import org.linkedin.util.io.resource.ResourceChain
import org.linkedin.zookeeper.cli.commands.UploadCommand
import org.linkedin.zookeeper.client.ZKClient
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperClusterMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.impl.builder.GluMetaModelBuilder

import java.util.concurrent.TimeoutException

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
    cli.d(longOpt: 'gen-dist', 'generate the distributions', args: 0, required: false)
    cli.k(longOpt: 'gen-keys', 'generate the keys', args: 0, required: false)
    cli.z(longOpt: 'configure-zookeeper-clusters', 'configure all zookeeper clusters', args: 0, required: false)
    cli.m(longOpt: 'meta-model', 'location of the meta model (multiple allowed)', args: 1, required: false)
    cli._(longOpt: 'configs-root', "location of the configs (multiple allowed) [default: ${defaultConfigsResource}]", args: 1, required: false)
    cli._(longOpt: 'packages-root', "location of the packages [default: ${defaultPackagesRootResource}]", args: 1, required: false)
    cli._(longOpt: 'keys-root', "location of the keys (if relative) [default: <outputFolder>/keys]", args: 1, required: false)
    cli._(longOpt: 'glu-root', "location of glu distribution [default: ${gluRootResource}]", args: 1, required: false)
    cli._(longOpt: 'agents-only', "generate distribution for agents only", args: 0, required: false)
    cli._(longOpt: 'consoles-only', "generate distribution for consoles only", args: 0, required: false)
    cli._(longOpt: 'zookeeper-clusters-only', "generate distribution for ZooKeeper clusters only", args: 0, required: false)
    cli.o(longOpt: 'output-folder', 'output folder', args: 1, required: false)
    cli._(longOpt: 'quiet', 'do not ask any question (use defaults)', args: 0, required: false)
    cli.f(longOpt: 'setup-config-file', 'the setup config file', args: 1, required: false)
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

    String out = Config.getOptionalString(config, 'output-folder', null)

    if(!out)
    {
      out = promptForValue("Enter the output directory",
                           System.getProperty('user.pwd', userDir))
    }

    outputFolder = FileResource.create(new File(out))

    def actions = [
      'gen-keys', 'gen-dist', 'configure-zookeeper-clusters'].findAll {
      Config.getOptionalString(config, it, null)
    }

    // execute each action
    actions.each { action ->
      action = action.replace('-', '_')
      properties."${action}"()
    }

  }

  /**
   * --gen-keys command
   */
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

  /**
   * --gen-dist command
   */
  def gen_dist = {
    println "Generating distributions"

    def packager = buildPackager(false)

    if(Config.getOptionalBoolean(config, 'agents-only', false) ||
       Config.getOptionalBoolean(config, 'consoles-only', false) ||
       Config.getOptionalBoolean(config, 'zookeeper-clusters-only', false))
    {
      if(Config.getOptionalBoolean(config, 'agents-only', false))
        packager.packageAgents()
      if(Config.getOptionalBoolean(config, 'consoles-only', false))
        packager.packageConsoles()
      if(Config.getOptionalBoolean(config, 'zookeeper-clusters-only', false))
        packager.packageZooKeeperClusters()
    }
    else
    {
      packager.packageAll()
    }
  }

  /**
   * --configure-zookeeper-clusters command
   */
  def configure_zookeeper_clusters = {
    println "Configuring ZooKeeper clusters"

    def packager = buildPackager(true)

    packager.packageZooKeeperClusters()

    def artifacts =
      packager.packagedArtifacts.findAll { k, v -> k instanceof ZooKeeperClusterMetaModel}

    artifacts.each { ZooKeeperClusterMetaModel model, def pas ->
      configureZooKeeperCluster(model, pas.zooKeeperCluster.location)
    }

  }

  protected void configureZooKeeperCluster(ZooKeeperClusterMetaModel model,
                                           Resource location)
  {
    println "Configuring ZooKeeper cluster [${model.name}]"

    def zkClient = new ZKClient(model.zooKeeperConnectionString,
                                Timespan.parse("5s"),
                                null)

    zkClient.start()

    try
    {
      zkClient.waitForStart(Timespan.parse('10s'))

      GroovyIOUtils.eachChildRecurse(location.createRelative('conf').chroot('.')) { Resource child ->
        if(!child.isDirectory())
        {
          println "uploading ${child.path} to ${model.zooKeeperConnectionString}"
          UploadCommand cmd = new UploadCommand()
          if(cmd.execute(zkClient, ['-f', child.file.canonicalPath, child.path]) != 0)
            throw new AbortException("Error while uploading to ZooKeeper cluster [${model.zooKeeperConnectionString}]", 3)
        }
      }
    }
    catch(TimeoutException ignored)
    {
      throw new AbortException("could not connect to ZooKeeper [${model.zooKeeperConnectionString}]", 4)
    }
    finally
    {
      zkClient.destroy()
    }

  }

  protected GluMetaModel loadGluMetaModel()
  {
    def metaModels = config.'meta-models'
    if(!metaModels)
      throw new AbortException("--meta-model <arg> required", 2)

    GluMetaModelBuilder builder = new GluMetaModelBuilder()
    metaModels.each { String metaModel ->
      builder.deserializeFromJsonResource(FileResource.create(metaModel))
    }

    builder.toGluMetaModel()
  }

  protected GluPackager buildPackager(boolean dryMode)
  {
    // meta model
    GluMetaModel gluMetaModel = loadGluMetaModel()

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

    new GluPackager(shell: shell,
                    configsRoot: configsRoots,
                    packagesRoot: FileResource.create(packagesRoot),
                    outputFolder: outputFolder,
                    keysRoot: FileResource.create(keysRoot),
                    gluMetaModel: gluMetaModel,
                    dryMode: dryMode)
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