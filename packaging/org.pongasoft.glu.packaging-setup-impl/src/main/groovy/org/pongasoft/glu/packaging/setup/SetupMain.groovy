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

/**
 * @author yan@pongasoft.com  */
public class SetupMain
{
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
    cli._(longOpt: 'gen-keys', 'generate the keys', args: 0, required: false)
    cli._(longOpt: 'out', 'output directory', args: 1, required: false)
    cli._(longOpt: 'quiet', 'do not ask any question (use defaults)', args: 0, required: false)
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

  public void start()
  {
    quiet = config.containsKey('quiet')

    String out = Config.getOptionalString(config, 'out', null)

    if(!out)
    {
      out = promptForValue("Enter the output directory",
                           System.getProperty('user.pwd', System.getProperty('user.dir')))
    }

    outputFolder = FileResource.create(new File(out))

    def actions = [
      'gen-keys'].findAll {
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
    def kmm = new KeysGenerator(shell: shell,
                                outputFolder: outputFolder.createRelative('keys'),
                                masterPassword: new String(masterPassword)).generateKeys().toExternalRepresentation()
    println "Keys have been generated in the following folder: ${outputFolder.path}"

    println "Copy the following section in your meta model (see comment in meta model)"

    println "//" * 20

    println "keys: ["
    kmm.each { storeName, store ->
      println "  ${storeName}: ["
      println store.findAll {k, v -> v != null}.collect { k, v -> "    ${k}: '${v}'"}.join(',\n')
      println "  ],"
    }
    println "]"

    println "//" * 20

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
    }
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
      }
    }

    return properties
  }

}