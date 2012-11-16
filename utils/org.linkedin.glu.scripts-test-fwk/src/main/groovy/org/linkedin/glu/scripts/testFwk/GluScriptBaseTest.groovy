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
 * In order to write a unit test for a glu script you should create your own test class by extending
 * this class.
 *
 * In your setup, you can set the {@link GluScriptBaseTest#initParameters}. Most of the methods
 * can be overriden to provide customized values. From your test you can then use the convenient
 * methods like {@link GluScriptBaseTest#deploy()} to run through all the phase of your script.
 *
 * @see GluScriptBaseTest#getScriptClass()} for advice on how to name your test
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
    rootShell = createRootShell()
    shell = createShell()
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
    new ShellImpl(fileSystem: appsFileSystem,
                  agentProperties: new AgentProperties(createAgentProperties()))
  }

  protected Shell createRootShell()
  {
    new ShellImpl(fileSystem: logFileSystem)
  }

  /**
   * If you need to add some properties, you can override this method and add your own set
   * of properties (available in glu script with <code>shell.env</code>)
   */
  protected Map createAgentProperties()
  {
    [
      'glu.agent.name': agentName,
      'glu.agent.fabric': fabric,
      'glu.agent.scriptRootDir': appsFileSystem.root.file.canonicalPath,
      'glu.agent.logDir': logFileSystem.root.file.canonicalPath,
      'glu.agent.tempDir': rootShell.tmpRoot.file.canonicalPath
    ]
  }

  protected AgentImpl createAgent()
  {
    new AgentImpl(clock: clock)
  }


  /**
   * @return the fabric this agent belongs to
   */
  protected String getFabric()
  {
    'test-fabric'
  }

  /**
   * @return name of the agent
   */
  protected String getAgentName()
  {
    'test-agent'
  }

  /**
   * @return the agent interface to call all the methods you want (see Agent javadoc api)
   */
  protected Agent getAgent()
  {
    agentImpl
  }

  /**
   * @return the same shell that your script has access to (so that the unit test can use the same
   * convenient calls)
   */
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

  /**
   * Modify the {@link #initParameters} field or extend this method to customize the initParameters
   * used when installing the script
   */
  protected Map getInitParameters()
  {
    return initParameters
  }

  /**
   * Modify the {@link #actionArgs} field or extend this method to customize the actionArgs
   * used when invoking an action on the script
   */
  protected Map getActionArgs(String action)
  {
    actionArgs[action]
  }

  /**
   * The mountpoint on which the script will be mounted. If the name of the mountpoint is being
   * used by your script, you may want to customize it!
   */
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
   * [scriptLocation: ...]. In general it is unnecessary since the test is testing a script which
   * should be in the classpath already so it can be instantiated
   */
  protected Map getScriptDefinition()
  {
    [scriptFactory: new FromClassNameScriptFactory(scriptClass)]
  }

  /**
   * Install the script (use sensible defaults if values are not provided)
   */
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

  /**
   * Install the script (use all default/configured values)
   */
  protected void installScript()
  {
    installScript([:])
  }

  /**
   * Unnstall the script (use sensible defaults if values are not provided)
   */
  protected void uninstallScript(def args)
  {
    if(!args.mountPoint)
      args.mountPoint = scriptMountPoint

    agentImpl.uninstallScript(args)
  }

  /**
   * Uninstall the script (use all default/configured values)
   */
  protected void uninstallScript()
  {
    uninstallScript([:])
  }

  /**
   * @return the (internal) script representation if you need to access it directly
   */
  protected ScriptNode getScript()
  {
    agentImpl.findScript(scriptMountPoint)
  }

  /**
   * Execute the action asynchronously
   *
   * @param args (use sensible defaults if values are not provided)
   * @see Agent#executeAction
   */
  protected String asyncExecuteAction(def args)
  {
    if(!args.mountPoint)
      args.mountPoint = scriptMountPoint

    if(!args.actionActionArgs)
      args.actionArgs = getActionArgs(args.action)

    agentImpl.executeAction(args)
  }

  /**
   * Execute the action synchronously
   *
   * @param args (use sensible defaults if values are not provided)
   * @see Agent#executeActionAndWait
   */
  protected def syncExecuteAction(def args)
  {
    if(!args.mountPoint)
      args.mountPoint = scriptMountPoint

    if(!args.actionActionArgs)
      args.actionArgs = getActionArgs(args.action)

    agentImpl.executeActionAndWait(args)
  }

  /**
   * @return the full state of the script
   */
  protected def getFullState()
  {
    agentImpl.getFullState(mountPoint: scriptMountPoint)
  }

  /**
   * Shortcut to get the value from the full state
   *
   * @return the value of a field in your script (note that if the value is not exported
   * (ex transient feature) you will get <code>null</code>)
   */
  protected def getExportedScriptFieldValue(String fieldName)
  {
    fullState?.scriptState?.script?."${fieldName}"
  }

  /**
   * Shortcut to get the value from the full state
   *
   * @return the state machine state
   */
  protected Map getStateMachineState()
  {
    fullState?.scriptState?.stateMachine
  }

  /**
   * Synchronously runs the install action on a previously installed script
   */
  protected def install()
  {
    syncExecuteAction(action: 'install')
  }

  /**
   * Asynchronously runs the install action on a previously installed script
   */
  protected String asyncInstall()
  {
    asyncExecuteAction(action: 'install')
  }

  /**
   * Synchronously runs the configure action on a previously installed script
   */
  protected def configure()
  {
    syncExecuteAction(action: 'configure')
  }

  /**
   * Asynchronously runs the configure action on a previously installed script
   */
  protected String asyncConfigure()
  {
    asyncExecuteAction(action: 'configure')
  }

  /**
   * Synchronously runs the start action on a previously installed script
   */
  protected def start()
  {
    syncExecuteAction(action: 'start')
  }

  /**
   * Asynchronously runs the start action on a previously installed script
   */
  protected String asyncStart()
  {
    asyncExecuteAction(action: 'start')
  }

  /**
   * Synchronously runs the stop action on a previously installed script
   */
  protected def stop()
  {
    syncExecuteAction(action: 'stop')
  }

  /**
   * Asynchronously runs the stop action on a previously installed script
   */
  protected String asyncStop()
  {
    asyncExecuteAction(action: 'stop')
  }

  /**
   * Synchronously runs the unconfigure action on a previously installed script
   */
  protected def unconfigure()
  {
    syncExecuteAction(action: 'unconfigure')
  }

  /**
   * Asynchronously runs the unconfigure action on a previously installed script
   */
  protected String asyncUnconfigure()
  {
    asyncExecuteAction(action: 'unconfigure')
  }

  /**
   * Synchronously runs the uninstall action on a previously installed script
   */
  protected def uninstall()
  {
    syncExecuteAction(action: 'uninstall')
  }

  /**
   * Asynchronously runs the uninstall action on a previously installed script
   */
  protected String asyncUninstall()
  {
    asyncExecuteAction(action: 'uninstall')
  }

  /**
   * Shortcut to run {@link #installScript()} then {@link #install()}, then {@link #configure()},
   * then {@link #start()}
   */
  protected void deploy()
  {
    installScript()
    install()
    configure()
    start()
  }

  /**
   * Shortcut to run {@link #stop()} then {@link #unconfigure()} then {@link #uninstall()} then
   * {@link #uninstallScript()}
   */
  protected void undeploy()
  {
    stop()
    unconfigure()
    uninstall()
    uninstallScript()
  }

  /**
   * Clear any error on your script
   */
  protected void clearError()
  {
    agentImpl.clearError(mountPoint: scriptMountPoint)
  }

  /**
   * Runs the closure (which should executing an action on your script) and make sure that it fails.
   * It returns the exception being thrown (always of type {@link ScriptExecutionCauseException}
   * provided that you executed an action in the script of course!
   */
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