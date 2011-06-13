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



package org.linkedin.glu.orchestration.engine.planner

import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.orchestration.engine.delta.DeltaMgr
import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.orchestration.engine.fabric.FabricService
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.plan.api.IStep.Type
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.util.annotations.Initializable

/**
 * System service.
 *
 * @author ypujante@linkedin.com */
class PlannerServiceImpl implements PlannerService
{
  @Initializable(required = true)
  AgentsService agentsService

  @Initializable(required = true)
  FabricService fabricService

  @Initializable(required = true)
  DeltaMgr deltaMgr

  @Initializable(required = true)
  Planner planner

  /**
   * Compute deployment plans between the system provided (params.system) and the current
   * system.
   */
  Collection<Plan<ActionDescriptor>> computeDeployPlans(params, def metadata)
  {
    computeDeploymentPlans(params, metadata) { Type type, SystemModelDelta delta ->
      planner.computeDeploymentPlan(type, delta)
    }
  }

  @Override
  Plan<ActionDescriptor> computeDeployPlan(params, def metadata)
  {
    params.type = params.type ?: Type.SEQUENTIAL
    return toSinglePlan(computeDeployPlans(params, metadata))
  }

  /**
   * Compute deployment plans by doing the following:
   * <ol>
   *   <li>compute delta between expected model (params.system) and current model (computed)
   *   <li>compute the deployment plan(s) (closure callback) (use params.type if a given type only
   *       is required
   *   <li>set various metadata on the plan(s) as well as the name
   * </ol>
   * @return a collection of plans (<code>null</code> if no expected model) which may be empty
   */
  private Collection<Plan<ActionDescriptor>> computeDeploymentPlans(params,
                                                                    def metadata,
                                                                    Closure closure)
  {
    computeDeploymentPlans(params, metadata, null, null, closure)
  }

  /**
   * Compute deployment plans by doing the following:
   * <ol>
   *   <li>compute delta between expected model (params.system) and current model (computed)
   *   <li>compute the deployment plan(s) (closure callback) (use params.type if a given type only
   *       is required
   *   <li>set various metadata on the plan(s) as well as the name
   * </ol>
   * @return a collection of plans (<code>null</code> if no expected model) which may be empty
   */
  private Collection<Plan<ActionDescriptor>> computeDeploymentPlans(params,
                                                                    def metadata,
                                                                    def expectedModelFilter,
                                                                    def currentModelFiter,
                                                                    Closure closure)
  {
    SystemModel expectedModel = params.system

    if(!expectedModel)
      return null

    if(expectedModelFilter)
      expectedModel = expectedModel.filterBy(expectedModelFilter)

    Fabric fabric = fabricService.findFabric(expectedModel.fabric)

    if(!fabric)
      throw new IllegalArgumentException("unknown fabric ${expectedModel.fabric}")

    SystemModel currentModel = agentsService.getCurrentSystemModel(fabric)

    if(currentModelFiter)
      currentModel = currentModel.filterBy(currentModelFiter)

    computeDeploymentPlans(params, expectedModel, currentModel, metadata, closure)
  }

  /**
   * Compute deployment plans by doing the following:
   * <ol>
   *   <li>compute delta between expected model and current model (computed)
   *   <li>compute the deployment plan(s) (closure callback) (use params.type if a given type only
   *       is required
   *   <li>set various metadata on the plan(s) as well as the name
   * </ol>
   * @return a collection of plans (<code>null</code> if no expected model) which may be empty
   */
  private Collection<Plan<ActionDescriptor>> computeDeploymentPlans(params,
                                                                    SystemModel expectedModel,
                                                                    SystemModel currentModel,
                                                                    def metadata,
                                                                    Closure closure)
  {
    // 1. compute delta between expectedModel and currentModel
    SystemModelDelta delta = deltaMgr.computeDelta(expectedModel, currentModel)

    Collection<Type> types = []
    if(params.type)
      types << Type.valueOf(params.type.toString())
    else
      types = [Type.SEQUENTIAL, Type.PARALLEL]

    if(metadata == null)
      metadata = [:]

    types.collect { Type type ->
      // 2. compute the deployment plan for the delta (and the given type)
      Plan<ActionDescriptor> plan = closure(type, delta)

      // 3. set name and metadata for the plan
      plan.setMetadata('fabric', expectedModel.fabric)
      plan.setMetadata('systemId', expectedModel.id)
      plan.setMetadata(metadata)

      plan.step?.metadata?.putAll(metadata)

      def name = params.name ?: metadata.name
      if(!name)
      {
        name = metadata.collect { k,v -> "${k}=$v"}.join(' - ')
      }
      name = "${name} - ${type}".toString()
      plan.name = name

      if(!plan.step?.metadata)
        plan.step?.metadata?.name = name

      return plan
    }.findAll { Plan plan -> plan.hasLeafSteps() }
  }

  /**
   * Computes a transition plan.
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeTransitionPlans(params, def metadata)
  {
    computeDeploymentPlans(params, metadata) { Type type, SystemModelDelta delta ->
      planner.computeTransitionPlan(type, delta, [params.state])
    }
  }

  @Override
  Plan<ActionDescriptor> computeTransitionPlan(params, def metadata)
  {
    params.type = params.type ?: Type.SEQUENTIAL
    return toSinglePlan(computeTransitionPlans(params, metadata))
  }

  // we filter by entries where the current state is 'running' or 'stopped'
  private def bounceCurrentModelFilter = { SystemEntry entry ->
      entry.entryState == 'running' || entry.entryState == 'stopped'
  }

  /**
   * Compute a bounce plan to bounce (= stop/start) containers.
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeBouncePlans(params, def metadata)
  {
    computeDeploymentPlans(params,
                           metadata,
                           null,
                           bounceCurrentModelFilter) { Type type, SystemModelDelta delta ->
      planner.computeTransitionPlan(type, delta, ['stopped', 'running'])
    }
  }

  @Override
  Plan<ActionDescriptor> computeBouncePlan(params, def metadata)
  {
    params.type = params.type ?: Type.SEQUENTIAL
    return toSinglePlan(computeBouncePlans(params, metadata))
  }

  /**
   * Compute an undeploy plan.
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeUndeployPlans(params, def metadata)
  {
    computeDeploymentPlans(params, metadata) { Type type, SystemModelDelta delta ->
      planner.computeTransitionPlan(type, delta, [null])
    }
  }

  @Override
  Plan<ActionDescriptor> computeUndeployPlan(params, def metadata)
  {
    params.type = params.type ?: Type.SEQUENTIAL
    return toSinglePlan(computeUndeployPlans(params, metadata))
  }

  // the purpose of this filter is to exclude entries that should not be deployed in the first
  // place (should not be part of 'redeploy' plan!
  private def redeployCurrentModelFilter = { SystemEntry entry ->
      true
  }

  /**
   * Compute a redeploy plan (= undeploy/deploy).
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeRedeployPlans(params, def metadata)
  {
    computeDeploymentPlans(params,
                           metadata,
                           null,
                           redeployCurrentModelFilter) { Type type, SystemModelDelta delta ->
      planner.computeTransitionPlan(type, delta, [null, '<expected>'])
    }
  }

  @Override
  Plan<ActionDescriptor> computeRedeployPlan(params, def metadata)
  {
    params.type = params.type ?: Type.SEQUENTIAL
    return toSinglePlan(computeRedeployPlans(params, metadata))
  }

  /**
   * Computes the deployment plan for upgrading agents
   * @param metadata any metadata to add to the plan(s)
   */
  @Override
  Plan<ActionDescriptor> computeAgentsUpgradePlan(params, def metadata)
  {
    params.type = params.type ?: Type.SEQUENTIAL
    SystemModel currentModel = agentsService.getCurrentSystemModel(params.fabric)
    def agents = (params.agents ?: []) as Set
    def filteredCurrentModel = currentModel.filterBy { SystemEntry entry ->
      agents.contains(entry.agent)
    }

    // we keep only the agents that are part of the current model!
    agents = new HashSet()
    filteredCurrentModel.each { SystemEntry entry ->
      agents << entry.agent
    }

    SystemModel expectedModel = new SystemModel(fabric: currentModel.fabric)
    agents.each { String agent ->
      SystemEntry entry = new SystemEntry(agent: agent,
                                          mountPoint: PlannerService.AGENT_SELF_UPGRADE_MOUNT_POINT,
                                          entryState: 'upgraded')
      entry.script = [scriptClassName: "org.linkedin.glu.agent.impl.script.AutoUpgradeScript"]
      entry.initParameters = [
        newVersion: params.version,
        agentTar: params.coordinates,
      ]
      expectedModel.addEntry(entry)
    }

    expectedModel = expectedModel.filterBy { SystemEntry entry ->
      entry.mountPoint == PlannerService.AGENT_SELF_UPGRADE_MOUNT_POINT
    }

    toSinglePlan(computeDeploymentPlans(params,
                                        expectedModel,
                                        currentModel,
                                        metadata) { Type type, SystemModelDelta delta ->
      planner.computeTransitionPlan(type, delta, ['<expected>', null])
    })
  }

  private def agentsCleanupUpgradeExpectedModelFilter = { SystemEntry entry ->
      entry.mountPoint == PlannerService.AGENT_SELF_UPGRADE_MOUNT_POINT
  }

  /**
   * Computes the deployment plan for cleaning any upgrade that failed
   * @param metadata any metadata to add to the plan(s)
   */
  @Override
  Plan<ActionDescriptor> computeAgentsCleanupUpgradePlan(params, def metadata)
  {
    params.type = params.type ?: Type.SEQUENTIAL
    toSinglePlan(computeDeploymentPlans(params,
                                        metadata,
                                        agentsCleanupUpgradeExpectedModelFilter,
                                        null) { Type type, SystemModelDelta delta ->
      planner.computeDeploymentPlan(type, delta)
    })
  }

  protected Plan<ActionDescriptor> toSinglePlan(Collection<Plan<ActionDescriptor>> plans)
  {
    if(plans == null || plans.size() == 0)
      return null
    return plans[0]
  }
}

