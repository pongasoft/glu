
/*
 * Copyright (c) 2010-2011 LinkedIn, Inc
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

import org.linkedin.glu.agent.api.ShellExecException

class JettyGluScript
{
  static requires = {
    agent(version: '1.6.0')
  }

  def version = '@script.version@'
  def serverRoot
  def serverCmd
  def logsDir
  def containerLog
  def config
  def pid
  def port
  def wars

  def install = {
    log.info "Installing..."

    // fetching/installing jetty
    def jettySkeleton = shell.fetch(params.skeleton)
    def distribution = shell.untar(jettySkeleton)
    shell.rmdirs(mountPoint)
    serverRoot = shell.mv(shell.ls(distribution)[0], mountPoint)

    // assigning variables
    logsDir = serverRoot.'logs'
    containerLog = logsDir.'jetty.log'
    def serverScript = serverRoot.'bin/jetty.sh'
    serverCmd = "JETTY_RUN=${logsDir.file} ${serverScript.file}"

    shell.rmdirs(serverRoot.'contexts')
    shell.rmdirs(serverRoot.'webapps')

    // make sure that the shell script is executable
    shell.chmodPlusX(serverScript)

    // creating etc/jetty-glu.xml
    shell.saveContent(serverRoot.'etc/jetty-glu.xml', DEFAULT_JETTY_XML)

    log.info "Install complete."
  }

  def configure = { args ->
    log.info "Configuring..."

    port = (params.port ?: 8080) as int

    def c = []

    c << DEFAULT_JETTY_CONFIG
    c << '--pre=etc/jetty-logging.xml'
    c << '--daemon'
    c << '\n' // forces an empty line

    config = shell.saveContent(serverRoot.'start.ini', c.join('\n'))

    // setting up a timer to monitor the container
    timers.schedule(timer: containerMonitor,
                    repeatFrequency: args?.containerMonitorRepeatFrequency ?: '5s')

    log.info "Configuration complete."
  }

  def start = {
    log.info "Starting..."

    pid = isServerUp()

    if(pid)
    {
      log.info "Server already up."
    }
    else
    {
      // we execute the start command (return right away)
      String cmd = "JAVA_OPTIONS=\"-Xmx1024m -Djetty.port=${port}\" ${serverCmd} start > /dev/null 2>&1 &"
      println config.file.text
      shell.exec(cmd)
      shell.saveContent(logsDir.'jetty.cmd', cmd)

      // we wait for the process to be started (should be quick)
      shell.waitFor(timeout: '5s', heartbeat: '250') {
        pid = isServerUp()
      }

//      // we now wait for the war to be up: we use a jmx call to do that
//      shell.waitFor(timeout: args?.startTimeout, heartbeat: '1s') { duration ->
//        log.info "${duration}: Waiting for server to be up"
//
//        // we check if the server is down already... in which case we throw an exception
//        if(isServerDown())
//          shell.fail("Container could not start. Check the log file for errors.")
//
//        return checkServices()
//      }
    }

    log.info "Started jetty on port ${port}."
  }

  def stop = { args ->
    log.info "Stopping..."

    if(isServerDown())
    {
      log.info "Server already down."
    }
    else
    {
      // invoke the stop command
      shell.exec("${serverCmd} stop")

      // we wait for the process to be stopped
      shell.waitFor(timeout: args?.stopTimeout, heartbeat: '1s') { duration ->
        log.info "${duration}: Waiting for server to be down"
        isServerDown()
      }
    }

    pid = null

    log.info "Stopped."
  }

  def unconfigure = {
    log.info "Unconfiguring..."

    timers.cancel(timer: containerMonitor)

    port = null

    log.info "Unconfiguration complete."
  }

  def uninstall = {
    // nothing
  }

  private def isServerUp()
  {
    try
    {
      def output = shell.exec("${serverCmd} check")
      def matcher = output =~ /Jetty running pid=([0-9]+)/
      if(matcher && shell.listening('localhost', port))
        return matcher[0][1]
      else
        return null
    }
    catch(ShellExecException e)
    {
      return null
    }
  }

  private def isServerDown()
  {
    !isServerUp()
  }

  private boolean checkServices()
  {
    // for now returns true
    return true
  }

  /**
   * Check that both container and services are up
   */
  private def checkContainerAndServices = {
    def up = [container: false, services: false]

    pid = isServerUp()
    up.container = pid != null
    if(up.container)
      up.services = checkServices()

    return up
  }

  /**
   * Defines the timer that will check for the container to be up and running and will act
   * according if not (change state)
   */
  def containerMonitor = {
    try
    {
      def up = checkContainerAndServices()

      def newState = null
      def error = null

      if(stateManager.state.currentState == 'running')
      {
        if(!up.container || !up.services)
        {
          newState = 'stopped'
          if(!up.container)
          {
            pid = null
            error = 'Container down detected. Check the log file for errors.'
          }
          else
            error = 'Container is up but services seem to be down. Check the log file for errors.'

          log.warn "${error} => forcing new state ${newState}"
        }
      }
      else
      {
        if(up.container && up.services)
        {
          newState = 'running'
          log.info "Container up detected."
        }
      }

      if(newState)
        stateManager.forceChangeState(newState, error)

      if(log.isDebugEnabled())
        log.debug "Container Monitor: ${stateManager.state.currentState} / ${up}"

      log.info "Container Monitor: ${stateManager.state.currentState} / ${up}"

    }
    catch(Throwable th)
    {
      log.warn "Exception while running containerMonitor: ${th.message}"
      if(log.isDebugEnabled())
        log.debug("Exception while running containerMonitor (ignored)", th)
    }
  }

  static String DEFAULT_JETTY_CONFIG = """
OPTIONS=Server,jsp,jmx,resources,websocket,ext
etc/jetty-glu.xml
etc/jetty-deploy.xml
etc/jetty-webapps.xml
etc/jetty-contexts.xml
"""

  /**
   * The 'default' file etc/jetty.xml contains a lot of hardcoded values and does not use
   * SystemProperty which is a bug because jetty.sh use -Djetty.port for the port! This script
   * will create a etc/jetty-glu.xml and use it instead
   */
  static String DEFAULT_JETTY_XML = """<?xml version="1.0"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure.dtd">
<Configure id="Server" class="org.eclipse.jetty.server.Server">
    <Set name="ThreadPool">
      <!-- Default queued blocking threadpool -->
      <New class="org.eclipse.jetty.util.thread.QueuedThreadPool">
        <Set name="minThreads"><SystemProperty name="jetty.minThreads" default="10"/></Set>
        <Set name="maxThreads"><SystemProperty name="jetty.maxThreads" default="200"/></Set>
      </New>
    </Set>
    <Call name="addConnector">
      <Arg>
          <New class="org.eclipse.jetty.server.nio.SelectChannelConnector">
            <Set name="host"><SystemProperty name="jetty.host" /></Set>
            <Set name="port"><SystemProperty name="jetty.port" default="8080"/></Set>
            <Set name="maxIdleTime"><SystemProperty name="jetty.maxIdleTime" default="300000"/></Set>
            <Set name="Acceptors"><SystemProperty name="jetty.Acceptors" default="2"/></Set>
            <Set name="statsOn"><SystemProperty name="jetty.statsOn" default="false"/></Set>
            <Set name="confidentialPort"><SystemProperty name="jetty.confidentialPort" default="8443"/></Set>
	          <Set name="lowResourcesConnections"><SystemProperty name="jetty.lowResourcesConnections" default="20000"/></Set>
	          <Set name="lowResourcesMaxIdleTime"><SystemProperty name="jetty.lowResourcesMaxIdleTime" default="5000"/></Set>
          </New>
      </Arg>
    </Call>
    <Set name="handler">
      <New id="Handlers" class="org.eclipse.jetty.server.handler.HandlerCollection">
        <Set name="handlers">
         <Array type="org.eclipse.jetty.server.Handler">
           <Item>
             <New id="Contexts" class="org.eclipse.jetty.server.handler.ContextHandlerCollection"/>
           </Item>
           <Item>
             <New id="DefaultHandler" class="org.eclipse.jetty.server.handler.DefaultHandler"/>
           </Item>
         </Array>
        </Set>
      </New>
    </Set>
    <Set name="stopAtShutdown"><SystemProperty name="jetty.stopAtShutdown" default="true"/></Set>
    <Set name="sendServerVersion"><SystemProperty name="jetty.sendServerVersion" default="true"/></Set>
    <Set name="sendDateHeader"><SystemProperty name="jetty.sendDateHeader" default="true"/></Set>
    <Set name="gracefulShutdown"><SystemProperty name="jetty.gracefulShutdown" default="1000"/></Set>

</Configure>
"""
}