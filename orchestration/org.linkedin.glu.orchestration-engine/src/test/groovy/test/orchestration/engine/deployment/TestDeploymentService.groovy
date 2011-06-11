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
   * No agent up upgrade
   */
  public void testAgentSelfUpgradeNoAgent()
  {
    SystemModel currentSystemModel = m()

    Plan<ActionDescriptor> plan = upgradePlan(currentSystemModel,
                                              ['a1', 'a2', 'a3'],
                                              Type.PARALLEL)

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

    Plan<ActionDescriptor> plan = upgradePlan(currentSystemModel,
                                              ['a1', 'a2', 'a3'],
                                              Type.PARALLEL)

    // TODO HIGH YP:  the plan generated is incorrect due to the 'bug' with transitions
    println plan.toXml()
  }

  /**
   * Nothing to cleanup
   */
  public void testAgentCleanupSelfUpgradeNoAgent()
  {
    SystemModel expectedModel = m()
    SystemModel currentModel = m()

    Plan<ActionDescriptor> plan = cleanupPlan(expectedModel, currentModel, Type.PARALLEL)

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

    Plan<ActionDescriptor> p = cleanupPlan(expectedModel, currentModel, Type.PARALLEL)

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

  private Plan<ActionDescriptor> upgradePlan(SystemModel currentSystemModel,
                                             Collection<String> agents,
                                             Type type)
  {
    currentModels[currentSystemModel.fabric] = currentSystemModel

    def params = [
      version: 'v1',
      coordinates: 'tar1',
      type: type,
      agents: agents,
      fabric: fabricService.findFabric(currentSystemModel.fabric)
    ]
    
    Collection<Plan<ActionDescriptor>> plans =
     deploymentService.computeAgentsUpgradePlan(params,
                                                [name: 'self upgrade'])
    if(plans.size() == 0)
      return null;

    return plans[0]
  }

  private Plan<ActionDescriptor> cleanupPlan(SystemModel expectedSystemModel,
                                             SystemModel currentSystemModel,
                                             Type type)
  {
    currentModels[currentSystemModel.fabric] = currentSystemModel

    Collection<Plan<ActionDescriptor>> plans =
      deploymentService.computeAgentsCleanupUpgradePlan([system: expectedSystemModel,
                                                        type: type],
                                                        null)

    if(plans.size() == 0)
      return null;

    return plans[0]
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