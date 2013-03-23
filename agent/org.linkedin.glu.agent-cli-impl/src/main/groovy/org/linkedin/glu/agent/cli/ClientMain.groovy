/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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


package org.linkedin.glu.agent.cli

import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.rest.client.AgentFactory
import org.linkedin.glu.agent.rest.client.AgentFactoryImpl
import org.linkedin.glu.groovy.utils.ExceptionJdk17Workaround
import org.linkedin.groovy.util.config.Config
import org.linkedin.groovy.util.state.StateMachine
import org.linkedin.util.lifecycle.Startable
import org.linkedin.glu.agent.api.AgentException
import org.linkedin.groovy.util.config.MissingConfigParameterException
import org.linkedin.groovy.util.log.JulToSLF4jBridge
import org.linkedin.glu.utils.tags.TagsSerializer
import org.linkedin.glu.groovy.util.state.DefaultStateMachine
import org.linkedin.glu.agent.rest.common.AgentRestUtils
import org.linkedin.glu.agent.api.TimeOutException
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution

/**
 * Command line to talk to the agent
 *
 * @author ypujante@linkedin.com
 */
class ClientMain implements Startable
{
  public static final String MODULE = ClientMain.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  public static final TagsSerializer TAGS_SERIALIZER = TagsSerializer.INSTANCE

  public static final Timespan waitCommandTimeout = Timespan.parse("10s")

  protected def config
  protected CliBuilder cli
  private AgentFactory factory
  private final StateMachine stateMachine = DefaultStateMachine.INSTANCE

  private int exitValue = 0

  ClientMain()
  {
    JulToSLF4jBridge.installBridge()
  }

  def withAgent(Closure closure)
  {
    factory.withRemoteAgent(new URI(Config.getRequiredString(config, 'url'))) { agent ->

      def async = {
        closure(agent)
      }

      def future = new FutureTaskExecution(async)

      addShutdownHook {
        future.cancel(true)
      }

      future.runAsync().get()
    }
  }

  public void start()
  {
    withAgent { agent ->
      def mountPoint = Config.getOptionalString(config, 'mountPoint', null)

      def hostActions = [
        'hostInfo', 'ps', 'kill', 'sync', 'fileContent',
        'tags', 'tag-add', 'tag-set', 'tag-remove'].findAll {
        Config.getOptionalString(config, it, null)
      }

      if(hostActions)
      {
        hostActions.each { hostAction ->
          hostAction = hostAction.replace('-', '_')
          properties."${hostAction}"(agent, config)
        }

        return
      }

      if(Config.getOptionalString(config, 'executeShellCommand', null))
      {
        executeShellCommand(agent, config)
        return
      }

      if(!mountPoint)
      {
        println agent.getMountPoints()
        return
      }

      def agentActions =
        ['interruptAction', 'installScript', 'installScriptClassname', 'clearError', 'install', 'executeAction', 'waitForState', 'start', 'uninstallScript', 'forceUninstallScript', 'uninstall'].findAll {
          Config.getOptionalString(config, it, null)
        }

      if(agentActions)
      {
        agentActions.each { agentAction ->
          properties."${agentAction}"(agent, mountPoint)
        }
      }
      else
        getState(agent, mountPoint)
    }
  }

  /**
   * Executing shell command
   */
  def executeShellCommand = { Agent agent, config ->

    boolean redirectStderr = Config.getOptionalBoolean(config, "redirectStderr", false)
    def args = [
      command: Config.getRequiredString(config, 'executeShellCommand')
    ]
    if(redirectStderr)
      args.redirectStderr = redirectStderr

    if(Config.getOptionalBoolean(config, "stdin", false))
      args.stdin = System.in

    def id = agent.executeShellCommand(args).id

    boolean completed = false

    // this will block until the command completes but will loop "regularly"
    while(!completed)
    {
      completed = waitForCommandNoTimeOutException(agent,
                                                   [
                                                     id: id,
                                                     timeout: waitCommandTimeout
                                                   ])
    }

    args = [
      id: id,
      exitValueStream: true,
      exitErrorStream: true,
      stdoutStream: true,
    ]

    if(!redirectStderr)
      args.stderrStream = true

    InputStream mis  = agent.streamCommandResults(args).stream

    exitValue = AgentRestUtils.demultiplexExecStream(mis, System.out, System.err) as int
  }

  boolean waitForCommandNoTimeOutException(Agent agent, def args)
  {
    try
    {
      agent.waitForCommand(args)
      return true
    }
    catch(TimeOutException e)
    {
      return false
    }
  }

  /******************************
   * tag related calls
   ******************************/
  // tags
  def tags = { Agent agent, config ->
    def agentTags = agent.getTags()
    if(!agentTags)
      println 'no tags'
    else
      println TAGS_SERIALIZER.serialize(agentTags.sort())
  }

  // tag-add
  def tag_add = { Agent agent, config ->
    String tagsToAdd = Config.getRequiredString(config, 'tag-add')

    Set<String> tags =
      agent.addTags(TAGS_SERIALIZER.deserialize(tagsToAdd))

    if(tags)
    {
      println "Added ${tagsToAdd} (${TAGS_SERIALIZER.serialize(tags.sort())} already present)."
    }
    else
      println "Added ${tagsToAdd}"
  }

  // tag-set
  def tag_set = { Agent agent, config ->
    String tagsToSet = Config.getRequiredString(config, 'tag-set')

    agent.setTags(TAGS_SERIALIZER.deserialize(tagsToSet))

    println "Set ${tagsToSet}"
  }

  // tag-remove
  def tag_remove = { Agent agent, config ->
    String tagsToRemove = Config.getRequiredString(config, 'tag-remove')

    Set<String> tags = agent.removeTags(TAGS_SERIALIZER.deserialize(tagsToRemove))

    if(tags)
    {
      println "Removed ${tagsToRemove} (${TAGS_SERIALIZER.serialize(tags.sort())} already removed)."
    }
    else
      println "Removed ${tagsToRemove}"
  }

  // hostInfo
  def hostInfo = { agent, config ->
    println agent.getHostInfo()
  }

  // ps
  def ps = { agent, config ->
    def procs = agent.ps()

    procs.each { k,v ->
      println "######### ${k} #########"
      println v
    }
  }

  /*
   * @params args.location which file to read the content
   * @params args.maxLine the number of lines maximum to read
   * @params args.maxSize the maximum size to read
   */
  def fileContent = { agent, config ->
    def is = agent.getFileContent(location: Config.getRequiredString(config, 'fileContent'),
                                  maxLine: Config.getOptionalString(config, 'maxLine', '10'))
    if(is == null)
      println "<< no content >>"
    else
      System.out << is
  }

  // kill
  def kill = { agent, config ->
    def pid = Config.getRequiredString(config, 'kill').split('/')
    def signal = 1
    if(pid.size() == 2)
    {
      signal = pid[1]
      pid = pid[0]
    }

    agent.kill(pid as long, signal as int)
  }

  // sync action
  def sync = { agent, config ->
    agent.sync()
  }

  // install
  def install = { agent, mountPoint ->
    agent.installScript(mountPoint: mountPoint,
                        scriptLocation: Config.getRequiredString(config, 'install'),
                        parent: Config.getOptionalString(config, 'parent', '/'),
                        initParameters: extractArgs(config))

    moveToState(agent, mountPoint, 'installed')
  }

  // uninstall
  def uninstall = { agent, mountPoint ->
    // first we move the sate machine to state uninstalled
    moveToState(agent, mountPoint, StateMachine.NONE)

    // then we uninstall the script
    agent.uninstallScript(mountPoint: mountPoint)
  }

  // start
  def start = { agent, mountPoint ->
    def scriptLocation = Config.getOptionalString(config, 'start', null)

    if(scriptLocation != 'true')
    {
      agent.installScript(mountPoint: mountPoint,
                          scriptLocation: scriptLocation,
                          parent: Config.getOptionalString(config, 'parent', '/'),
                          initParameters: extractArgs(config))
    }

    moveToState(agent, mountPoint, 'running')
  }

  protected def moveToState(agent, mountPoint, toState)
  {
    def state = agent.getState(mountPoint: mountPoint)

    if(state.error)
    {
      agent.clearError(mountPoint: mountPoint)
    }

    def path = stateMachine.findShortestPath(state.currentState, toState)

    path.each { transition ->
      agent.executeAction(mountPoint: mountPoint,
                          action: transition.action)
      agent.waitForState(mountPoint: mountPoint,
                         state: transition.to,
                         timeout: Config.getOptionalString(config, 'timeout', ''))
    }
  }

  // installScript
  def installScript = { agent, mountPoint ->
    agent.installScript(mountPoint: mountPoint,
                        scriptLocation: Config.getRequiredString(config, 'installScript'),
                        parent: Config.getOptionalString(config, 'parent', '/'),
                        initParameters: extractArgs(config))
  }

  // installScriptClassname
  def installScriptClassname = { agent, mountPoint ->
    agent.installScript(mountPoint: mountPoint,
                        scriptClassName: Config.getRequiredString(config, 'installScriptClassname'),
                        parent: Config.getOptionalString(config, 'parent', '/'),
                        initParameters: extractArgs(config))
  }

  // executeAction
  def executeAction =  { agent, mountPoint ->
    agent.executeAction(mountPoint: mountPoint,
                        action: Config.getRequiredString(config, 'executeAction'),
                        actionArgs: extractArgs(config))
  }

  // interruptAction
  def interruptAction =  { agent, mountPoint ->
    def state = agent.getState(mountPoint: mountPoint)

    if(state.transitionAction)
    {
      if(agent.interruptAction(mountPoint: mountPoint, action: state.transitionAction))
        println "Interrupted action ${state.transitionAction}"
      else
        println "Action ${state.transitionAction} completed."
    }
    else
    {
      println "Not currently executing an action"
    }
  }

  // waitForState
  def waitForState = { agent, mountPoint ->
    if(agent.waitForState(mountPoint: mountPoint,
                          state: Config.getRequiredString(config, 'waitForState'),
                          timeout: Config.getOptionalString(config, 'timeout', '')))
    {
      println 'ok'
    }
    else
    {
      println 'failure'
    }
  }

  // uninstallScript
  def uninstallScript = { agent, mountPoint ->
    agent.uninstallScript(mountPoint: mountPoint)
  }

  // forceUninstallScript
  def forceUninstallScript = { agent, mountPoint ->
    agent.uninstallScript(mountPoint: mountPoint, force: true)
  }

  // clear error
  def clearError = { agent, mountPoint ->
    agent.clearError(mountPoint: mountPoint)
  }

  // getState
  def getState = { agent, mountPoint ->
    println "${agent.getFullState(mountPoint: mountPoint)}"
  }

  protected def init(args)
  {
    cli = new CliBuilder(usage: './bin/agent-cli.sh [-h] [-f <agentConfigFile>] [-s url] ' +
                                '[-i scriptLocation] [-c classname] [-x action] [-u] [-a args] ' +
                                '[-w state] [-t timeout] [-p parentMountPoint] [-m mountPoint]')
    cli._(longOpt: 'tags', 'list the agent tags', args: 0, required: false)
    cli._(longOpt: 'tag-add', 'add the given tags', argName: 'tag1;tag2...', args: 1, required: false)
    cli._(longOpt: 'tag-remove', 'remove the given tags', argName: 'tag1;tag2...', args: 1, required: false)
    cli._(longOpt: 'tag-set', 'sets the given tags', argName: 'tag1;tag2...', args: 1, required: false)
    cli._(longOpt: 'redirectStderr', 'redirect stderr into stdout (use with -E)', args: 0, required: false)
    cli._(longOpt: 'stdin', 'provides stdin to the command (use with -E)', args: 0, required: false)
    cli.a(longOpt: 'args', 'arguments of the script or action (ex: [a:\'12\'])', args:1, required: false)
    cli.c(longOpt: 'installScriptClassname', 'install the script given a class name)', args:1, required: false)
    cli.C(longOpt: 'fileContent', 'retrieves the file content', args:1, required: false)
    cli.e(longOpt: 'executeAction', 'executes the provided action (ex: install)', args:1, required: false)
    cli.E(longOpt: 'executeShellCommand', 'executes the provided shell command', args:1, required: false)
    cli.f(longOpt: 'clientConfigFile', 'the client config file', args: 1, required: false)
    cli.F(longOpt: 'forceUninstallScript', 'force uninstall script', args: 0, required: false)
    cli.h(longOpt: 'help', 'display help')
    cli.H(longOpt: 'hostInfo', 'get infor about the host', args:0, required: false)
    cli.i(longOpt: 'installScript', 'install the script located at the location (ex: http://host/myscript.groovy)', args:1, required: false)
    cli.I(longOpt: 'install', 'shortcut for installScript + executeAction install + waitForState', args:1, required: false)
    cli.K(longOpt: 'kill', 'send a signal to a process (pid/signal)... default to HUP', args:1, required: false)
    cli.m(longOpt: 'mountPoint', 'the mount point (ex: /s)', args:1, required: false)
    cli.M(longOpt: 'maxLine', 'the number of lines maximum', args:1, required: false)
    cli.p(longOpt: 'parent', 'the parent mount point when installing (ex: /)', args:1, required: false)
    cli.P(longOpt: 'ps', 'run the ps command', args:0, required: false)
    cli.s(longOpt: 'url', 'the url to the server', args:1, required: false)
    cli.S(longOpt: 'start', 'shortcut for start', args:1, optionalArg: true, required: false)
    cli.t(longOpt: 'timeout', 'the timeout for waiting (ex: 5s)', args:1, required: false)
    cli.u(longOpt: 'uninstallScript', 'uninstall the script', args:0, required: false)
    cli.U(longOpt: 'uninstall', 'shortcut for executeAction uninstall + waitForState + uninstallScript', args:0, required: false)
    cli.v(longOpt: 'verbose', 'verbose', args: 0, required: false)
    cli.w(longOpt: 'waitForState', 'wait for the provided state', args:1, required: false)
    cli.x(longOpt: 'clearError', 'arguments of the script or action (ex: [a:\'12\'])', args:0, required: false)
    cli.X(longOpt: 'interruptAction', 'interrupts the current action', args:0, required: false)
    cli.y(longOpt: 'sync', 'sync', args: 0, required: false)

    def options = cli.parse(args)
    if(!options)
    {
      return
    }

    if(options.h)
    {
      cli.usage()
      return
    }
    config = getConfig(cli, options)
    factory = AgentFactoryImpl.create(config)
    return options
  }


  static void main(args)
  {
    ExceptionJdk17Workaround.installWorkaround()

    ClientMain clientMain = new ClientMain()
    def options = clientMain.init(args)

    try
    {
      clientMain.start()
    }
    catch (MissingConfigParameterException e)
    {
      println e
      clientMain.cli.usage()
    }
    catch(AgentException e)
    {
      if(log.isDebugEnabled())
      {
        log.debug("AgentException", e)
      }
      System.err.println("Error: ${e.message}")
      if(options.v)
      {
        e.printStackTrace(System.err)
      }
      System.exit(1)
    }

    System.exit(clientMain.exitValue)
  }

  protected def extractArgs(def config)
  {
    return extractArgs(Config.getOptionalString(config, 'args', '[:]'))
  }

  protected def extractArgs(String args)
  {
    return new GroovyShell().evaluate(args)
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

    if(new URI(properties.url).scheme == 'http')
      properties.sslEnabled = false

    return properties
  }
}
