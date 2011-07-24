/*
 * Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.scripts.testFwk

import org.linkedin.glu.agent.impl.storage.RAMStorage
import org.linkedin.glu.agent.impl.storage.Storage
import org.linkedin.glu.agent.impl.capabilities.ShellImpl
import org.linkedin.glu.agent.impl.AgentImpl
import org.linkedin.glu.agent.impl.storage.AgentProperties
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.glu.agent.api.Shell
import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory
import org.linkedin.glu.agent.impl.script.ScriptNode
import org.linkedin.glu.agent.api.ScriptExecutionException
import org.linkedin.glu.agent.api.ScriptExecutionCauseException
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock

/**
 * @author yan@pongasoft.com */
public class GluScriptBaseTest extends GroovyTestCase
{
  def logFileSystem
  def rootShell
  def shell
  def appsFileSystem
  AgentImpl agentImpl
  Clock clock = SystemClock.instance()

  def ramStorage = [:]

  Map initParameters = [:]
  Map actionArgs = [:]

  protected void setUp()
  {
    super.setUp();

    // the agent is logging for each script... we don't want the output in the test
    // Log.setFactory(new RAMLogFactory())

    logFileSystem = createLogFileSystem()
    appsFileSystem = createAppsFileSystem()
    shell = createShell()
    rootShell = createRootShell()
    agentImpl =  createAgent()
    agentImpl.boot(agentBootArgs)
  }

  protected void tearDown()
  {
    try
    {
      logFileSystem.destroy()
      appsFileSystem.destroy()
      agentImpl.shutdown()
      agentImpl.waitForShutdown(0)
    }
    finally
    {
      super.tearDown()
    }
  }

  protected def createLogFileSystem()
  {
    FileSystemImpl.createTempFileSystem()
  }

  protected def createAppsFileSystem()
  {
    FileSystemImpl.createTempFileSystem()
  }

  protected Storage createStorage()
  {
    new RAMStorage(ramStorage)
  }

  protected Shell createShell()
  {
    new ShellImpl(fileSystem: appsFileSystem, agentProperties: createAgentProperties())
  }

  protected Shell createRootShell()
  {
    new ShellImpl(fileSystem: logFileSystem)
  }

  protected AgentProperties createAgentProperties()
  {
    new AgentProperties()
  }

  protected AgentImpl createAgent()
  {
    new AgentImpl(clock: clock)
  }

  protected Agent getAgent()
  {
    agentImpl
  }

  protected Shell getShell()
  {
    agentImpl.shellForScripts
  }

  protected Map getAgentBootArgs()
  {
    [
      shellForScripts: shell,
      rootShell: rootShell,
      agentLogDir: logFileSystem.root,
      storage: createStorage()
    ]
  }

  protected Map getInitParameters()
  {
    return initParameters
  }

  protected Map getActionArgs(String action)
  {
    actionArgs[action]
  }

  protected String getScriptMountPoint()
  {
    "/test/${this.class.simpleName}"
  }

  /**
   * If you name your test appropriately (<code>Test<NameOfGluScriptClass></code>) then you don't
   * have to override this method, otherwise override this method and provide the (fully qualified)
   * class name. Note that this method is only used by {@link #getScript()} to instantiate
   * the script directly.
   * 
   * @return the fully qualified class name of the script
   */
  protected String getScriptClass()
  {
    this.class.simpleName - 'Test'
  }

  /**
   * If you want to use a location (uri) instead, then override this method and return
   * [scriptLocation: ...]. In general it is uncessary since the test is testing a script which
   * should be in the classpath already so it can be instantiated
   */
  protected Map getScriptDefinition()
  {
    [scriptFactory: new FromClassNameScriptFactory(scriptClass)]
  }

  protected void installScript(def args)
  {
    if(!args.mountPoint)
      args.mountPoint = scriptMountPoint

    if(!args.initParameters)
      args.initParameters = initParameters

    if(!args.scriptLocation && !args.scriptFactory)
      args.putAll(scriptDefinition)

    agentImpl.installScript(args)
  }

  protected void installScript()
  {
    installScript([:])
  }

  protected void uninstallScript(def args)
  {
    if(!args.mountPoint)
      args.mountPoint = scriptMountPoint

    agentImpl.uninstallScript(args)
  }


  protected void uninstallScript()
  {
    uninstallScript([:])
  }

  protected ScriptNode getScript()
  {
    agentImpl.findScript(scriptMountPoint)
  }


  protected String asyncExecuteAction(def args)
  {
    if(!args.mountPoint)
      args.mountPoint = scriptMountPoint

    if(!args.actionActionArgs)
      args.actionArgs = getActionArgs(args.action)

    agentImpl.executeAction(args)
  }

  protected def syncExecuteAction(def args)
  {
    if(!args.mountPoint)
      args.mountPoint = scriptMountPoint

    if(!args.actionActionArgs)
      args.actionArgs = getActionArgs(args.action)

    agentImpl.executeActionAndWait(args)
  }

  protected def getFullState()
  {
    agentImpl.getFullState(mountPoint: scriptMountPoint)
  }

  protected def getExportedScriptFieldValue(String fieldName)
  {
    fullState?.scriptState?.script?."${fieldName}"
  }

  protected Map getStateMachineState()
  {
    fullState?.scriptState?.stateMachine
  }

  protected def install()
  {
    syncExecuteAction(action: 'install')
  }

  protected String asyncInstall()
  {
    asyncExecuteAction(action: 'install')
  }

  protected def configure()
  {
    syncExecuteAction(action: 'configure')
  }

  protected String asyncConfigure()
  {
    asyncExecuteAction(action: 'configure')
  }

  protected def start()
  {
    syncExecuteAction(action: 'start')
  }

  protected String asyncStart()
  {
    asyncExecuteAction(action: 'start')
  }

  protected def stop()
  {
    syncExecuteAction(action: 'stop')
  }

  protected String asyncStop()
  {
    asyncExecuteAction(action: 'stop')
  }

  protected def unconfigure()
  {
    syncExecuteAction(action: 'unconfigure')
  }

  protected String asyncUnconfigure()
  {
    asyncExecuteAction(action: 'unconfigure')
  }

  protected def uninstall()
  {
    syncExecuteAction(action: 'uninstall')
  }

  protected String asyncUninstall()
  {
    asyncExecuteAction(action: 'uninstall')
  }

  protected void deploy()
  {
    installScript()
    install()
    configure()
    start()
  }

  protected void undeploy()
  {
    stop()
    unconfigure()
    uninstall()
    uninstallScript()
  }

  protected void clearError()
  {
    agentImpl.clearError(mountPoint: scriptMountPoint)
  }

  protected ScriptExecutionCauseException scriptShouldFail(Closure closure)
  {
    try
    {
      closure()
      fail("script should have failed")
      return null // never reached...
    }
    catch(ScriptExecutionException e)
    {
      return e.cause as ScriptExecutionCauseException
    }
  }
}