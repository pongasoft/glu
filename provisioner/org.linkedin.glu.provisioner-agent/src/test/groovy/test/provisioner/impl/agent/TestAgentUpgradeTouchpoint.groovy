/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package test.provisioner.impl.agent

import org.linkedin.glu.provisioner.api.planner.IAgentPlanner
import test.provisioner.impl.mocks.AgentMock
import test.provisioner.impl.mocks.AgentFactoryMock
import org.linkedin.glu.provisioner.core.touchpoint.TouchpointActionFactory
import org.linkedin.glu.provisioner.deployment.impl.ActionDescriptorStepExecutor
import org.linkedin.glu.provisioner.plan.api.ILeafStepExecutor
import org.linkedin.glu.provisioner.core.environment.Installation
import test.provisioner.impl.mocks.MockKeyProvider
import org.linkedin.glu.provisioner.impl.planner.AgentPlanner
import org.linkedin.glu.provisioner.impl.agent.AgentUpgradeTouchpoint
import org.linkedin.glu.agent.api.Agent
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.provisioner.impl.agent.AgentTouchpoint
import org.linkedin.glu.agent.api.ScriptExecutionException

/**
 * Tests for agent upgrade
 *
 * @author ypujante@linkedin.com */
class TestAgentUpgradeTouchpoint extends GroovyTestCase
{
  private IAgentPlanner _planner
  private AgentMock _agent = new AgentMock()
  private Map _agentMethods = [
      installScript: _agent.&installScript,
      clearError: _agent.&clearError,
      executeAction: _agent.&executeAction,
      waitForState: _agent.&waitForState,
      uninstallScript: _agent.&uninstallScript
  ]
  private ILeafStepExecutor _leafStepExecutor

  protected void setUp()
  {
    super.setUp()
    _planner = new AgentPlanner()
    def tp1 = new AgentUpgradeTouchpoint(new AgentFactoryMock(agent: _agentMethods as Agent), new MockKeyProvider())
    tp1.waitForRestartTimeout = Timespan.parse('200')
    def tp2 = new AgentTouchpoint(new AgentFactoryMock(agent: _agentMethods as Agent), new MockKeyProvider())
    _leafStepExecutor = new ActionDescriptorStepExecutor(actionFactory: new TouchpointActionFactory([tp1, tp2]))
  }

  /**
   * Simple happy path
   */
  public void testSuccess()
  {
    def plan = _planner.createUpgradePlan(['host1': new URI("https://host1:12906")], '1.0.0', 'file:/tmp/agent-1.0.0.tgz')

    // 1 host
    assertEquals(1, plan.step.steps.size())

    // 1 mount point (always)
    assertEquals(1, plan.step.steps[0].steps.size())

    plan.step.steps[0].steps[0].steps.each {
      _leafStepExecutor.executeLeafStep(it)
    }

    def expectedActions = [
        'installScript [mountPoint:/upgrade/1.0.0, scriptClassName:org.linkedin.glu.agent.impl.script.AutoUpgradeScript, parent:null, initParameters:[newVersion:1.0.0, agentTar:file:/tmp/agent-1.0.0.tgz]]',
        'clearError [mountPoint:/upgrade/1.0.0]',
        'executeAction [mountPoint:/upgrade/1.0.0, action:install, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/upgrade/1.0.0, state:installed, timeout:10s]',
        'clearError [mountPoint:/upgrade/1.0.0]',
        'executeAction [mountPoint:/upgrade/1.0.0, action:prepare, actionArgs:[:]]',
        'waitForState [mountPoint:/upgrade/1.0.0, state:prepared, timeout:10s]',
        'clearError [mountPoint:/upgrade/1.0.0]',
        'executeAction [mountPoint:/upgrade/1.0.0, action:commit, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/upgrade/1.0.0, state:upgraded, timeout:10s]',
        'clearError [mountPoint:/upgrade/1.0.0]',
        'executeAction [mountPoint:/upgrade/1.0.0, action:uninstall, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/upgrade/1.0.0, state:NONE, timeout:10s]',
        'uninstallScript [mountPoint:/upgrade/1.0.0]'
    ]

    checkActions(expectedActions, _agent.log)
  }


  /**
   * Test the rollback scenario: if commit fails, then issue a rollback
   */
  public void testRollback()
  {
    def plan = _planner.createUpgradePlan(['host1': new URI("https://host1:12906")], '1.0.0', 'file:/tmp/agent-1.0.0.tgz')

    // 1 host
    assertEquals(1, plan.step.steps.size())

    // 1 mount point (always)
    assertEquals(1, plan.step.steps[0].steps.size())

    plan.step.steps[0].steps[0].steps.each { step ->
      if(step.action.id == 'commitOrRollback')
      {
        def executeAction = _agentMethods.executeAction

        // changing the method to throw an exception during commit
        _agentMethods.executeAction = { args ->
          executeAction(args)
          if(args.action == 'commit')
            throw new ScriptExecutionException('msg: stacktrace')
        }
        _leafStepExecutor.executeLeafStep(step)

        // restoring execute action behavior
        _agentMethods.executeAction = executeAction
      }
      else
      {
        _leafStepExecutor.executeLeafStep(step)
      }
    }

    def expectedActions = [
        'installScript [mountPoint:/upgrade/1.0.0, scriptClassName:org.linkedin.glu.agent.impl.script.AutoUpgradeScript, parent:null, initParameters:[newVersion:1.0.0, agentTar:file:/tmp/agent-1.0.0.tgz]]',
        'clearError [mountPoint:/upgrade/1.0.0]',
        'executeAction [mountPoint:/upgrade/1.0.0, action:install, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/upgrade/1.0.0, state:installed, timeout:10s]',
        'clearError [mountPoint:/upgrade/1.0.0]',
        'executeAction [mountPoint:/upgrade/1.0.0, action:prepare, actionArgs:[:]]',
        'waitForState [mountPoint:/upgrade/1.0.0, state:prepared, timeout:10s]',
        'clearError [mountPoint:/upgrade/1.0.0]',
        'executeAction [mountPoint:/upgrade/1.0.0, action:commit, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'clearError [mountPoint:/upgrade/1.0.0]',
        'executeAction [mountPoint:/upgrade/1.0.0, action:rollback, actionArgs:[:]]',
        'waitForState [mountPoint:/upgrade/1.0.0, state:installed, timeout:10s]',
        'clearError [mountPoint:/upgrade/1.0.0]',
        'executeAction [mountPoint:/upgrade/1.0.0, action:uninstall, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/upgrade/1.0.0, state:NONE, timeout:10s]',
        'uninstallScript [mountPoint:/upgrade/1.0.0]'
    ]

    checkActions(expectedActions, _agent.log)
  }

  private void checkActions(List expectedActions, List actualActions)
  {
    assertEquals("Different number of actions! ${expectedActions} != ${actualActions}", expectedActions.size(), actualActions.size())
    expectedActions.eachWithIndex {el, idx ->
      assertEquals(el, actualActions[idx])
    }
  }

  public void testTransitionPlan()
  {
    def plan = _planner.createTransitionPlan('host1', '/mp/i001', new URI("https://host1:12906"), 'installed', 'running', null)

    // should be 2 actions
    assertEquals(2, plan.step.steps.size())

    plan.step.steps.each {
      _leafStepExecutor.executeLeafStep(it)
    }

    def expectedActions = [
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:configure, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:stopped, timeout:10s]',
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:start, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:running, timeout:10s]'
    ]

    checkActions(expectedActions, _agent.log)
    _agent.clearLog()

    // simulate bounce (stop/start)
    plan = _planner.createTransitionPlan('host1', '/mp/i001', new URI("https://host1:12906"), 'running', ['stopped', 'running'], null)

    // should be 2 actions
    assertEquals(2, plan.step.steps.size())

    plan.step.steps.each {
      _leafStepExecutor.executeLeafStep(it)
    }

    expectedActions = [
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:stop, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:stopped, timeout:10s]',
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:start, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:running, timeout:10s]'
    ]

    checkActions(expectedActions, _agent.log)
    _agent.clearLog()

    // simulate bounce (from installed state)
    plan = _planner.createTransitionPlan('host1', '/mp/i001', new URI("https://host1:12906"), 'installed', ['stopped', 'running'], null)

    // should be 2 actions
    assertEquals(2, plan.step.steps.size())

    plan.step.steps.each {
      _leafStepExecutor.executeLeafStep(it)
    }

    expectedActions = [
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:configure, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:stopped, timeout:10s]',
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:start, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:running, timeout:10s]'
    ]

    checkActions(expectedActions, _agent.log)
    _agent.clearLog()

    plan = _planner.createTransitionPlan([installation('host1', '/mp/i001', 'running'),
                                          installation('host1', '/mp/i002', 'installed'),
                                          installation('host1', '/mp/i003', 'stopped')], ['stopped', 'running'], null) { true }

    /*
<?xml version="1.0"?>
<plan name="Transition [stopped-&gt;running]">
  <sequential name="Transition [stopped-&gt;running]">
    <leaf name="Stop /mp/i001 on host1" />
    <leaf name="Start /mp/i001 on host1" />
    <leaf name="Configure /mp/i002 on host1" />
    <leaf name="Start /mp/i002 on host1" />
    <leaf name="Start /mp/i003 on host1" />
  </sequential>
</plan>
     */

    // should be 5 actions
    assertEquals(5, plan.step.steps.size())

    plan.step.steps.each {
      _leafStepExecutor.executeLeafStep(it)
    }

    expectedActions = [
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:stop, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:stopped, timeout:10s]',
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:start, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:running, timeout:10s]',
        'clearError [mountPoint:/mp/i002]',
        'executeAction [mountPoint:/mp/i002, action:configure, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i002, state:stopped, timeout:10s]',
        'clearError [mountPoint:/mp/i002]',
        'executeAction [mountPoint:/mp/i002, action:start, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i002, state:running, timeout:10s]',
        'clearError [mountPoint:/mp/i003]',
        'executeAction [mountPoint:/mp/i003, action:start, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i003, state:running, timeout:10s]'
    ]

    checkActions(expectedActions, _agent.log)
    _agent.clearLog()

    // testing filtering
    plan = _planner.createTransitionPlan([installation('host1', '/mp/i001', 'running'),
                                          installation('host1', '/mp/i002', 'installed'),
                                          installation('host1', '/mp/i003', 'stopped')], ['stopped', 'running'], null) {
      it.action.descriptorProperties.mountPoint == '/mp/i002'
    }

    /*
<?xml version="1.0"?>
<plan name="Transition [stopped-&gt;running]">
  <sequential name="Transition [stopped-&gt;running]">
    <leaf name="Configure /mp/i002 on host1" />
    <leaf name="Start /mp/i002 on host1" />
  </sequential>
</plan>
     */

    // should be 2 actions
    assertEquals(2, plan.step.steps.size())

    plan.step.steps.each {
      _leafStepExecutor.executeLeafStep(it)
    }

    expectedActions = [
        'clearError [mountPoint:/mp/i002]',
        'executeAction [mountPoint:/mp/i002, action:configure, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i002, state:stopped, timeout:10s]',
        'clearError [mountPoint:/mp/i002]',
        'executeAction [mountPoint:/mp/i002, action:start, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i002, state:running, timeout:10s]',
    ]

    checkActions(expectedActions, _agent.log)
    _agent.clearLog()

    // testing undeployed state (uninstall + uninstall script)
    plan = _planner.createTransitionPlan([installation('host1', '/mp/i001', 'running'),
                                          installation('host1', '/mp/i002', 'installed'),
                                          installation('host1', '/mp/i003', 'stopped')], ['undeployed'], null) {
      it.action.descriptorProperties.mountPoint == '/mp/i001'
    }

    /*
<?xml version="1.0"?>
<plan name="Transition [undeployed]">
  <sequential name="Transition [undeployed]">
    <leaf name="Stop /mp/i001 on host1" />
    <leaf name="Unconfigure /mp/i001 on host1" />
    <leaf name="Uninstall /mp/i001 from host1" />
    <leaf name="Uninstall script for installation /mp/i001 on host1" />
  </sequential>
</plan>
     */

    // should be 4 actions
    assertEquals(4, plan.step.steps.size())

    plan.step.steps.each {
      _leafStepExecutor.executeLeafStep(it)
    }

    expectedActions = [
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:stop, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:stopped, timeout:10s]',
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:unconfigure, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:installed, timeout:10s]',
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:uninstall, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:NONE, timeout:10s]',
        'uninstallScript [mountPoint:/mp/i001]'
    ]

    checkActions(expectedActions, _agent.log)

    _agent.clearLog()

    // testing undeployed -> running
    plan = _planner.createTransitionPlan([installation('host1', '/mp/i001', 'running'),
                                          installation('host1', '/mp/i002', 'installed'),
                                          installation('host1', '/mp/i003', 'stopped')], ['undeployed', 'running'], null) {
      it.action.descriptorProperties.mountPoint == '/mp/i001'
    }

    /*
<?xml version="1.0"?>
<plan name="Transition [undeployed-&gt;running]">
  <sequential name="Transition [undeployed-&gt;running]">
    <leaf name="Stop /mp/i001 on host1" />
    <leaf name="Unconfigure /mp/i001 on host1" />
    <leaf name="Uninstall /mp/i001 from host1" />
    <leaf name="Uninstall script for installation /mp/i001 on host1" />
    <leaf name="Install script for installation /mp/i001 on host1" />
    <leaf name="Install /mp/i001 on host1" />
    <leaf name="Configure /mp/i001 on host1" />
    <leaf name="Start /mp/i001 on host1" />
  </sequential>
</plan>
     */

    // should be 8 actions
    assertEquals(8, plan.step.steps.size())

    plan.step.steps.each {
      _leafStepExecutor.executeLeafStep(it)
    }

    expectedActions = [
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:stop, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:stopped, timeout:10s]',
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:unconfigure, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:installed, timeout:10s]',
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:uninstall, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:NONE, timeout:10s]',
        'uninstallScript [mountPoint:/mp/i001]',
        'installScript [mountPoint:/mp/i001, scriptLocation:null, parent:null, initParameters:[:]]',
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:install, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:installed, timeout:10s]',
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:configure, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:stopped, timeout:10s]',
        'clearError [mountPoint:/mp/i001]',
        'executeAction [mountPoint:/mp/i001, action:start, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2]]]',
        'waitForState [mountPoint:/mp/i001, state:running, timeout:10s]'        
    ]

    checkActions(expectedActions, _agent.log)

  }

  private Installation installation(String hostname, String mountPoint, String state)
  {
    new Installation([hostname: hostname,
                      mount: mountPoint,
                      name: mountPoint,
                      state: state])
  }
}
