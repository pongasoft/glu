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

package test.orchestration.engine.planner

import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor
import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptorAdjuster
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.orchestration.engine.delta.impl.DeltaMgrImpl

import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.orchestration.engine.fabric.FabricService
import org.linkedin.glu.orchestration.engine.planner.impl.PlannerImpl
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.plan.api.IStep.Type
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.orchestration.engine.planner.PlannerServiceImpl
import org.linkedin.glu.provisioner.core.model.JSONSystemModelSerializer
import org.linkedin.glu.groovy.utils.plugins.PluginServiceImpl

/**
 * @author yan@pongasoft.com */
public class TestPlannerService extends GroovyTestCase
{
  // setting a noop action descriptor adjuster to not have to deal with names
  ActionDescriptorAdjuster actionDescriptorAdjuster = { smd, ad ->
    return ad
  } as ActionDescriptorAdjuster
  PlannerImpl planner = new PlannerImpl(actionDescriptorAdjuster: actionDescriptorAdjuster)
  DeltaMgrImpl deltaMgr = new DeltaMgrImpl()

  FabricService fabricService = [
    findFabric: { String fabricName -> new Fabric(name: fabricName)}
  ] as FabricService

  Map<String, SystemModel> currentModels = [:]
  AgentsService agentService = [
    getCurrentSystemModel: { Fabric fabric -> currentModels[fabric.name] }
  ] as AgentsService

  PluginServiceImpl pluginService = new PluginServiceImpl()
  PlannerServiceImpl plannerService = new PlannerServiceImpl(planner: planner,
                                                             deltaMgr: deltaMgr,
                                                             fabricService: fabricService,
                                                             agentsService: agentService,
                                                             planIdFactory: { null },
                                                             pluginService: pluginService)
  /**
   * Test for bounce plan
   */
  public void testBouncePlan()
  {
    SystemModel expectedModel =
      m(
        [agent: 'a2', mountPoint: '/m1', script: 's1'],
        [agent: 'a2', mountPoint: '/m2', script: 's1'],
        [agent: 'a2', mountPoint: '/m3', script: 's1'],
        [agent: 'a2', mountPoint: '/m4', script: 's1']
      )

    SystemModel currentSystemModel =
      m(
        [agent: 'a2', mountPoint: '/m1', script: 's1'],
        [agent: 'a2', mountPoint: '/m2', script: 's1', entryState: 'stopped'],
        [agent: 'a2', mountPoint: '/m3', script: 's1', entryState: 'installed'])

    Plan<ActionDescriptor> p = bouncePlan(Type.PARALLEL, expectedModel, currentSystemModel)

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="bounce - PARALLEL">
  <parallel name="bounce - PARALLEL">
    <sequential agent="a2" mountPoint="/m1">
      <leaf agent="a2" fabric="f1" mountPoint="/m1" scriptAction="stop" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m1" scriptAction="start" toState="running" />
    </sequential>
    <sequential agent="a2" mountPoint="/m2">
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="start" toState="running" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(3, p.leafStepsCount)
  }

  /**
   * Test for bounce plan when transition
   */
  public void testBouncePlanWithTransition()
  {
    SystemModel expectedModel =
      m(
        [agent: 'a2', mountPoint: '/m1', script: 's1'],
        [agent: 'a2', mountPoint: '/m2', script: 's1'],
        [agent: 'a2', mountPoint: '/m3', script: 's1'],
        [agent: 'a2', mountPoint: '/m4', script: 's1']
      )

    SystemModel currentSystemModel =
      m(
        [agent: 'a2', mountPoint: '/m1', script: 's1', metadata: [transitionState: 'installed->stopped']],
        [agent: 'a2', mountPoint: '/m2', script: 's1'])

    Plan<ActionDescriptor> p = bouncePlan(Type.PARALLEL, expectedModel, currentSystemModel)

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="bounce - PARALLEL">
  <parallel name="bounce - PARALLEL">
    <sequential agent="a2" mountPoint="/m1">
      <leaf action="noop" agent="a2" fabric="f1" mountPoint="/m1" reason="alreadyInTransition" transitionState="installed-&gt;stopped" />
    </sequential>
    <sequential agent="a2" mountPoint="/m2">
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="stop" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="start" toState="running" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(3, p.leafStepsCount)
  }

  public void testBounceWithTags()
  {
    Plan<ActionDescriptor> p

    SystemModel expectedModel =
      m(
        [agent: 'a2', mountPoint: '/m1', script: 's1'],
        [agent: 'a2', mountPoint: '/m2', script: 's1', tags: ['t1']],
      ).filterBy("tags='t1'")

    SystemModel currentModel =
      m(
        [agent: 'a2', mountPoint: '/m1', script: 's1'],
        [agent: 'a2', mountPoint: '/m2', script: 's1'],
        )

    p = bouncePlan(Type.PARALLEL, expectedModel, currentModel)

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="bounce - PARALLEL">
  <parallel name="bounce - PARALLEL">
    <sequential agent="a2" mountPoint="/m2">
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="stop" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="start" toState="running" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(2, p.leafStepsCount)
  }

  /**
   * Test for redeploy plan
   */
  public void testRedeployPlan()
  {
    SystemModel expectedModel =
      m(
        [agent: 'a2', mountPoint: '/m1', script: 's1'],
        [agent: 'a2', mountPoint: '/m2', script: 's1'],
        [agent: 'a2', mountPoint: '/m3', script: 's1', entryState: 'installed']
      )

    SystemModel currentSystemModel =
      m(
        [agent: 'a2', mountPoint: '/m1', script: 's1'],
        [agent: 'a2', mountPoint: '/m2', script: 's1', entryState: 'stopped'],
        [agent: 'a2', mountPoint: '/m3', script: 's1'],
        [agent: 'a2', mountPoint: '/m4', script: 's1'],
      )

    Plan<ActionDescriptor> p = redeployPlan(Type.PARALLEL, expectedModel, currentSystemModel)

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="redeploy - PARALLEL">
  <parallel name="redeploy - PARALLEL">
    <sequential agent="a2" mountPoint="/m1">
      <leaf agent="a2" fabric="f1" mountPoint="/m1" scriptAction="stop" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m1" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a2" fabric="f1" mountPoint="/m1" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a2" fabric="f1" mountPoint="/m1" scriptLifecycle="uninstallScript" />
      <leaf agent="a2" fabric="f1" mountPoint="/m1" script="s1" scriptLifecycle="installScript" />
      <leaf agent="a2" fabric="f1" mountPoint="/m1" scriptAction="install" toState="installed" />
      <leaf agent="a2" fabric="f1" mountPoint="/m1" scriptAction="configure" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m1" scriptAction="start" toState="running" />
    </sequential>
    <sequential agent="a2" mountPoint="/m2">
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptLifecycle="uninstallScript" />
      <leaf agent="a2" fabric="f1" mountPoint="/m2" script="s1" scriptLifecycle="installScript" />
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="install" toState="installed" />
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="configure" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="start" toState="running" />
    </sequential>
    <sequential agent="a2" mountPoint="/m3">
      <leaf agent="a2" fabric="f1" mountPoint="/m3" scriptAction="stop" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m3" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a2" fabric="f1" mountPoint="/m3" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a2" fabric="f1" mountPoint="/m3" scriptLifecycle="uninstallScript" />
      <leaf agent="a2" fabric="f1" mountPoint="/m3" script="s1" scriptLifecycle="installScript" />
      <leaf agent="a2" fabric="f1" mountPoint="/m3" scriptAction="install" toState="installed" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(21, p.leafStepsCount)
  }

  /**
   * No agent up upgrade
   */
  public void testAgentSelfUpgradeNoAgent()
  {
    SystemModel currentSystemModel = m()

    Plan<ActionDescriptor> plan = upgradePlan(Type.PARALLEL,
                                              currentSystemModel,
                                              ['a1', 'a2', 'a3'])

    // no agent to upgrade! => no plan
    assertNull(plan)
  }

  /**
   * a1 is empty agent, a2 has an entry, a3 has already a self upgrade entry
   */
  public void testAgentSelfUpgrade()
  {
    SystemModel currentSystemModel =
      m([agent: 'a1', metadata: [emptyAgent: true, currentState: 'NA']],
        [agent: 'a2', mountPoint: '/m1', script: 's1'],
        [agent: 'a3', mountPoint: '/self/upgrade',
         script: [scriptClassName: "org.linkedin.glu.agent.impl.script.AutoUpgradeScript"],
         initParameters: [newVersion: 'v0', agentTar: 'tar0'],
         entryState: 'prepared'])

    Plan<ActionDescriptor> p = upgradePlan(Type.PARALLEL,
                                           currentSystemModel,
                                           ['a1', 'a2', 'a3'])

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="self upgrade - PARALLEL">
  <parallel name="self upgrade - PARALLEL">
    <sequential agent="a1" mountPoint="/self/upgrade">
      <leaf agent="a1" fabric="f1" initParameters="{agentTar=tar1, newVersion=v1}" mountPoint="/self/upgrade" script="{scriptClassName=org.linkedin.glu.agent.impl.script.AutoUpgradeScript}" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="/self/upgrade" scriptAction="install" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="/self/upgrade" scriptAction="prepare" toState="prepared" />
      <leaf agent="a1" fabric="f1" mountPoint="/self/upgrade" scriptAction="commit" toState="upgraded" />
      <leaf agent="a1" fabric="f1" mountPoint="/self/upgrade" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="/self/upgrade" scriptLifecycle="uninstallScript" />
    </sequential>
    <sequential agent="a2" mountPoint="/self/upgrade">
      <leaf agent="a2" fabric="f1" initParameters="{agentTar=tar1, newVersion=v1}" mountPoint="/self/upgrade" script="{scriptClassName=org.linkedin.glu.agent.impl.script.AutoUpgradeScript}" scriptLifecycle="installScript" />
      <leaf agent="a2" fabric="f1" mountPoint="/self/upgrade" scriptAction="install" toState="installed" />
      <leaf agent="a2" fabric="f1" mountPoint="/self/upgrade" scriptAction="prepare" toState="prepared" />
      <leaf agent="a2" fabric="f1" mountPoint="/self/upgrade" scriptAction="commit" toState="upgraded" />
      <leaf agent="a2" fabric="f1" mountPoint="/self/upgrade" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a2" fabric="f1" mountPoint="/self/upgrade" scriptLifecycle="uninstallScript" />
    </sequential>
    <sequential agent="a3" mountPoint="/self/upgrade">
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptAction="rollback" toState="installed" />
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptLifecycle="uninstallScript" />
      <leaf agent="a3" fabric="f1" initParameters="{agentTar=tar1, newVersion=v1}" mountPoint="/self/upgrade" script="{scriptClassName=org.linkedin.glu.agent.impl.script.AutoUpgradeScript}" scriptLifecycle="installScript" />
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptAction="install" toState="installed" />
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptAction="prepare" toState="prepared" />
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptAction="commit" toState="upgraded" />
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptLifecycle="uninstallScript" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(21, p.leafStepsCount)
  }

  /**
   * Nothing to cleanup
   */
  public void testAgentCleanupSelfUpgradeNoAgent()
  {
    SystemModel expectedModel = m()
    SystemModel currentModel = m()

    Plan<ActionDescriptor> plan = cleanupPlan(Type.PARALLEL, expectedModel, currentModel)

    assertNull(plan)
  }

  /**
   * 1 agent in 'prepared' state, other in 'upgraded' state
   */
  public void testAgentCleanupSelfUpgrade()
  {
    SystemModel expectedModel = m()
    SystemModel currentModel =
    m([agent: 'a1', metadata: [emptyAgent: true, currentState: 'NA']],
      [agent: 'a2', mountPoint: '/m1', script: 's1'],
      [agent: 'a3', mountPoint: '/self/upgrade',
       script: [scriptClassName: "org.linkedin.glu.agent.impl.script.AutoUpgradeScript"],
       initParameters: [newVersion: 'v0', agentTar: 'tar0'],
       entryState: 'prepared'],
      [agent: 'a4', mountPoint: '/self/upgrade',
      script: [scriptClassName: "org.linkedin.glu.agent.impl.script.AutoUpgradeScript"],
      initParameters: [newVersion: 'v0', agentTar: 'tar0'],
      entryState: 'upgraded'])

    Plan<ActionDescriptor> p = cleanupPlan(Type.PARALLEL,
                                           expectedModel,
                                           currentModel)

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name=" - PARALLEL">
  <parallel name=" - PARALLEL">
    <sequential agent="a3" mountPoint="/self/upgrade">
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptAction="rollback" toState="installed" />
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptLifecycle="uninstallScript" />
    </sequential>
    <sequential agent="a4" mountPoint="/self/upgrade">
      <leaf agent="a4" fabric="f1" mountPoint="/self/upgrade" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a4" fabric="f1" mountPoint="/self/upgrade" scriptLifecycle="uninstallScript" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(5, p.leafStepsCount)
  }

  String parentFilter = "mountPoint='p1'"
  String childFilter = "mountPoint='c1'"

  /**
   * Test for bounce for parent/child
   */
  public void testBouncePlanWithParentChildNoFilter()
  {
    // bounce
    Plan<ActionDescriptor> p = bouncePlan(Type.PARALLEL,
                                          m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                                            [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                                            [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']),

                                          m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                                            [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                                            [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="bounce - PARALLEL">
  <sequential name="bounce - PARALLEL">
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="start" toState="running" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="start" toState="running" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="start" toState="running" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(6, p.leafStepsCount)
  }
  
  /**
   * Test for bounce for parent/child
   */
  public void testBouncePlanWithParentChildWithParentFilter()
  {
    Plan<ActionDescriptor> p

    // bounce (parent only through filter => child is included anyway)
    p = bouncePlan(Type.PARALLEL,
                   m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']).filterBy(parentFilter),

                   m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="bounce - PARALLEL">
  <sequential name="bounce - PARALLEL">
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="start" toState="running" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="start" toState="running" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="start" toState="running" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(6, p.leafStepsCount)
  }

  public void testBouncePlanWithParentChildWithChildFilter()
  {
    // bounce (child only through filter => parent not included)
    Plan<ActionDescriptor> p =
      bouncePlan(Type.PARALLEL,
                 m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                   [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                   [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']).filterBy(childFilter),

                 m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                   [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                   [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="bounce - PARALLEL">
  <parallel name="bounce - PARALLEL">
    <sequential agent="a1" mountPoint="c1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="start" toState="running" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(2, p.leafStepsCount)

    // bounce (child only through filter => parent included because not started)
    p = bouncePlan(Type.PARALLEL,
                   m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']).filterBy(childFilter),

                   m([agent: 'a1', mountPoint: 'p1', script: 's1', entryState: 'stopped'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1', entryState: 'stopped'],
                     [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1', entryState: 'stopped']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="bounce - PARALLEL">
  <sequential name="bounce - PARALLEL">
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="start" toState="running" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="start" toState="running" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(2, p.leafStepsCount)
  }

  /**
   * Test for undeploy for parent/child
   */
  public void testUndeployPlanWithParentChildNoFilter()
  {
    Plan<ActionDescriptor> p

    // undeployed 
    p = undeployPlan(Type.PARALLEL,
                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']),

                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="undeploy - PARALLEL">
  <sequential name="undeploy - PARALLEL">
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="unconfigure" toState="installed" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="uninstall" toState="NONE" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptLifecycle="uninstallScript" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(12, p.leafStepsCount)
  }

  /**
   * Test for undeploy for parent/child
   */
  public void testUndeployPlanWithParentChildWithParentFilter()
  {
    Plan<ActionDescriptor> p

    // undeploy (parent only through filter => child is included anyway)
    p = undeployPlan(Type.PARALLEL,
                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']).filterBy(parentFilter),

                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="undeploy - PARALLEL">
  <sequential name="undeploy - PARALLEL">
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="unconfigure" toState="installed" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="uninstall" toState="NONE" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptLifecycle="uninstallScript" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(12, p.leafStepsCount)
  }

  /**
   * Test for undeploy for parent/child
   */
  public void testUndeployPlanWithParentChildWithChildFilter()
  {
    Plan<ActionDescriptor> p

    // undeploy (parent only through filter => child is included anyway)
    p = undeployPlan(Type.PARALLEL,
                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']).filterBy(childFilter),

                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="undeploy - PARALLEL">
  <parallel name="undeploy - PARALLEL">
    <sequential agent="a1" mountPoint="c1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptLifecycle="uninstallScript" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(4, p.leafStepsCount)
  }

  /**
   * Test for redeploy for parent/child
   */
  public void testRedeployPlanWithParentChildNoFilter()
  {
    Plan<ActionDescriptor> p

    // redeploy everything
    p = redeployPlan(Type.PARALLEL,
                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']),

                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="redeploy - PARALLEL">
  <sequential name="redeploy - PARALLEL">
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="unconfigure" toState="installed" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="uninstall" toState="NONE" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptLifecycle="uninstallScript" />
    </parallel>
    <parallel depth="5">
      <leaf agent="a1" fabric="f1" mountPoint="p1" script="s1" scriptLifecycle="installScript" />
    </parallel>
    <parallel depth="6">
      <leaf agent="a1" fabric="f1" mountPoint="c1" parent="p1" script="s1" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" parent="p1" script="s1" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="install" toState="installed" />
    </parallel>
    <parallel depth="7">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="install" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="install" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="configure" toState="stopped" />
    </parallel>
    <parallel depth="8">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="configure" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="configure" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="start" toState="running" />
    </parallel>
    <parallel depth="9">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="start" toState="running" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="start" toState="running" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(24, p.leafStepsCount)
  }

  /**
   * Test for redeploy for parent/child
   */
  public void testRedeployPlanWithParentChildWithChildFilter()
  {
    Plan<ActionDescriptor> p

    // redeploy (child only through filter => parent not included)
    p = redeployPlan(Type.PARALLEL,
                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']).filterBy(childFilter),

                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="redeploy - PARALLEL">
  <parallel name="redeploy - PARALLEL">
    <sequential agent="a1" mountPoint="c1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" parent="p1" script="s1" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="install" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="configure" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="start" toState="running" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(8, p.leafStepsCount)
  }

  /**
   * Test for redeploy for parent/child (child fully undeployed first)
   */
  public void testRedeployPlanWithParentChildWithChildFilter2()
  {
    Plan<ActionDescriptor> p

    // redeploy (child only through filter => parent not included)
    p = redeployPlan(Type.PARALLEL,
                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']).filterBy(childFilter),

                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="redeploy - PARALLEL">
  <parallel name="redeploy - PARALLEL">
    <sequential agent="a1" mountPoint="c1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" parent="p1" script="s1" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="install" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="configure" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="start" toState="running" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(4, p.leafStepsCount)
  }

  /**
   * Test for redeploy for parent/child
   */
  public void testRedeployPlanWithParentChildWithParentFilter()
  {
    Plan<ActionDescriptor> p

    // redeploy (parent only through filter => child is included anyway)
    p = redeployPlan(Type.PARALLEL,
                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']).filterBy(parentFilter),

                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="redeploy - PARALLEL">
  <sequential name="redeploy - PARALLEL">
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="unconfigure" toState="installed" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="uninstall" toState="NONE" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptLifecycle="uninstallScript" />
    </parallel>
    <parallel depth="5">
      <leaf agent="a1" fabric="f1" mountPoint="p1" script="s1" scriptLifecycle="installScript" />
    </parallel>
    <parallel depth="6">
      <leaf agent="a1" fabric="f1" mountPoint="c1" parent="p1" script="s1" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" parent="p1" script="s1" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="install" toState="installed" />
    </parallel>
    <parallel depth="7">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="install" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="install" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="configure" toState="stopped" />
    </parallel>
    <parallel depth="8">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="configure" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="configure" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="start" toState="running" />
    </parallel>
    <parallel depth="9">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="start" toState="running" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" scriptAction="start" toState="running" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(24, p.leafStepsCount)

    // redeploy (parent only through filter => child is included anyway but should stop at desired state)
    p = redeployPlan(Type.PARALLEL,
                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1', entryState: 'installed']).filterBy(parentFilter),

                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1', entryState: 'installed']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="redeploy - PARALLEL">
  <sequential name="redeploy - PARALLEL">
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="stop" toState="stopped" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="unconfigure" toState="installed" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="uninstall" toState="NONE" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptLifecycle="uninstallScript" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="p1" script="s1" scriptLifecycle="installScript" />
    </parallel>
    <parallel depth="5">
      <leaf agent="a1" fabric="f1" mountPoint="c1" parent="p1" script="s1" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="install" toState="installed" />
    </parallel>
    <parallel depth="6">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="install" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="configure" toState="stopped" />
    </parallel>
    <parallel depth="7">
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="start" toState="running" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(12, p.leafStepsCount)
  }

  /**
   * Test for NPE caused when undeploying parent/child in expected model not present in current 
   * model
   */
  public void testParentChildGlu115()
  {
    Plan<ActionDescriptor> p

    // p1 is not present in the current model and in the expected model c1 is a child of p1
    p = undeployPlan(Type.PARALLEL,
                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']),

                     m([agent: 'a1', mountPoint: 'c1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="undeploy - PARALLEL">
  <parallel name="undeploy - PARALLEL">
    <sequential agent="a1" mountPoint="c1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptLifecycle="uninstallScript" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(4, p.leafStepsCount)

    // p1 is not present in the current model and in the expected model c1 is a child of p1
    p = redeployPlan(Type.PARALLEL,
                     m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                       [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']),

                     m([agent: 'a1', mountPoint: 'c1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="redeploy - PARALLEL">
  <sequential name="redeploy - PARALLEL">
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" script="s1" scriptLifecycle="installScript" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="install" toState="installed" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="configure" toState="stopped" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="start" toState="running" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="c1" parent="p1" script="s1" scriptLifecycle="installScript" />
    </parallel>
    <parallel depth="5">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="install" toState="installed" />
    </parallel>
    <parallel depth="6">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="configure" toState="stopped" />
    </parallel>
    <parallel depth="7">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="start" toState="running" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(12, p.leafStepsCount)

    // p1 is not present in the current model and in the expected model c1 is a child of p1
    p = deployPlan(Type.PARALLEL,
                   m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']),

                   m([agent: 'a1', mountPoint: 'c1', script: 's1']))

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="deploy - PARALLEL">
  <sequential name="deploy - PARALLEL">
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="stop" toState="stopped" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" script="s1" scriptLifecycle="installScript" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="install" toState="installed" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="configure" toState="stopped" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" scriptAction="start" toState="running" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="c1" parent="p1" script="s1" scriptLifecycle="installScript" />
    </parallel>
    <parallel depth="5">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="install" toState="installed" />
    </parallel>
    <parallel depth="6">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="configure" toState="stopped" />
    </parallel>
    <parallel depth="7">
      <leaf agent="a1" fabric="f1" mountPoint="c1" scriptAction="start" toState="running" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(12, p.leafStepsCount)
  }

  /**
   * Test for stop plan when empty (issue #129)
   */
  public void testStopPlanWhenEmpty()
  {
    SystemModel expectedModel = m("""{
  "fabric": "glu-dev-1",
  "id": "94fe0badb83b6d0203db4eb8a21bd455469333a7",
  "metadata": {"name": "Empty System Model"}
}
""")

    SystemModel currentSystemModel = m("""{
  "entries": [{
    "agent": "agent-1",
    "metadata": {
      "currentState": "NA",
      "emptyAgent": true
    }
  }],
  "fabric": "glu-dev-1",
  "metadata": {"accuracy": "ACCURATE"}
}
""")

    Plan<ActionDescriptor> p = transitionPlan(Type.PARALLEL, expectedModel, currentSystemModel, "stopped")
    assertNull(p)
  }

  public void testComputePlanPlugin()
  {
    pluginService.initializePlugin(
      [
        PlannerService_pre_computePlans: { args ->
          switch(args.params.planType)
          {
            case "customPlan":
              args.params.state = "installed"
              return plannerService.computeTransitionPlans(args.params, args.metadata)
              break

            default:
              return null
          }
        },
        PlannerService_post_computePlans: { args ->
          args.serviceResult[0].metadata.fromCustomPlan = 'post'
          return null
        },
      ], [:])


    SystemModel expectedModel =
      m(
        [agent: 'a2', mountPoint: '/m1', script: 's1'],
        [agent: 'a2', mountPoint: '/m2', script: 's1'],
        [agent: 'a2', mountPoint: '/m3', script: 's1', entryState: 'installed']
      )

    SystemModel currentSystemModel =
      m(
        [agent: 'a2', mountPoint: '/m1', script: 's1'],
        [agent: 'a2', mountPoint: '/m2', script: 's1', entryState: 'stopped'],
        [agent: 'a2', mountPoint: '/m3', script: 's1'],
        [agent: 'a2', mountPoint: '/m4', script: 's1'],
      )

    // will trigger the custom plan switch statement
    Plan<ActionDescriptor> p = computePlan(Type.PARALLEL,
                                           expectedModel,
                                           currentSystemModel,
                                           [name: 'customPlan'])

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="customPlan - PARALLEL" fromCustomPlan="post">
  <parallel name="customPlan - PARALLEL">
    <sequential agent="a2" mountPoint="/m1">
      <leaf agent="a2" fabric="f1" mountPoint="/m1" scriptAction="stop" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m1" scriptAction="unconfigure" toState="installed" />
    </sequential>
    <sequential agent="a2" mountPoint="/m2">
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="unconfigure" toState="installed" />
    </sequential>
    <sequential agent="a2" mountPoint="/m3">
      <leaf agent="a2" fabric="f1" mountPoint="/m3" scriptAction="stop" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m3" scriptAction="unconfigure" toState="installed" />
    </sequential>
    <sequential agent="a2" mountPoint="/m4">
      <leaf agent="a2" fabric="f1" mountPoint="/m4" scriptAction="stop" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m4" scriptAction="unconfigure" toState="installed" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(7, p.leafStepsCount)

    // will trigger the "default" path in the switch statement and verify that the post
    // sequence gets invoked properly
    p = deployPlan(Type.PARALLEL, expectedModel, currentSystemModel)

    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="deploy - PARALLEL" fromCustomPlan="post">
  <parallel name="deploy - PARALLEL">
    <sequential agent="a2" mountPoint="/m2">
      <leaf agent="a2" fabric="f1" mountPoint="/m2" scriptAction="start" toState="running" />
    </sequential>
    <sequential agent="a2" mountPoint="/m3">
      <leaf agent="a2" fabric="f1" mountPoint="/m3" scriptAction="stop" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m3" scriptAction="unconfigure" toState="installed" />
    </sequential>
    <sequential agent="a2" mountPoint="/m4">
      <leaf agent="a2" fabric="f1" mountPoint="/m4" scriptAction="stop" toState="stopped" />
      <leaf agent="a2" fabric="f1" mountPoint="/m4" scriptAction="unconfigure" toState="installed" />
      <leaf agent="a2" fabric="f1" mountPoint="/m4" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a2" fabric="f1" mountPoint="/m4" scriptLifecycle="uninstallScript" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(7, p.leafStepsCount)
  }

  private Plan<ActionDescriptor> upgradePlan(Type type,
                                             SystemModel currentSystemModel,
                                             Collection<String> agents)
  {
    def params = [
      version: 'v1',
      coordinates: 'tar1',
      type: type,
      agents: agents,
      name: 'self upgrade',
      fabric: fabricService.findFabric(currentSystemModel.fabric)
    ]

    computePlan(type, null, currentSystemModel, params) { args ->
      return plannerService.computeAgentsUpgradePlan(args, null)
    }
  }

  private Plan<ActionDescriptor> cleanupPlan(Type type,
                                             SystemModel expectedSystemModel,
                                             SystemModel currentSystemModel)
  {
    computePlan(type, m(), currentSystemModel, null) { params ->
      return plannerService.computeAgentsCleanupUpgradePlan(params, null)
    }
  }

  private Plan<ActionDescriptor> bouncePlan(Type type,
                                            SystemModel expectedSystemModel,
                                            SystemModel currentSystemModel)
  {
    computePlan(type, expectedSystemModel, currentSystemModel, [name: 'bounce'])
  }

  private Plan<ActionDescriptor> undeployPlan(Type type,
                                              SystemModel expectedSystemModel,
                                              SystemModel currentSystemModel)
  {
    computePlan(type, expectedSystemModel, currentSystemModel, [name: 'undeploy'])
  }


  private Plan<ActionDescriptor> redeployPlan(Type type,
                                              SystemModel expectedSystemModel,
                                              SystemModel currentSystemModel)
  {
    computePlan(type, expectedSystemModel, currentSystemModel, [name: 'redeploy'])
  }

  private Plan<ActionDescriptor> deployPlan(Type type,
                                            SystemModel expectedSystemModel,
                                            SystemModel currentSystemModel)
  {
    computePlan(type, expectedSystemModel, currentSystemModel, [name: 'deploy'])
  }

  private Plan<ActionDescriptor> transitionPlan(Type type,
                                                SystemModel expectedSystemModel,
                                                SystemModel currentSystemModel,
                                                String state)
  {
    computePlan(type, expectedSystemModel, currentSystemModel, [name: 'transition', state: state])
  }

  private Plan<ActionDescriptor> computePlan(Type type,
                                             SystemModel expectedSystemModel,
                                             SystemModel currentSystemModel,
                                             def params)
  {
    if(params == null)
      params = [:]
    params.stepType = type
    params.system = expectedSystemModel
    params.planType = params.name
    currentModels[currentSystemModel.fabric] = currentSystemModel
    try
    {
      return plannerService.computePlan(params, null)
    }
    finally
    {
      currentModels.remove(currentSystemModel.fabric)
    }
  }

  private Plan<ActionDescriptor> computePlan(Type type,
                                             SystemModel expectedSystemModel,
                                             SystemModel currentSystemModel,
                                             def params,
                                             Closure closure)
  {
    if(params == null)
      params = [:]
    params.stepType = type
    params.system = expectedSystemModel
    currentModels[currentSystemModel.fabric] = currentSystemModel
    try
    {
      return closure(params)
    }
    finally
    {
      currentModels.remove(currentSystemModel.fabric)
    }
  }


  private SystemModel m(String model)
  {
    JSONSystemModelSerializer.INSTANCE.deserialize(model)
  }

  private SystemModel m(Map... entries)
  {
    SystemModel model = new SystemModel(fabric: "f1")


    entries.each {
      model.addEntry(SystemEntry.fromExternalRepresentation(it))
    }

    return model
  }
}