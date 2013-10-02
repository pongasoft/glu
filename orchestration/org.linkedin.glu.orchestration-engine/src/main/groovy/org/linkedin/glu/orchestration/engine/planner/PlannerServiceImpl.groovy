/*
 * Copyright (c) 2011-2013 Yan Pujante
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
import org.linkedin.glu.orchestration.engine.delta.impl.SystemFiltersDeltaSystemModelFilter
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.orchestration.engine.fabric.FabricService
import org.linkedin.glu.provisioner.core.model.LogicSystemFilterChain
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.core.model.SystemEntryStateSystemFilter
import org.linkedin.glu.provisioner.core.model.SystemFilter
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.plan.api.IStep.Type
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.orchestration.engine.delta.DeltaSystemModelFilter
import org.linkedin.glu.orchestration.engine.delta.impl.BounceDeltaSystemModelFilter
import org.linkedin.glu.orchestration.engine.delta.impl.RedeployDeltaSystemModelFilter
import org.linkedin.glu.groovy.utils.plugins.PluginService

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

  @Initializable(required = true)
  Closure planIdFactory = { Plan<ActionDescriptor> plan ->
    UUID.randomUUID().toString()
  }

  @Initializable
  String autoUpgradeScriptClassname = 'org.linkedin.glu.agent.impl.script.AutoUpgradeScript'

  @Initializable
  PluginService pluginService

  /**
   * Compute deployment plans between the system provided (params.system) and the current
   * system.
   */
  Collection<Plan<ActionDescriptor>> computeDeployPlans(params, def metadata)
  {
    doComputeDeploymentPlans(params, metadata, null)
  }

  /**
   * Compute deployment plans by doing the following:
   * <ol>
   *   <li>compute delta between expected model (params.system) and current model (computed)
   *   <li>compute the deployment plan(s) (closure callback) (use params.stepType if a given type only
   *       is required
   *   <li>set various metadata on the plan(s) as well as the name
   * </ol>
   * @return a collection of plans (<code>null</code> if no expected model) which may be empty
   */
  private Collection<Plan<ActionDescriptor>> doComputeDeploymentPlans(params,
                                                                      def metadata,
                                                                      Collection<String> toStates,
                                                                      DeltaSystemModelFilter filter = null)
  {
    computeDeploymentPlans(params, metadata, null, null, filter, toStates)
  }

  /**
   * Compute deployment plans by doing the following:
   * <ol>
   *   <li>compute delta between expected model (params.system) and current model (computed)
   *   <li>compute the deployment plan(s) (closure callback) (use params.stepType if a given type only
   *       is required
   *   <li>set various metadata on the plan(s) as well as the name
   * </ol>
   * @return a collection of plans (<code>null</code> if no expected model) which may be empty
   */
  @Override
  public Collection<Plan<ActionDescriptor>> computeDeploymentPlans(params,
                                                                   def metadata,
                                                                   def expectedModelFilter,
                                                                   def currentModelFilter,
                                                                   DeltaSystemModelFilter filter,
                                                                   Collection<String> toStates)
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

    if(currentModelFilter)
      currentModel = currentModel.filterBy(currentModelFilter)

    doComputeDeploymentPlans(params, expectedModel, currentModel, filter, metadata, toStates)
  }

  /**
   * Compute deployment plans by doing the following:
   * <ol>
   *   <li>compute delta between expected model and current model (computed)
   *   <li>compute the deployment plan(s) (closure callback) (use params.stepType if a given type only
   *       is required
   *   <li>set various metadata on the plan(s) as well as the name
   * </ol>
   * @return a collection of plans (<code>null</code> if no expected model) which may be empty
   */
  private Collection<Plan<ActionDescriptor>> doComputeDeploymentPlans(params,
                                                                      SystemModel expectedModel,
                                                                      SystemModel currentModel,
                                                                      DeltaSystemModelFilter filter,
                                                                      def metadata,
                                                                      Collection<String> toStates)
  {
    // 1. compute delta
    def delta
    if(toStates)
      delta = deltaMgr.computeDeltas(expectedModel, currentModel, toStates, filter)
    else
      delta = deltaMgr.computeDelta(expectedModel, currentModel, filter)

    // 2. compute the transition plan
    TransitionPlan<ActionDescriptor> transitionPlan = planner.computeTransitionPlan(delta)

    Collection<Type> types = []
    if(params.stepType)
      types << Type.valueOf(params.stepType.toString())
    else
      types = [Type.SEQUENTIAL, Type.PARALLEL]

    if(metadata == null)
      metadata = [:]

    Collection<Plan<ActionDescriptor>> allPlans = types.collect { Type type ->
      // 3. compute the deployment plan the given type
      Plan<ActionDescriptor> plan = transitionPlan.buildPlan(type)

      // 4. set name and metadata for the plan
      plan.setMetadata('fabric', expectedModel.fabric)
      plan.setMetadata('systemId', expectedModel.id)

      // 5. set the plan id
      String planId = planIdFactory(plan)
      if(planId)
        plan.setId(planId)

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
    }

    allPlans.findAll { Plan<ActionDescriptor> plan -> plan.hasLeafSteps() }
  }

  /**
   * Computes a transition plan.
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeTransitionPlans(params, def metadata)
  {
    DeltaSystemModelFilter filter = null

    if(params.expectedEntryStates || params.currentEntryStates)
    {
      def expectedEntrySystemFilter =
        LogicSystemFilterChain.and(
          (SystemFilter) params.system.filters,
          SystemEntryStateSystemFilter.create((params.expectedEntryStates ?: []) as Set)
        )

      params.system = params.system.unfilter()

      def currentEntrySystemFilter =
        SystemEntryStateSystemFilter.create((params.currentEntryStates ?: []) as Set)

      filter = new SystemFiltersDeltaSystemModelFilter(expectedEntrySystemFilter,
                                                       currentEntrySystemFilter)
    }

    doComputeDeploymentPlans(params, metadata, params.states ?: [params.state], filter)
  }

  /**
   * Compute a bounce plan to bounce (= stop/start) containers.
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeBouncePlans(params, def metadata)
  {
    DeltaSystemModelFilter filter = new BounceDeltaSystemModelFilter(params.system.filters)

    params.system = params.system.unfilter()

    computeDeploymentPlans(params,
                           metadata,
                           null,
                           null,
                           filter,
                           ['stopped', 'running'])
  }

  /**
   * Compute an undeploy plan.
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeUndeployPlans(params, def metadata)
  {
    computeDeploymentPlans(params,
                           metadata,
                           null,
                           null,
                           null,
                           [null])
  }

  /**
   * Compute a redeploy plan (= undeploy/deploy).
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeRedeployPlans(params, def metadata)
  {
    RedeployDeltaSystemModelFilter filter =
      new RedeployDeltaSystemModelFilter(params.system.filters)

    params.system = params.system.unfilter()

    computeDeploymentPlans(params,
                           metadata,
                           null,
                           null,
                           filter,
                           [null, '<expected>'])
  }

  /**
   * Generic call which defines which plan to create with the <code>params.planType</code> parameter
   * and allow for a plugin to take over entirely.
   *
   * @params params.planType the type of plan to create (deploy, undeploy, ...)
   */
  @Override
  Collection<Plan<ActionDescriptor>> computePlans(params, def metadata)
  {
    pluginService.executePrePostMethods(PlannerService,
                                        "computePlans",
                                        [
                                          params: params,
                                          metadata: metadata
                                        ]) { args ->
      Collection<Plan<ActionDescriptor>> plans = args.pluginResult

      if(plans == null)
        plans = this."compute${args.params.planType.capitalize()}Plans"(args.params,
                                                                        args.metadata)

      return plans
    } as Collection<Plan<ActionDescriptor>>
  }

  /**
   * Generic call which defines which plan to create with the <code>params.planType</code> parameter
   * and allow for a plugin to take over entirely.
   *
   * @params params.planType the type of plan to create (deploy, undeploy, ...)
   * @params params.stepType the type of steps to create (sequential/parallel)
   */
  @Override
  Plan<ActionDescriptor> computePlan(params, def metadata)
  {
    params.stepType = params.stepType ?: Type.SEQUENTIAL
    return toSinglePlan(computePlans(params, metadata))
  }

  /**
   * Computes the deployment plan for upgrading agents
   * @param metadata any metadata to add to the plan(s)
   */
  @Override
  Plan<ActionDescriptor> computeAgentsUpgradePlan(params, def metadata)
  {
    params.stepType = params.stepType ?: Type.SEQUENTIAL
    SystemModel currentModel = agentsService.getCurrentSystemModel(params.fabric)
    def agents = (params.agents ?: []) as Set
    def filteredCurrentModel = currentModel.filterBy { SystemEntry entry ->
      entry != null && agents.contains(entry.agent)
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
      entry.script = [scriptClassName: autoUpgradeScriptClassname]
      entry.initParameters = [
        newVersion: params.version,
        agentTar: params.coordinates,
      ]
      expectedModel.addEntry(entry)
    }

    expectedModel = expectedModel.filterBy { SystemEntry entry ->
      entry?.mountPoint == PlannerService.AGENT_SELF_UPGRADE_MOUNT_POINT
    }

    toSinglePlan(doComputeDeploymentPlans(params,
                                          expectedModel,
                                          currentModel,
                                          null,
                                          metadata,
                                          ['<expected>', null]))
  }

  private def agentsCleanupUpgradeExpectedModelFilter = { SystemEntry entry ->
      entry?.mountPoint == PlannerService.AGENT_SELF_UPGRADE_MOUNT_POINT
  }

  /**
   * Computes the deployment plan for cleaning any upgrade that failed
   * @param metadata any metadata to add to the plan(s)
   */
  @Override
  Plan<ActionDescriptor> computeAgentsCleanupUpgradePlan(params, def metadata)
  {
    params.stepType = params.stepType ?: Type.SEQUENTIAL
    toSinglePlan(computeDeploymentPlans(params,
                                        metadata,
                                        agentsCleanupUpgradeExpectedModelFilter,
                                        null,
                                        null,
                                        null))
  }

  protected Plan<ActionDescriptor> toSinglePlan(Collection<Plan<ActionDescriptor>> plans)
  {
    if(plans == null || plans.size() == 0)
      return null
    return plans[0]
  }
}

