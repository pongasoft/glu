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

package test.orchestration.engine.deployment

import org.linkedin.glu.orchestration.engine.deployment.DeploymentServiceImpl
import org.linkedin.glu.orchestration.engine.delta.impl.DeltaMgrImpl
import org.linkedin.glu.orchestration.engine.planner.impl.PlannerImpl
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.plan.api.IStep.Type
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.orchestration.engine.fabric.FabricService
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptorAdjuster

/**
 * @author yan@pongasoft.com */
public class TestDeploymentService extends GroovyTestCase
{
  // setting a noop action descriptor adjuster to not have to deal with names
  ActionDescriptorAdjuster actionDescriptorAdjuster = {
    return it
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

  DeploymentServiceImpl deploymentService= new DeploymentServiceImpl(planner: planner,
                                                                     deltaMgr: deltaMgr,
                                                                     fabricService: fabricService,
                                                                     agentsService: agentService)
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
  <parallel>
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
  <parallel>
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

    // TODO HIGH YP:  the plan generated is incorrect due to the 'bug' with transitions (a3 is incorrect)
    assertEquals("""<?xml version="1.0"?>
<plan fabric="f1" name="self upgrade - PARALLEL">
  <parallel>
    <sequential agent="a1" mountPoint="/self/upgrade">
      <leaf agent="a1" fabric="f1" initParameters="{newVersion=v1, agentTar=tar1}" mountPoint="/self/upgrade" script="{scriptClassName=org.linkedin.glu.agent.impl.script.AutoUpgradeScript}" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="/self/upgrade" scriptAction="install" toState="installed" />
      <leaf agent="a1" fabric="f1" mountPoint="/self/upgrade" scriptAction="prepare" toState="prepared" />
      <leaf agent="a1" fabric="f1" mountPoint="/self/upgrade" scriptAction="commit" toState="upgraded" />
      <leaf agent="a1" fabric="f1" mountPoint="/self/upgrade" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a1" fabric="f1" mountPoint="/self/upgrade" scriptLifecycle="uninstallScript" />
    </sequential>
    <sequential agent="a2" mountPoint="/self/upgrade">
      <leaf agent="a2" fabric="f1" initParameters="{newVersion=v1, agentTar=tar1}" mountPoint="/self/upgrade" script="{scriptClassName=org.linkedin.glu.agent.impl.script.AutoUpgradeScript}" scriptLifecycle="installScript" />
      <leaf agent="a2" fabric="f1" mountPoint="/self/upgrade" scriptAction="install" toState="installed" />
      <leaf agent="a2" fabric="f1" mountPoint="/self/upgrade" scriptAction="prepare" toState="prepared" />
      <leaf agent="a2" fabric="f1" mountPoint="/self/upgrade" scriptAction="commit" toState="upgraded" />
      <leaf agent="a2" fabric="f1" mountPoint="/self/upgrade" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a2" fabric="f1" mountPoint="/self/upgrade" scriptLifecycle="uninstallScript" />
    </sequential>
    <sequential agent="a3" mountPoint="/self/upgrade">
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptAction="commit" toState="upgraded" />
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptAction="uninstall" toState="NONE" />
      <leaf agent="a3" fabric="f1" mountPoint="/self/upgrade" scriptLifecycle="uninstallScript" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(15, p.leafStepsCount)

    println p.toXml()
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
  <parallel>
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

    computePlan(type, null, currentSystemModel, params, "computeAgentsUpgradePlan")
  }

  private Plan<ActionDescriptor> cleanupPlan(Type type,
                                             SystemModel expectedSystemModel,
                                             SystemModel currentSystemModel)
  {
    computePlan(type, m(), currentSystemModel, null, "computeAgentsCleanupUpgradePlan")
  }

  private Plan<ActionDescriptor> bouncePlan(Type type,
                                            SystemModel expectedSystemModel,
                                            SystemModel currentSystemModel)
  {
    computePlan(type, expectedSystemModel, currentSystemModel, [name: 'bounce'], "computeBouncePlans")
  }

  private Plan<ActionDescriptor> redeployPlan(Type type,
                                              SystemModel expectedSystemModel,
                                              SystemModel currentSystemModel)
  {
    computePlan(type, expectedSystemModel, currentSystemModel, [name: 'redeploy'], "computeRedeployPlans")
  }

  private Plan<ActionDescriptor> computePlan(Type type,
                                             SystemModel expectedSystemModel,
                                             SystemModel currentSystemModel,
                                             def params,
                                             String computePlanName)
  {
    if(params == null)
      params = [:]
    params.type = type
    params.system = expectedSystemModel
    currentModels[currentSystemModel.fabric] = currentSystemModel
    try
    {
      Collection<Plan<ActionDescriptor>> plans =
        deploymentService."${computePlanName}"(params, null)

      if(plans.size() == 0)
        return null;

      return plans[0]
    }
    finally
    {
      currentModels.remove(currentSystemModel.fabric)
    }
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