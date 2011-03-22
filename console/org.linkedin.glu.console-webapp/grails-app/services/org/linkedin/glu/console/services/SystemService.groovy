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

package org.linkedin.glu.console.services

import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.core.environment.Environment
import org.linkedin.glu.provisioner.deployment.api.IDeploymentManager
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.provisioner.plan.api.LeafStep
import org.linkedin.glu.provisioner.plan.api.IStep
import org.linkedin.glu.provisioner.plan.api.IPlanExecutionProgressTracker
import org.linkedin.glu.console.domain.AuditLog
import org.apache.shiro.SecurityUtils
import org.linkedin.glu.console.domain.DbDeployment
import org.linkedin.glu.provisioner.plan.api.FilteredPlanExecutionProgressTracker
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus
import org.linkedin.glu.provisioner.plan.api.IPlanExecution
import org.linkedin.glu.provisioner.plan.api.SequentialStep
import org.linkedin.glu.console.domain.DbSystemModel
import org.linkedin.glu.console.domain.DbCurrentSystem
import org.linkedin.glu.provisioner.services.fabric.Fabric
import org.linkedin.util.clock.Timespan
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.impl.agent.DefaultDescriptionProvider
import org.linkedin.glu.provisioner.services.fabric.FabricService

/**
 * System service.
 *
 * @author ypujante@linkedin.com */
class SystemService
{
  // we keep entries in the plan cache no more than 5m
  Timespan planCacheTimeout = Timespan.parse('5m')
  Clock clock = SystemClock.INSTANCE

  AgentsService agentsService
  FabricService fabricService
  IDeploymentManager deploymentMgr

  private Map _deployments = [:]
  private Map<String, Plan> _plans = [:]

  /**
   * Given a fabric, returns the list of agents that are declared in the system
   * but are not available through ZooKeeper
   */
  def getMissingAgents(Fabric fabric)
  {
    getMissingAgents(fabric, DbSystemModel.findCurrent(fabric)?.systemModel)
  }

  /**
   * Given a system, returns the list of agents that are declared
   * but are not available through ZooKeeper
   */
  def getMissingAgents(Fabric fabric, SystemModel system)
  {
    def availableAgents = agentsService.getAgentInfos(fabric)

    def missingAgents = [] as Set

    system?.each { SystemEntry se ->
      if(!availableAgents.containsKey(se.agent))
        missingAgents << se.agent
    }

    return missingAgents
  }

  /**
   * Saves the new model as the current system.
   * @return the new current system
   */
  DbCurrentSystem saveCurrentSystem(SystemModel newSystemModel)
  {
    if(newSystemModel.filters)
      throw new IllegalStateException("cannot save a filtered model!")

    def newSystemModelSha1 = newSystemModel.computeContentSha1()

    if(!newSystemModel.id)
      newSystemModel.id = newSystemModelSha1

    def currentSystemModel = DbCurrentSystem.findByFabric(newSystemModel.fabric)

    // when the new system and the current system match, there is no reason to
    // change it
    if(newSystemModelSha1 == currentSystemModel?.systemModel?.systemModel?.computeContentSha1())
    {
      return currentSystemModel
    }

    def previousSystemModel = DbSystemModel.findBySystemId(newSystemModel.id)

    DbSystemModel newDbSystemModel

    if(previousSystemModel)
    {
      if(newSystemModelSha1 != previousSystemModel?.systemModel?.computeContentSha1())
      {
        throw new IllegalArgumentException("same id ${newSystemModel.id} but different content!")
      }
      newDbSystemModel = previousSystemModel
    }
    else
    {
      newDbSystemModel = new DbSystemModel(systemModel: newSystemModel)
      if(!newDbSystemModel.save())
      {
        throw new IllegalArgumentException("error while saving new system ${newDbSystemModel.errors}")
      }
    }

    if(currentSystemModel)
      currentSystemModel.systemModel = newDbSystemModel
    else
      currentSystemModel = new DbCurrentSystem(systemModel: newDbSystemModel,
                                               fabric: newDbSystemModel.fabric)

    currentSystemModel = currentSystemModel.save()

    AuditLog.audit('system.change',
                   "fabric: ${newSystemModel.fabric}, systemId: ${newSystemModel.id}")

    return currentSystemModel
  }

  def getHostsWithDeltas(params)
  {
    def hosts = new HashSet()

    def plan = computeDeploymentPlan(params)

    plan?.leafSteps?.each { LeafStep step ->
      if(step.action instanceof ActionDescriptor)
      {
        ActionDescriptor ad = (ActionDescriptor) step.action
        hosts << ad.descriptorProperties.hostname
      }
    }

    return hosts
  }

  Plan computeDeploymentPlan(params)
  {
    computeDeploymentPlan(params) {
      return true
    }
  }

  Plan computeDeploymentPlan(params, Closure closure)
  {
    SystemModel system = params.system

    if(!system)
      return null

    def expectedEnvironment = agentsService.computeEnvironment(params.fabric, system)
    def currentSystem = agentsService.getCurrentSystemModel(params.fabric)
    (system, currentSystem) = SystemModel.filter(system, currentSystem)
    def currentEnvironment = agentsService.computeEnvironment(params.fabric, currentSystem)

    if(expectedEnvironment && currentEnvironment)
    {
      def plan = createPlan(params.name,
                            currentEnvironment,
                            expectedEnvironment,
                            closure)
      plan.setMetadata('fabric', system.fabric)
      plan.setMetadata('systemId', system.id)
      return plan
    }
    else
    {
      return null
    }
  }

  /**
   * @return the current environment (live from zookeeper)
   */
  private def getCurrentEnvironment(Fabric fabric)
  {
    agentsService.getCurrentEnvironment(fabric)
  }

  /**
   * @return the current environment (filtered by the provided system if provided)
   */
  private def getCurrentEnvironment(params)
  {
    def currentSystem = agentsService.getCurrentSystemModel(params.fabric)
    currentSystem = currentSystem.filterBy(params.system)
    agentsService.computeEnvironment(params.fabric, currentSystem)
  }

  /**
   * Computes a transition plan. The closure is meant to
   * filter out the installations (<code>Installation</code>) and should return
   * <code>true</code> for all installation  that need to be part of the plan.
   */
  Plan computeTransitionPlan(params, Closure filter)
  {
    def env = getCurrentEnvironment(params)

    agentsService.createTransitionPlan(installations: env.installations, state: params.state, filter)
  }

  /**
   * Compute a bounce plan to bounce (= stop/start) containers. The closure is meant to
   * filter out the installations (<code>Installation</code>) and should return
   * <code>true</code> for all installation  that need to be part of the plan.
   */
  Plan computeBouncePlan(params, Closure filter)
  {
    params.state = ['stopped', 'running']
    computeTransitionPlan(params, filter)
  }

  /**
   * Compute an undeploy plan. The closure is meant to
   * filter out the installations (<code>Installation</code>) and should return
   * <code>true</code> for all installation  that need to be part of the plan.
   */
  Plan computeUndeployPlan(params, Closure filter)
  {
    computeUndeployPlan(getCurrentEnvironment(params), filter)
  }

  /**
   * Compute an undeploy plan. The closure is meant to
   * filter out the installations (<code>Installation</code>) and should return
   * <code>true</code> for all installation  that need to be part of the plan.
   */
  private Plan computeUndeployPlan(Environment environment, Closure filter)
  {
    // we create an undeploy plan
    def undeployPlan = agentsService.createTransitionPlan(installations: environment.installations,
                                                          state: ['undeployed'],
                                                          filter)

    return undeployPlan
  }

  /**
   * Compute a deploy plan. The closure is meant to
   * filter out the installations (<code>Installation</code>) and should return
   * <code>true</code> for all installation  that need to be part of the plan.
   */
  private Plan computeDeployPlan(Environment environment, params, Closure filter)
  {
    SystemModel system = params.system

    if(!system)
      return null

    def expectedEnvironment = agentsService.computeEnvironment(params.fabric, system)
    def currentSystem = new SystemModel(fabric: system.fabric) // empty
    def currentEnvironment = agentsService.computeEnvironment(params.fabric, currentSystem)

    // all the installations in transition need to be removed from the current environment
    def installations = environment.installations.findAll { it.transitionState == null }.id as Set

    expectedEnvironment = expectedEnvironment.filterBy(expectedEnvironment.name) {
      installations.contains(it.id)
    }

    if(expectedEnvironment && currentEnvironment)
    {
      def plan = createPlan(params.name,
                            currentEnvironment,
                            expectedEnvironment,
                            filter)
      return plan
    }
    else
    {
      return null
    }
  }

  /**
   * Compute a redeploy plan (= undeploy/deploy). The closure is meant to
   * filter out the installations (<code>Installation</code>) and should return
   * <code>true</code> for all installation  that need to be part of the plan.
   */
  Plan computeRedeployPlan(params, Closure filter)
  {
    def env = getCurrentEnvironment(params)

    def undeployPlan = computeUndeployPlan(env, filter)
    def deployPlan = computeDeployPlan(env, params, filter)
    def steps = []
    if(undeployPlan?.step)
      steps << undeployPlan?.step
    if(deployPlan?.step)
      steps << deployPlan?.step
    if(steps)
    {
      Map metadata = [:]
      steps.each { metadata.putAll(it.metadata) }
      new Plan(new SequentialStep("Redeploy", metadata, steps))
    }
    else
      return null
  }

  public Plan createPlan(String name,
                         Environment currentEnvironment,
                         Environment expectedEnvironment,
                         Closure closure)
  {
    deploymentMgr.createPlan(name, 
                             currentEnvironment,
                             expectedEnvironment,
                             DefaultDescriptionProvider.INSTANCE,
                             closure)
  }

  /**
   * Shortcut to group the plan by hostname first, then mountpoint in both sequential and parallel
   * types.
   */
  Map<IStep.Type, Plan> groupByHostnameAndMountPoint(Plan plan)
  {
    def Map<IStep.Type, Plan> plans = [:]

    [IStep.Type.SEQUENTIAL, IStep.Type.PARALLEL].each { IStep.Type stepType ->
      def newPlan = groupByHostnameAndMountPoint(plan, stepType)
      if(newPlan)
        plans[stepType] = newPlan
    }

    return plans
  }

  /**
   * Create a new plan of the given type where the entries are grouped by hostname first, then
   * mountpoint and call the closure to filter all leaves.
   */
  Plan groupByHostnameAndMountPoint(Plan plan, IStep.Type type)
  {
    def leafSteps = plan?.leafSteps

    if(leafSteps)
    {
      // first we group the steps by hostname
      def hosts = leafSteps.groupBy { LeafStep<ActionDescriptor> step ->
        return step.action.descriptorProperties.hostname
      }

      // then we group them by mountPoint
      hosts.keySet().each { hostname ->
        hosts[hostname] = hosts[hostname].groupBy { LeafStep<ActionDescriptor> step ->
          step.action.descriptorProperties.mountPoint
        }
      }

      def planBuilder = plan.createEmptyPlanBuilder()
      def compositeStepsBuilder = planBuilder.addCompositeSteps(type)
      planBuilder.name = "${plan.name} - ${type}".toString()

      hosts.keySet().sort().each { hostname ->
        def stepsByMountPoint = hosts[hostname]
        def hostStepsBuilder = compositeStepsBuilder.addCompositeSteps(type)
        hostStepsBuilder.name = hostname

        // we split the plan by mountPoint
        stepsByMountPoint.keySet().sort().each { mountPoint ->
          def steps = stepsByMountPoint[mountPoint]

          // for a given mount point it has to be sequential!
          def mountPointStepsBuilder = hostStepsBuilder.addSequentialSteps()
          mountPointStepsBuilder.name = mountPoint

          steps?.each { step ->
            mountPointStepsBuilder.each { it.addLeafStep(step) }
          }
        }
      }

      plan = planBuilder.toPlan()
    }
    else
    {
      return null
    }
  }

  /**
   * Shortcut to group the plan by instance in both sequential and parallel types.
   */
  Collection<Plan> groupByInstance(Plan plan, def metadata)
  {
    def plans = []

    [IStep.Type.SEQUENTIAL, IStep.Type.PARALLEL].each { IStep.Type stepType ->
      def newPlan = groupByInstance(plan, stepType, metadata)
      if(newPlan)
        plans << newPlan
    }

    return plans
  }

  /**
   * Create a new plan of the given type where the entries going to be grouped by instance.
   */
  Plan groupByInstance(Plan plan, IStep.Type type, def metadata)
  {
    def leafSteps = plan?.leafSteps

    if(leafSteps)
    {
      if(metadata == null)
        metadata = [:]
      def planBuilder = plan.createEmptyPlanBuilder()
      planBuilder.metadata = plan.metadata
      def compositeStepsBuilder = planBuilder.addCompositeSteps(type)
      compositeStepsBuilder.metadata = metadata

      def metadatas = [:]

      def instances = leafSteps.groupBy { LeafStep<ActionDescriptor> step ->
        def key = "${step.action.descriptorProperties.agentName}:${step.action.descriptorProperties.mountPoint}".toString()
        metadatas[key] = [
            agent: step.action.descriptorProperties.agentName,
            mountPoint: step.action.descriptorProperties.mountPoint
        ]
        return key
      }

      // we sort them by instance
      def sortedInstances = new TreeMap(instances)

      sortedInstances.each { key, leaves ->
        def instanceStepsBuilder = compositeStepsBuilder.addSequentialSteps()
        instanceStepsBuilder.metadata = metadatas[key]
        leaves.each { instanceStepsBuilder.addLeafStep (it) }
      }

      def name = plan.name
      if(!name)
      {
        name = metadata.collect { k,v -> "${k}=$v"}.join(' - ')
      }
      name = "${name} - ${type}".toString()
      planBuilder.name = name

      return planBuilder.toPlan()
    }
    else
    {
      return null
    }
  }

  def getPlan(id)
  {
    synchronized(_plans)
    {
      return _plans[id]
    }
  }

  def savePlan(Plan plan)
  {
    synchronized(_plans)
    {
      // first we remove all old entries
      def cutoffTime = planCacheTimeout.pastTimeMillis(clock)
      _plans = _plans.findAll { k,v -> v.metadata.savedTime > cutoffTime }

      plan.metadata.savedTime = clock.currentTimeMillis()
      _plans[plan.id] = plan
    }
  }

  def getDeployments(String fabric)
  {
    synchronized(_deployments)
    {
      return _deployments.values().findAll { it.fabric == fabric }
    }
  }

  /**
   * Returns all the deployments matching the closure
   */
  def getDeployments(String fabric, Closure closure)
  {
    synchronized(_deployments)
    {
      _deployments.values().findAll { it.fabric == fabric && closure(it) }
    }
  }


  def archiveDeployment(id)
  {
    synchronized(_deployments)
    {
      def deployment = _deployments[id]
      if(deployment)
      {
        if(deployment.planExecution.isCompleted())
        {
          _deployments.remove(id)
          AuditLog.audit('plan.archive', "plan: ${id}")
        }
        else
        {
          throw new IllegalStateException("cannot archive a running deployment")
        }
      }
    }
  }

  /**
   * Archive all deployments (that are completed of course)
   * @return the number of archived deployments
   */
  def archiveAllDeployments(String fabric)
  {
    synchronized(_deployments)
    {
      def deploymentsToArchive = getDeployments(fabric) {
        it.planExecution.isCompleted()
      }

      deploymentsToArchive.each { _deployments.remove(it.id) }

      return deploymentsToArchive.size()
    }
  }
  
  def getDeployment(id)
  {
    synchronized(_deployments)
    {
      _deployments[id]
    }
  }

  DbDeployment getArchivedDeployment(id)
  {
    DbDeployment.get(id)
  }

  boolean isExecutingDeploymentPlan(String fabric)
  {
    synchronized(_deployments)
    {
      return _deployments.values().find { it.fabric == fabric && !it.planExecution.isCompleted() }
    }
  }

  def executeDeploymentPlan(SystemModel system, Plan plan)
  {
    executeDeploymentPlan(system, plan, plan.name, null)
  }

  def executeDeploymentPlan(SystemModel system,
                            Plan plan,
                            String description,
                            IPlanExecutionProgressTracker progressTracker)
  {
    synchronized(_deployments)
    {
      String username = SecurityUtils.getSubject()?.principal?.toString()

      def deployment = new DbDeployment(description: description,
                                        fabric: system.fabric,
                                        username: username,
                                        details: plan.toXml())

      if(!deployment.save())
        throw new Exception("cannot save deployment ${plan.name}: ${deployment.errors}")

      def id = deployment.id.toString()

      def tracker = new ProgressTracker(progressTracker, id, system)

      def planExecution = deploymentMgr.executePlan(plan, tracker)

      _deployments[id] = [
        id: id,
        username: username,
        fabric: system.fabric,
        systemId: system.id,
        planExecution: planExecution,
        description: description,
        progressTracker: progressTracker
      ]

      AuditLog.audit('plan.execute', "plan: ${id}, desc: ${description}, systemId: ${system.id}")

      return id
    }
  }
}

class ProgressTracker<T> extends FilteredPlanExecutionProgressTracker<T>
{
  public static final String MODULE = ProgressTracker.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  def final _deploymentId
  private IPlanExecution _planExecution
  private final SystemModel _system

  def ProgressTracker(tracker, deploymentId, SystemModel system)
  {
    super(tracker)
    _deploymentId = deploymentId
    _system = system
  }

  public void onPlanStart(IPlanExecution<T> planExecution)
  {
    super.onPlanStart(planExecution)
    _planExecution = planExecution
  }


  public void onPlanEnd(IStepCompletionStatus<T> status)
  {
    super.onPlanEnd(status)

    DbDeployment.withTransaction { txStatus ->
      DbDeployment deployment = DbDeployment.get(_deploymentId)
      if(!deployment)
      {
        log.warn("could not find deployment ${_deploymentId}")
      }
      else
      {
        deployment.startDate = new Date(status.startTime)
        deployment.endDate = new Date(status.endTime)
        deployment.status = status.status.name()
        deployment.details = _planExecution.toXml([fabric: _system.fabric, systemId: _system.id])

        if(!deployment.save())
        {
          log.warn("could not save deployment ${_deploymentId}: ${deployment.errors}")
        }
      }
    }
  }
}
