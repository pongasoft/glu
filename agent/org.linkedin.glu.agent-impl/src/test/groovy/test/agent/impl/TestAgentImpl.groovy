/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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


package test.agent.impl

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.api.NoSuchMountPointException
import org.linkedin.glu.agent.impl.AgentImpl
import org.linkedin.glu.agent.impl.capabilities.ShellImpl
import org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory
import org.linkedin.glu.agent.impl.script.RootScript
import org.linkedin.glu.agent.impl.script.ScriptDefinition
import org.linkedin.glu.agent.impl.storage.RAMStorage
import org.linkedin.glu.agent.impl.storage.Storage
import org.linkedin.glu.agent.api.ScriptExecutionException
import org.linkedin.glu.agent.api.ScriptIllegalStateException
import org.linkedin.util.concurrent.ThreadControl
import org.linkedin.util.clock.Timespan
import org.linkedin.util.clock.SettableClock
import org.linkedin.util.io.ram.RAMDirectory
import org.linkedin.util.io.resource.internal.RAMResourceProvider
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.groovy.util.state.StateMachine
import org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils
import java.util.concurrent.TimeoutException
import org.linkedin.glu.agent.impl.storage.AgentProperties
import org.linkedin.glu.agent.impl.storage.TagsStorage
import org.linkedin.util.concurrent.ThreadPerTaskExecutor
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import org.linkedin.glu.groovy.utils.concurrent.GluGroovyConcurrentUtils
import org.linkedin.glu.agent.api.Shell
import org.linkedin.util.clock.Chronos
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.linkedin.glu.utils.core.Externable
import org.linkedin.glu.groovy.utils.test.GluGroovyTestUtils

/**
 * Test for AgentImpl
 *
 * @author ypujante@linkedin.com
 */
def class TestAgentImpl extends GroovyTestCase
{
  // we must use a static global because the glu script will be destroyed and recreated
  // by the agent.. so there is no real way to recover one except making it static global...
  public static ThreadControl GLOBAL_TC = new ThreadControl(Timespan.parse('5s'))

  public static final String APTN = 'TestAgentImpl.tags'

  def ram
  def fileSystem
  def logFileSystem
  def shell
  def ramStorage = [:]
  AgentProperties agentProperties = new AgentProperties(['org.linkedin.app.name': 'glu-agent'])
  AgentImpl agent
  def clock = new SettableClock()

  protected void setUp()
  {
    super.setUp();

    ram = new RAMDirectory()
    RAMResourceProvider rp = new RAMResourceProvider(ram)
    fileSystem = [
            mkdirs: { dir ->
              ram.mkdirhier(dir.toString())
              return rp.createResource(dir.toString())
            },
            rmdirs: { dir ->
              ram.rm(dir.toString())
            },

            getRoot: { rp.createResource('/') },

            getTmpRoot: { rp.createResource('/tmp') },

            newFileSystem: { r,t -> fileSystem }
    ] as FileSystem

    // the agent is logging for each script... we don't want the output in the test
    // TODO MED YP: how do I do this with slf4j ?
    // Log.setFactory(new RAMLoggerFactory())

    logFileSystem = FileSystemImpl.createTempFileSystem()

    shell = new ShellImpl(fileSystem: fileSystem)

    agent = new AgentImpl()
    Storage storage = createStorage()
    agent.boot(clock: clock,
               shellForScripts: shell,
               rootShell: new ShellImpl(fileSystem: logFileSystem,
                                        agentProperties: agentProperties),
               agentLogDir: logFileSystem.root,
               taggeable: new TagsStorage(storage, APTN),
               storage: storage)
  }

  protected void tearDown()
  {
    try
    {
      logFileSystem.destroy()
      agent.shutdown()
      agent.waitForShutdown('5s')
    }
    finally
    {
      super.tearDown()
    }
  }

  private def createStorage()
  {
    return new RAMStorage(ramStorage, agentProperties)
  }

  /**
   * Basic test for the script manager
   */
  void testScriptManager()
  {
    assertEquals([MountPoint.ROOT], agent.getMountPoints())

    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        initParameters: [p1: 'v1'],
                        scriptFactory: new FromClassNameScriptFactory(MyScriptTestAgentImpl))

    assertEquals([MountPoint.ROOT, scriptMountPoint], agent.getMountPoints())

    def res = [:]
    // then we run the 'install' action
    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'install',
                        actionArgs: [value: 1,
                         expectedMountPoint: scriptMountPoint,
                         expectedParentRootPath: MountPoint.ROOT,
                         res: res])

    // then we wait for the action to be completed
    assertTrue agent.waitForState(mountPoint: scriptMountPoint, state: 'installed')

    // make sure the install closure is actually called!
    assertEquals('1/v1', res.install)

    // we cannot uninstall the script because it is not in NONE state
    shouldFail(ScriptIllegalStateException) {
      agent.uninstallScript(mountPoint: scriptMountPoint)         
    }

    // test shortcut method: run uninstall and wait for the state to be NONE
    assertTrue agent.executeActionAndWaitForState(mountPoint: scriptMountPoint,
                                                  action: 'uninstall',
                                                  state: StateMachine.NONE)

    // script is already in NONE state
    assertTrue agent.waitForState(mountPoint: scriptMountPoint,
                                  state: StateMachine.NONE)

    // now uninstall should work
    agent.uninstallScript(mountPoint: scriptMountPoint)

    assertEquals([MountPoint.ROOT], agent.getMountPoints())
    
    // the script is not found => null
    assertEquals('/s', shouldFail(NoSuchMountPointException) {
                 agent.waitForState(mountPoint: scriptMountPoint,
                                    state: StateMachine.NONE)
                 })
  }

  /**
   * The agent behaves in an asynchronous fashion so this test will make sure that it is the
   * case.
   */
  void testAsynchronism()
  {
    def tc = new ThreadControl()

    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        initParameters: [p1: 'v1'],
                        scriptFactory: new FromClassNameScriptFactory(MyScriptTestAgentImpl2))

    def res = [:]
    // then we run the 'install' action
    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'install',
                        actionArgs: [res: res, tc: tc])

    tc.waitForBlock('s1')

    assertFalse agent.waitForState(mountPoint: scriptMountPoint, state: 'installed', timeout: 200)

    tc.unblock('s1')

    assertTrue agent.waitForState(mountPoint: scriptMountPoint, state: 'installed')

    assertEquals('v1', res.install)
  }


  /**
   * Test that an action can be interrupted properly
   */
  void testInterrupt()
  {
    def tc = new ThreadControl()

    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        scriptFactory: new FromClassNameScriptFactory(MyScriptTestInterrupt))

    def res = [:]
    // then we run the 'install' action
    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'install',
                        actionArgs: [res: res, tc: tc])

    tc.unblock('start')

    // we make sure that we are sleeping...
    assertFalse agent.waitForState(mountPoint: scriptMountPoint, state: 'installed', timeout: '500')

    // wrong action.. should return false
    assertFalse agent.interruptAction(mountPoint: scriptMountPoint,
                                      action: 'foobar')

    // wrong mountpoint.. should return false
    assertFalse agent.interruptAction(mountPoint: '/foobar',
                                      action: 'install')

    // interrupt
    assertTrue agent.interruptAction(mountPoint: scriptMountPoint,
                                     action: 'install')

    tc.unblock('exception') 

    shouldFail(ScriptExecutionException) {
      agent.waitForState(mountPoint: scriptMountPoint, state: 'installed', timeout: '1s')
    }

    assertNull res.notReached

    assertEquals('sleep interrupted', shouldFail(InterruptedException) {
      throw res.exception
    })
  }

  /**
   * we make sure that the state is being stored correctly
   */
  void testStateKeeperStore()
  {
    // should be root in the state...
    assertEquals(1, ramStorage.size())
    def rootValues = [
            scriptDefinition: new ScriptDefinition(MountPoint.ROOT,
                                                   null,
                                                   new FromClassNameScriptFactory(RootScript),
                                                   [:]),
            scriptState: [
                    script: [rootPath: MountPoint.ROOT],
                    stateMachine: [currentState: 'installed']
            ]
    ]

    // we check root
    checkStorage(MountPoint.ROOT, rootValues)

    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        initParameters: [p1: 'v1'],
                        scriptFactory: new FromClassNameScriptFactory(MyScriptTestAgentImpl3))

    def scriptValues = [
            scriptDefinition: new ScriptDefinition(scriptMountPoint,
                                                   MountPoint.ROOT,
                                                   new FromClassNameScriptFactory(MyScriptTestAgentImpl3),
                                                   [p1: 'v1']),
            scriptState: [
                    script: [:],
                    stateMachine: [currentState: StateMachine.NONE],
            ]
    ]

    // we check root (to be sure) and /s
    checkStorage(MountPoint.ROOT, rootValues)
    checkStorage(scriptMountPoint, scriptValues)

    // we run the install action
    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'install',
                        actionArgs: [p: 'c'])
    assertTrue agent.waitForState(mountPoint: scriptMountPoint, state: 'installed')

    // we verify that the new state is stored
    scriptValues.scriptState.stateMachine.currentState = 'installed'
    scriptValues.scriptState.script.vp1 = 'v1c'
    checkStorage(scriptMountPoint, scriptValues)

    // we then force an exception to be raised
    Exception sex = new ScriptExecutionException("${MyScriptTestAgentImpl3.class.name} [/s]".toString(),
                                                 "configure", [exception: true],
                                                 new Exception('mine'))
    def actionId = agent.executeAction(mountPoint: scriptMountPoint,
                                       action: 'configure',
                                       actionArgs: [exception: true])
    assertEquals(sex.message,
                 shouldFail(ScriptExecutionException) {
                     agent.waitForAction(mountPoint: scriptMountPoint, actionId: actionId)
                 })

    assertEquals(sex.message,
                 shouldFail(ScriptExecutionException) {
                   agent.waitForState(mountPoint: scriptMountPoint, state: 'installed')
                 })

    // we verify that the new state is stored
    scriptValues.scriptState.stateMachine.error = sex
    checkStorage(scriptMountPoint, scriptValues)

    // we clear the error generated in the previous call
    agent.clearError(mountPoint: scriptMountPoint)

    // we verify that the error state is cleared
    scriptValues.scriptState.stateMachine.remove('error')
    checkStorage(scriptMountPoint, scriptValues)

    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'uninstall')
    assertTrue agent.waitForState(mountPoint: scriptMountPoint, state: StateMachine.NONE)

    // we verify that the new state is stored
    scriptValues.scriptState.stateMachine.currentState = StateMachine.NONE
    checkStorage(scriptMountPoint, scriptValues)

    // we then uninstall the script... only remaining should be root
    agent.uninstallScript(mountPoint: scriptMountPoint)

    assertEquals(1, ramStorage.size())
    checkStorage(MountPoint.ROOT, rootValues)

    // we reinstall the script and bring it up to installed state
    agent.installScript(mountPoint: scriptMountPoint,
                        initParameters: [p1: 'v1'],
                        scriptFactory: new FromClassNameScriptFactory(MyScriptTestAgentImpl3))
    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'install',
                        actionArgs: [p: 'c'])
    assertTrue agent.waitForState(mountPoint: scriptMountPoint, state: 'installed')
    scriptValues.scriptState.stateMachine.currentState = 'installed'
    checkStorage(scriptMountPoint, scriptValues)

    // we set a timer and we make sure it gets executed
    def timerMountPoint = MountPoint.fromPath('/t')
    agent.installScript(mountPoint: timerMountPoint,
                        initParameters: [p1: 'v1'],
                        scriptFactory: new FromClassNameScriptFactory(MyScriptTestTimer))
    actionId = agent.executeAction(mountPoint: timerMountPoint,
                                   action: 'install',
                                   actionArgs: [repeatFrequency: '1s'])
    assertEquals('v1', agent.waitForAction(mountPoint: timerMountPoint, actionId: actionId))

    def timerNode = agent.executeCall(mountPoint: timerMountPoint, call: 'getScriptNode')

    // we advance the clock by 1s: the timer should fire
    advanceClock('1s', timerNode)

    // should fire the timer
    GLOBAL_TC.unblock('timer1.start')
    GLOBAL_TC.unblock('timer1.end')

    assertEquals([currentState: 'installed'], agent.getState(mountPoint: timerMountPoint))

    // we advance the clock by 1s: the timer should fire
    advanceClock('1s', timerNode)

    // should fire the timer and force the state machine in a new state
    GLOBAL_TC.unblock('timer1.start', [currentState: 'stopped', error: null])
    GLOBAL_TC.unblock('timer1.end')

    // the timer is running in a separate thread.. so the condition will happen asynchronously
    GroovyConcurrentUtils.waitForCondition(clock, '5s', 10) {
      clock.addDuration(Timespan.parse('10'))
      [currentState: 'stopped'] == agent.getState(mountPoint: timerMountPoint)
    }

    // we set some tags before shutdown
    assertFalse(agent.hasTags())
    agent.setTags(['fruit', 'vegetable'])
    assertEquals(['fruit', 'vegetable'] as Set, agent.getTags())

    // now we shutdown the current agent and we recreate a new one
    agent.shutdown()
    agent.waitForShutdown(0)

    agent = new AgentImpl()
    Storage storage = createStorage()
    agent.boot(clock: clock,
               shellForScripts: shell,
               taggeable: new TagsStorage(storage, APTN),
               storage: storage)

    // we make sure that right after boot the tags are still there
    assertEquals(['fruit', 'vegetable'] as Set, agent.getTags())

    // we verify that the scripts have been restored properly
    assertEquals([currentState: 'installed'],
                 agent.getState(mountPoint: scriptMountPoint))

    checkStorage(MountPoint.ROOT, rootValues)
    checkStorage(scriptMountPoint, scriptValues)

    // this will test the value
    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'configure',
                        actionArgs: [value: 'v1c'])
    assertTrue agent.waitForState(mountPoint: scriptMountPoint, state: 'stopped')

    scriptValues.scriptState.stateMachine.currentState = 'stopped'
    checkStorage(scriptMountPoint, scriptValues)

    // we revert back to installed state
    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'unconfigure',
                        actionArgs: [value: 'v1c'])
    assertTrue agent.waitForState(mountPoint: scriptMountPoint, state: 'installed')

    scriptValues.scriptState.stateMachine.currentState = 'installed'
    checkStorage(scriptMountPoint, scriptValues)

    // we now force an exception
    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'configure',
                        actionArgs: [exception: true])
    assertEquals(sex.message,
                 shouldFail(ScriptExecutionException) {
      agent.waitForState(mountPoint: scriptMountPoint, state: 'stopped')
    })

    assertEquals([currentState: 'installed', error: sex],
                 agent.getState(mountPoint: scriptMountPoint))
    
    // now we shutdown the current agent and we recreate a new one
    agent.shutdown()
    agent.waitForShutdown(0)

    agent = new AgentImpl()
    storage = createStorage()
    agent.boot(shellForScripts: shell,
               taggeable: new TagsStorage(storage, APTN),
               storage: storage)

    // we make sure that right after boot the tags are still there
    assertEquals(['fruit', 'vegetable'] as Set, agent.getTags())
    
    // we verify that the scripts have been restored properly
    assertEquals([currentState: 'installed', error: sex],
                 agent.getState(mountPoint: scriptMountPoint))

    // /s
    def exepectedFullState = [
        scriptDefinition:
        [
            mountPoint: scriptMountPoint,
            parent: MountPoint.ROOT,
            scriptFactory:
            [
                'class': 'org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory',
                className: MyScriptTestAgentImpl3.class.name
            ],
            initParameters: [p1: 'v1']
        ],
        scriptState:
        [
            script: [vp1: 'v1c'],
            stateMachine:
            [
                currentState: 'installed',
                error:sex
            ]
        ]

    ]
    assertEquals(exepectedFullState, agent.getFullState(mountPoint: scriptMountPoint))

    // /t
    exepectedFullState = [
        scriptDefinition:
        [
            mountPoint: timerMountPoint,
            parent: MountPoint.ROOT,
            scriptFactory:
            [
                'class': 'org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory',
                className: MyScriptTestTimer.class.name
            ],
            initParameters: [p1: 'v1']
        ],
        scriptState:
        [
            script: [:],
            stateMachine: [ currentState: 'stopped' ],
            timers: [[timer: 'timer1', repeatFrequency: '1s']]
        ]

    ]
    assertEquals(exepectedFullState, agent.getFullState(mountPoint: timerMountPoint))

    // we make sure that the timers get restored properly after shutdown
    timerNode = agent.executeCall(mountPoint: timerMountPoint, call: 'getScriptNode')

    // we advance the clock by 1s: the timer should fire
    advanceClock('1s', timerNode)

    // should fire the timer
    GLOBAL_TC.unblock('timer1.start', [currentState: 'installed', error: null])
    GLOBAL_TC.unblock('timer1.end')

    // the timer is running in a separate thread.. so the condition will happen asynchronously
    GroovyConcurrentUtils.waitForCondition(clock, '5s', 10) {
      [currentState: 'installed'] == agent.getState(mountPoint: timerMountPoint)
    }

    // we advance the clock by 1s: the timer should fire
    advanceClock('1s', timerNode)

    // should fire the timer (which will raise an exception)
    GLOBAL_TC.unblock('timer1.start', [currentState: 'invalid', error: null])
    GLOBAL_TC.unblock('timer1.end')

    // we advance the clock by 1s: the timer should fire
    advanceClock('1s', timerNode)

    // make sure that exception did not cause the timer to not fire anymore and did not change the
    // state
    GLOBAL_TC.waitForBlock('timer1.start')
    assertEquals([currentState: 'installed'], agent.getState(mountPoint: timerMountPoint))
    GLOBAL_TC.unblock('timer1.start', [currentState: 'running', error: null])
    GLOBAL_TC.unblock('timer1.end')

    // the timer is running in a separate thread.. so the condition will happen asynchronously
    GroovyConcurrentUtils.waitForCondition(clock, '5s', 10) {
      [currentState: 'running'] == agent.getState(mountPoint: timerMountPoint)
    }
  }

  /**
   * Testing proper agent shutdown (glu-20).
   */
  public void testAgentShutdown()
  {
    def tc = new ThreadControl(Timespan.parse('30s'))

    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        scriptFactory: new FromClassNameScriptFactory(MyScriptTestShutdown))

    def res = [:]
    // then we run the 'install' action
    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'install',
                        actionArgs: [res: res, tc: tc])

    // we make sure that the script is currently executing
    tc.waitForBlock('shutdown')

    // we now shutdown the agent
    agent.shutdown()
    // the agent will not shutdown until the closure completes
    shouldFail(TimeoutException) { agent.waitForShutdown('1s') }

    // let the install closure complete
    tc.unblock('shutdown')

    // now it will shutdown
    agent.waitForShutdown('5s')
  }

  /**
   * Test for deadlock on shutdown (glu-52)
   */
  public void testDeadlockOnShutdown()
  {
    def tc = new ThreadControl(Timespan.parse('30s'))

    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        scriptFactory: new FromClassNameScriptFactory(MyScriptTestDeadlockOnShutdown))

    // then we run the 'install' action
    def id = agent.executeAction(mountPoint: scriptMountPoint,
                                 action: 'install',
                                 actionArgs: [tc: tc])

    // we make sure that the script is currently executing
    tc.waitForBlock('shutdown')

    // we now shutdown the agent
    agent.shutdown()

    // the agent will not shutdown until the closure completes
    shouldFail(TimeoutException) { agent.waitForShutdown('1s') }

    Thread.start {
      // now it will shutdown
      agent.waitForShutdown('10s')

      tc.block('shutdownComplete')
    }

    String transitionState

    // let the install closure continue
    tc.unblock('shutdown')

    transitionState =
      agent.waitForAction(mountPoint: scriptMountPoint,
                          actionId: id,
                          timeout: '5s').transitionState

    // verify the transition state
    assertEquals('NONE->installed', transitionState)

    // make sure that the agent shutdown properly
    tc.unblock('shutdownComplete')
  }

  /**
   * test the wait for shutdown state feature which is a fix for issue #69
   */
  public void testWaitForShutdownState()
  {
    def tc = new ThreadControl(Timespan.parse('30s'))

    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        scriptFactory: new FromClassNameScriptFactory(MyScriptTestWaitForShutdownState))

    // then we run the 'install' action
    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'install',
                        actionArgs: [tc: tc])

    // we make sure that the script is currently executing
    tc.waitForBlock('beforeWait')

    assertFalse(agent.waitForState(mountPoint: scriptMountPoint,
                                   state: 'installed',
                                   timeout: '250'))

    tc.unblock('beforeWait')

    assertFalse(agent.waitForState(mountPoint: scriptMountPoint,
                                   state: 'installed',
                                   timeout: '250'))

    def asyncWaitForState = {
      agent.waitForState(mountPoint: scriptMountPoint,
                                   state: 'installed',
                                   timeout: '10s')
    }

    Future futureWaitForState =
      ThreadPerTaskExecutor.execute(GluGroovyConcurrentUtils.asCallable(asyncWaitForState))

    agent.shutdown()

    tc.waitForBlock('afterWait')

    assertFalse(agent.waitForState(mountPoint: scriptMountPoint,
                                   state: 'installed',
                                   timeout: '250'))

    tc.unblock('afterWait')

    assertEquals(Boolean.TRUE, futureWaitForState.get(5, TimeUnit.SECONDS))

    agent.waitForShutdown('5s')
  }

  public void testStateManager()
  {
    // we set a timer and we make sure it gets executed
    def timerMountPoint = MountPoint.fromPath('/t')
    agent.installScript(mountPoint: timerMountPoint,
                        initParameters: [p1: 'v1'],
                        scriptFactory: new FromClassNameScriptFactory(MyScriptTestTimer))
    def id = agent.executeAction(mountPoint: timerMountPoint,
                                 action: 'install',
                                 actionArgs: [repeatFrequency: '1s'])
    assertEquals('v1', agent.waitForAction(mountPoint: timerMountPoint, actionId: id))

    def timerNode = agent.executeCall(mountPoint: timerMountPoint, call: 'getScriptNode')

    // we advance the clock by 1s: the timer should fire
    advanceClock('1s', timerNode)

    // should fire the timer
    GLOBAL_TC.unblock('timer1.start')
    GLOBAL_TC.unblock('timer1.end')

    assertEquals([currentState: 'installed'], agent.getState(mountPoint: timerMountPoint))

    // we advance the clock by 1s: the timer should fire
    advanceClock('1s', timerNode)

    // should fire the timer and force the state machine in a new state
    GLOBAL_TC.unblock('timer1.start', [currentState: 'stopped', error: 'this is the error'])

    // state should now be changed
    GLOBAL_TC.waitForBlock('timer1.end')

    assertEquals("state machine in error: [this is the error]",
                 shouldFail(IllegalStateException) { agent.waitForState(mountPoint: timerMountPoint,
                                                                        state: 'stopped',
                                                                        timeout: '1s') })

    assertEquals([currentState: 'stopped', error: 'this is the error'],
                 agent.getState(mountPoint: timerMountPoint))

    GLOBAL_TC.unblock('timer1.end')

    agent.clearError(mountPoint: timerMountPoint)
    
    agent.executeActionAndWait(mountPoint: timerMountPoint,
                               action: 'unconfigure')

    agent.executeActionAndWait(mountPoint: timerMountPoint,
                               action: 'uninstall')
  }

  // we advance the clock and wake up the execution thread to make sure it processes the event
  private void advanceClock(timeout, node)
  {
    // we wait first for the timeline to not be empty
    GroovyConcurrentUtils.waitForCondition(clock, '5s', 10) {
      node.scriptExecution.timeline
    }
    clock.addDuration(Timespan.parse(timeout.toString()))
    synchronized(node.scriptExecution.lock)
    {
      node.scriptExecution.lock.notifyAll()
    }
  }

  /**
   * Test for tailing the log
   */
  public void testTailAgentLog()
  {
    assertNull(agent.tailAgentLog([maxLine: 4]))

    def agentLog = logFileSystem.withOutputStream('glu-agent.out') { file, out ->
      (1..1000).each { lineNumber ->
        out.write("gal: ${lineNumber}\n".getBytes('UTF-8'))
      }
      return file
    }

    assertEquals("""gal: 997
gal: 998
gal: 999
gal: 1000
""", agent.tailAgentLog([maxLine: 4]).text)

    def gcLog = logFileSystem.withOutputStream('gc.log') { file, out ->
      (1..1000).each { lineNumber ->
        out.write("gc: ${lineNumber}\n".getBytes('UTF-8'))
      }
      return file
    }

    assertEquals("""gc: 999
gc: 1000
""", agent.tailAgentLog([log: gcLog.name, maxLine: 2]).text)
  }

  private Map toExternalRepresentation(Map m)
  {
    GluGroovyCollectionUtils.collectKey(m, [:]) { k, v ->
      if(v instanceof Externable)
        v.toExternalRepresentation()
      else
        v
    }
  }

  private void checkStorage(mountPoint, args)
  {
    def state = ramStorage[mountPoint]

    assertNotNull("state is null for ${mountPoint}", state)

    args.each { k,v ->
      assertEquals("testing ${k}", args[k], state[k])
    }

    // we make sure it is serializable
    new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(ramStorage) 
  }

  /**
   * The content of this test is very similar to TestCapabilities.testGenericExec (minus the test
   * for arguments that don't make sense)
   */
  public void testCommands()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def shell = new ShellImpl(fileSystem: fs)
      def shellScript = shell.fetch("./src/test/resources/shellScriptTestShellExec.sh")
      // let's make sure it is executable
      fs.chmod(shellScript, '+x')

      // stdout only
      checkShellExec(shell, [command: [shellScript, "-1"]], 0, "this goes to stdout\n", "")

      // both stdout and stderr in their proper channel
      checkShellExec(shell, [command: [shellScript, "-1", "-2"]], 0, "this goes to stdout\n", "this goes to stderr\n")

      // redirecting stderr to stdout
      checkShellExec(shell, [command: [shellScript, "-1", "-2"], redirectStderr: true], 0, "this goes to stdout\nthis goes to stderr\n", "")

      // testing for failure/exit value
      checkShellExec(shell, [command: [shellScript, "-1", "-e"]], 1, "this goes to stdout\n", "")

      // reading from stdin
      checkShellExec(shell, [command: [shellScript, "-1", "-c"], stdin: "abc\ndef\n"], 0, "this goes to stdout\nabc\ndef\n", "")

      Chronos c = new Chronos()

      // testing interrupt 1 (interrupting before reading)
      checkShellExec(shell, [command: ["sleep 10"]], null, "", "", { execResult ->

        // we wait until the command is actually started...
        shell.waitFor(timeout: '2s', heartbeat: '10') {
          agent._commandManager.findCommand(execResult.id).command.exitValueStream != null
        }
        assertTrue(agent.interruptCommand([id: execResult.id]))
        return execResult
      }, null)

      // we make sure that the command got interrupted properly and that it did not last the
      // full 10s. We use 2s as a buffer...
      assertTrue(c.tick() < Timespan.parse("2s").durationInMilliseconds)

      // testing interrupt 2 (interrupting after reading)
      checkShellExec(shell, [command: ["sleep 10"]], null, "", "", null, { execResult, streamResults ->

        // we wait until the command is actually started...
        shell.waitFor(timeout: '2s', heartbeat: '10') {
          agent._commandManager.findCommand(execResult.id).command.exitValueStream != null
        }

        assertTrue(agent.interruptCommand([id: execResult.id]))
        return streamResults
      })

      // we make sure that the command got interrupted properly and that it did not last the
      // full 10s. We use 2s as a buffer...
      assertTrue(c.tick() < Timespan.parse("2s").durationInMilliseconds)
    }
  }

  /**
   * test that the state is preserved properly
   */
  public void testCommandsState()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def shell = new ShellImpl(fileSystem: fs)

      Chronos c = new Chronos()

      // testing interrupt 1 (interrupting before reading)
      checkShellExec(shell, [command: ["sleep 10"]], null, "", "", { execResult ->

        // we wait until the command is actually started...
        shell.waitFor(timeout: '2s', heartbeat: '10') {
          agent._commandManager.findCommand(execResult.id).command.exitValueStream != null
        }

        def mountPoint = MountPoint.create("/_/command/${execResult.id}")

        def state = [
          scriptDefinition: [
            mountPoint: mountPoint,
            parent: MountPoint.ROOT,
            scriptFactory: [
              'class': 'CommandGluScriptFactory'
            ],
            initParameters: [:],
          ],
          scriptState: [
            script: [:],
            stateMachine: [
              currentState: 'stopped',
              transitionAction: 'start',
              transitionState: 'stopped->running'
            ]
          ]
        ]

        try
        {
          checkStorage2(mountPoint, state)
        }
        catch(Throwable th)
        {
          th.printStackTrace()
          throw th
        }

        assertTrue(agent.interruptCommand([id: execResult.id]))
        return execResult
      }, null)

      // we make sure that the command got interrupted properly and that it did not last the
      // full 10s. We use 2s as a buffer...
      assertTrue(c.tick() < Timespan.parse("2s").durationInMilliseconds)

      // after the command complete, it is automatically removed from storage
      shell.waitFor(timeout: '2s', heartbeat: '10') {
        ramStorage.size() == 1
      }
    }
  }

  private void checkStorage2(mountPoint, args)
  {
    def state = ramStorage[mountPoint]

    assertNotNull("state is null for ${mountPoint}", state)

    state = toExternalRepresentation(state)
    args = toExternalRepresentation(args)

    GluGroovyTestUtils.assertEqualsIgnoreType(this, "storage mismatch", args, state)

    // we make sure it is serializable
    new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(ramStorage)
  }

  private void checkShellExec(Shell shell,
                              commands,
                              exitValue,
                              stdout,
                              stderr,
                              Closure afterExecuteShellCommand = null,
                              Closure afterStreamCommandResults = null)
  {
    commands.command = commands.command.collect { it.toString() }.join(" ")
    if(commands.stdin)
      commands.stdin = new ByteArrayInputStream(commands.stdin.getBytes("UTF-8"))

    def execResult = agent.executeShellCommand(*: commands)

    if(afterExecuteShellCommand)
      execResult = afterExecuteShellCommand(execResult)

    def commandId = execResult.id

    def streamResults = agent.streamCommandResults(id: commandId,
                                                   exitValueStream: true,
                                                   exitValueStreamTimeout: 0,
                                                   exitErrorStream: true,
                                                   stdoutStream: true,
                                                   stderrStream: true)

    if(afterStreamCommandResults)
      streamResults = afterStreamCommandResults(execResult, streamResults)

    def stream = streamResults.stream

    if(!stream)
    {
      shouldFail { agent.waitForCommand(id: commandId) }
      agent.waitForCommand(id: commandId)
    }
    else
    {
      String text = stream.text

      stream = new ByteArrayInputStream(text.getBytes("UTF-8"))

      OutputStream stdoutStream = new ByteArrayOutputStream()

      OutputStream stderrStream = new ByteArrayOutputStream()

      try
      {
        try
        {
          assertEquals(exitValue, shell.demultiplexExecStream(stream, stdoutStream, stderrStream))
        }
        finally
        {
          assertEquals(stdout, new String(stdoutStream.toByteArray(), "UTF-8"))
          assertEquals(stderr, new String(stderrStream.toByteArray(), "UTF-8"))
        }

        assertEquals(exitValue, agent.waitForCommand(id: commandId))
      }
      catch(Throwable th)
      {
        System.err.println("Issue with stream?")
        System.err.println("<=================")
        System.err.println(text)
        System.err.println("=================>")
        throw th
      }
    }
  }

  private static class MyScriptTestAgentImpl
  {
    // YP Note: contrary to the test in TestScriptManager, the result is not returned (due to
    // asynchronism), so we put the return value in args.res thus insuring that the closure
    // is actually called...
    def install = { args ->
      GroovyTestCase.assertEquals(args.expectedMountPoint, mountPoint)
      GroovyTestCase.assertEquals(args.expectedParentRootPath, parent.rootPath)
      shell.mkdirs(mountPoint)
      args.res.install = "${args.value}/${params.p1}".toString()
    }
  }

  private static class MyScriptTestAgentImpl2
  {
    def install = { args ->
      args.tc.block('s1')
      args.res.install = params.p1
    }
  }

  private static class MyScriptTestAgentImpl3
  {
    def vp1

    def install = { args ->
      vp1 = params.p1 + args.p
    }

    def configure = { args ->
      if(args.exception)
        throw new Exception('mine')
      assert vp1 == args.value
      return vp1
    }

    def unconfigure = { args ->
      assert vp1 == args.value
      return vp1
    }
  }

  private static class MyScriptTestInterrupt
  {
    def install = { args ->

      args.tc.block('start')

      try
      {
        shell.waitFor() { duration ->
          return false
        }
      }
      catch (InterruptedException e)
      {
        args.tc.block('exception')
        args.res.exception = e
        throw e
      }

      // should never reach this line as an exception should be thrown!
      args.res.notReached = true
    }
  }

  private static class MyScriptTestTimer
  {
    def timer1 = {
      def args = TestAgentImpl.GLOBAL_TC.block('timer1.start')
      try
      {
        if(args)
        {
          stateManager.forceChangeState(args.currentState, args.error)
        }
      }
      finally
      {
        TestAgentImpl.GLOBAL_TC.block('timer1.end')
      }
    }

    def install = { args ->
      timers.schedule(timer: timer1, repeatFrequency: args.repeatFrequency)
      return params.p1
    }

    def uninstall = {
      timers.cancel(timer: timer1)
    }

    // this is a way to get the underlying script node
    def getScriptNode = {
      self
    }
  }

  private static class MyScriptTestShutdown
  {
    def install = { args ->
      args.tc.block('shutdown')
    }
  }

  private static class MyScriptTestDeadlockOnShutdown
  {
    def install = { args ->
      args.tc.block('shutdown')
      return stateManager.state // this causes a deadlock prior to fixing glu-52
    }
  }

  private static class MyScriptTestWaitForShutdownState
  {
    def install = { args ->
      args.tc.block('beforeWait')
      stateManager.waitForShutdownState()
      args.tc.block('afterWait')
    }
  }
}

