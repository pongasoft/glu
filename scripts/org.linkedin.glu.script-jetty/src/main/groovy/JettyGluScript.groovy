
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
import javax.management.ObjectName
import javax.management.InstanceNotFoundException

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
  def pid
  def port
  def webapps

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
    serverCmd = "JETTY_RUN=${logsDir.file} ${serverRoot.'bin/jetty.sh'.file}"

    shell.rmdirs(serverRoot.'contexts')
    shell.rmdirs(serverRoot.'webapps')

    // make sure all bin/*.sh files are executable
    shell.ls(serverRoot.bin) {
      include(name: '*.sh')
    }.each { shell.chmodPlusX(it) }

    // creating etc/jetty-glu.xml
    shell.saveContent(serverRoot.'etc/jetty-glu.xml', DEFAULT_JETTY_XML)

    log.info "Install complete."
  }

  def configure = { args ->
    log.info "Configuring..."

    // first we configure the server
    configureServer()

    // second we configure the apps
    configureWebapps()

    // setting up a timer to monitor the container
    timers.schedule(timer: containerMonitor,
                    repeatFrequency: args?.containerMonitorRepeatFrequency ?: '5s')

    log.info "Configuration complete."
  }

  def start = { args ->
    log.info "Starting..."

    pid = isServerUp()

    if(pid)
    {
      log.info "Server already up."
    }
    else
    {
      // we execute the start command (return right away)
      String cmd = "JAVA_OPTIONS=\"-Djetty.port=${port} -Dcom.sun.management.jmxremote\" ${serverCmd} start > /dev/null 2>&1 &"
      shell.exec(cmd)
      shell.saveContent(logsDir.'jetty.cmd', cmd)

      // we wait for the process to be started (should be quick)
      shell.waitFor(timeout: '5s', heartbeat: '250') {
        pid = isServerUp()
      }

      // we now wait for the war to be up: we use a jmx call to do that
      shell.waitFor(timeout: args?.startTimeout, heartbeat: '1s') { duration ->
        log.info "${duration}: Waiting for server to be up"

        // we check if the server is down already... in which case we throw an exception
        if(isServerDown())
          shell.fail("Container could not start. Check the log file for errors.")

        return checkWebapps() == 'ok'
      }
    }

    log.info "Started jetty on port ${port}."
  }

  def stop = { args ->
    log.info "Stopping..."

    doStop()

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

  private def doStop = {
    if(isServerDown())
    {
      log.info "Server already down."
    }
    else
    {
      // invoke the stop command
      shell.exec("${serverCmd} stop")

      // we wait for the process to be stopped
      shell.waitFor(timeout: params?.stopTimeout, heartbeat: '1s') { duration ->
        log.info "${duration}: Waiting for server to be down"
        isServerDown()
      }
    }

    pid = null
  }

  /**
   * use jmx to fetch status of webapps installed
   * @return map where the key is the context path and the value is a <code>boolean</code>
   *         (<code>true</code> means failed)
   */
  private Map<String, Boolean> getJmxInfo()
  {
    Map<String, Boolean> values = [:]

    shell.withMBeanServerConnection(pid) { connection ->
      if(connection)
      {
        try
        {
          def contexts =
            connection.getAttribute('org.eclipse.jetty.server:type=server,id=0',
                                    'contexts')?.toList()

          contexts.each { ObjectName contextName ->
            Map<String,Object> attributes = connection.getAttributes(contextName,
                                                                    ['contextPath', 'failed'])
            values[attributes.contextPath] = attributes.failed as boolean
          }
        }
        catch (InstanceNotFoundException e)
        {
          log.debug("Could not find mbean", e)
          println e
          values = [:]
        }
      }
    }

    return values
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

  private def configureServer = {
    port = (params.port ?: 8080) as int
    def c = []
    c << DEFAULT_JETTY_CONFIG
    c << 'etc/jetty-jmx.xml'
    c << '--pre=etc/jetty-logging.xml'
    c << '--daemon'
    c << '\n' // forces an empty line
    shell.saveContent(serverRoot.'start.ini', c.join('\n'))
  }

  private def configureWebapps = {
    def w = params.webapps ?: []

    // case when only one webapp provided as a map
    if(w instanceof Map)
      w = [w]

    def ws = [:]

    w.each {
      def webapp = configureWebapp(it)
      if(ws.containsKey(webapp.contextPath))
        shell.fail("deplicate contextPath ${webapp.contextPath}")
      ws[webapp.contextPath] = webapp
    }

    webapps = ws
  }

  private def configureWebapp = { webapp ->
    if(webapp.war)
      configureWar(webapp)
    else
    {
      if(webapp.resources)
        configureResources(webapp)
      else
        shell.fail("cannot configure webapp: ${webapp}")
    }
  }

  private def configureWar = { webapp ->
    String contextPath = (webapp.contextPath ?: '/').toString()
    String name = contextPath.replace('/', '_')

    def war = shell.fetch(webapp.war, serverRoot."wars/${name}.war")

    def context = shell.saveContent(serverRoot."contexts/${name}.xml",
                                    WAR_CONTEXT,
                                    ['war.localWar': war.file.canonicalPath,
                                    'war.contextPath': contextPath])

    return [
      remoteWar: webapp.war,
      localWar: war,
      contextPath: contextPath,
      context: context,
      monitor: webapp.monitor
    ]
  }

  private def configureResources = { webapp ->

  }

  /**
   * @return a map of failed apps. The map is empty if there is none. The key is the context path
   * and the value can be 'init' (when failed during the init process),
   * 'busy' or 'dead' when failed during monitoring or 'unknown', if in the process of being deployed
   */
  private Map<String, String> getFailedWebapps()
  {
    Map<String, String> failedWebapps = [:]

    // when no webapps at all there is no need to talk to the server
    if(!webapps)
      return failedWebapps

    webapps.keySet().each { failedWebapps[it] = 'unknown'}

    Map<String, Boolean> jmxInfo = getJmxInfo()

    jmxInfo.each { String contextPath, boolean failed ->
      if(failed)
        failedWebapps[contextPath] = 'init'
      else
      {
        failedWebapps.remove(contextPath)

        def monitor = webapps[contextPath]?.monitor
        if(monitor)
        {
          try
          {
            int code =
              shell.httpHead("http://localhost:${port}${contextPath}${monitor}").responseCode

            switch(code)
            {
              case 503:
                failedWebapps[contextPath] = 'busy'
                break

              case 500:
                failedWebapps[contextPath] = 'dead'
                break
            }
          }
          catch(IOException e)
          {
            log.debug("Could not talk to ${contextPath}", e)
            failedWebapps[contextPath] = 'dead'
          }
        }
      }
    }

    println "failedWebapps=${failedWebapps}"

    return failedWebapps
  }

  /**
   * @return 'ok' if all apps are good, 'dead' if any app is dead, 'busy' if any app is busy,
   *         otherwise 'unknown' (which is when the apps are in the process of being deployed)
   */
  private String checkWebapps()
  {
    def failedApps = getFailedWebapps()

    if(failedApps.isEmpty())
      return 'ok'

    if(failedApps.values().find { it == 'init' || it == 'dead' })
    {
      log.warn ("Failed apps: ${failedApps}. Shutting down container...")
      doStop()
      return 'dead'
    }
    else
    {
      if(failedApps.values().find { it == 'busy'} )
        return 'busy'
    }

    return 'unknown'
  }

  /**
   * Check that both container and webapps are up
   */
  private def checkContainerAndWebapps = {
    def up = [container: false, webapps: 'unknown']

    pid = isServerUp()
    up.container = pid != null
    if(up.container)
      up.webapps = checkWebapps()

    return up
  }

  /**
   * Defines the timer that will check for the container to be up and running and will act
   * according if not (change state)
   */
  def containerMonitor = {
    try
    {
      def up = checkContainerAndWebapps()

      def currentState = stateManager.state.currentState
      def currentError = stateManager.state.error

      def newState = null
      def newError = null

      // case when current state is running
      if(currentState == 'running')
      {
        if(!up.container || up.webapps == 'dead')
        {
          newState = 'stopped'
          pid = null
          newError = 'Container down detected. Check the log file for errors.'
          log.warn "${newError} => forcing new state ${newState}"
        }
        else
        {
          if(up.webapps == 'busy')
          {
            newState = 'running' // remain running
            newError = 'Container is up but webapps seem to be down. Check the log file for errors.'
            log.warn newError
          }
          else
          {
            if(up.webapps == 'ok' && currentError)
            {
              newState = 'running' // remain running
              log.info "All webapps are up, clearing error status."
            }
          }
        }
      }
      else
      {
        if(up.container && up.webapps == 'ok')
        {
          newState = 'running'
          log.info "Container up detected."
        }
      }

      if(newState)
        stateManager.forceChangeState(newState, newError)

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

  static String WAR_CONTEXT = """<?xml version="1.0"  encoding="ISO-8859-1"?>
<!DOCTYPE Configure PUBLIC "-//Mort Bay Consulting//DTD Configure//EN" "http://jetty.eclipse.org/configure.dtd">
<Configure class="org.eclipse.jetty.webapp.WebAppContext">
  <Call class="org.eclipse.jetty.util.log.Log" name="debug"><Arg>Configure war=@war.localWar@ contextPath=@war.contextPath@</Arg></Call>
  <Set name="contextPath">@war.contextPath@</Set>
  <Set name="war">@war.localWar@</Set>
</Configure>
"""
}