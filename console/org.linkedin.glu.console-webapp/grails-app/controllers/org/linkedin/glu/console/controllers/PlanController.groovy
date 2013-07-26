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

package org.linkedin.glu.console.controllers

import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.linkedin.glu.provisioner.plan.api.IStepFilter
import org.linkedin.glu.orchestration.engine.deployment.DeploymentService
import org.linkedin.glu.provisioner.plan.api.IPlanExecutionProgressTracker
import org.linkedin.glu.provisioner.plan.api.IStep
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.provisioner.plan.api.IStepExecution
import org.linkedin.glu.provisioner.plan.api.IPlanExecution
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import javax.servlet.http.HttpServletResponse
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.glu.orchestration.engine.deployment.CurrentDeployment
import org.linkedin.glu.orchestration.engine.planner.PlannerService
import org.linkedin.glu.orchestration.engine.action.descriptor.NoOpActionDescriptor
import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.glu.orchestration.engine.deployment.ArchivedDeployment
import org.linkedin.glu.orchestration.engine.deployment.Deployment
import java.security.AccessControlException

/**
 * @author ypujante@linkedin.com */
public class PlanController extends ControllerBase
{
  /**
   * Default plans
   */
  def static DEFAULT_PLANS = [
    [planAction: "deploy", planType: "deploy"],
    [planAction: "bounce", planType: "bounce"],
    [planAction: "bounce", planType: "redeploy"],
    [planAction: "undeploy", planType: "undeploy"],
    [
      planAction: "reconfigure",
      planType: "transition",
      displayName: "Reconfigure",
      states: ["installed", "running"],
      expectedEntryStates: ["running", "stopped", "installed"],
      currentEntryStates: ["running", "stopped"]
    ],
    [
      planAction: "start",
      planType: "transition",
      displayName: "Start",
      state: "running",
      expectedEntryStates: ["running"],
      currentEntryStates: ["stopped"]
    ],
    [
      planAction: "stop",
      planType: "transition",
      displayName: "Stop",
      state: "stopped",
      expectedEntryStates: ["running", "stopped"],
      currentEntryStates: ["running"]
    ],
  ]

  Clock clock = SystemClock.instance()
  ConsoleConfig consoleConfig
  DeploymentService deploymentService
  PlannerService plannerService
  AgentsService agentsService

  def beforeInterceptor = {
    // we make sure that the fabric is always set before executing any action
    return ensureCurrentFabric()
  }

  /**
   * Redirect post into get for plan
   */
  def redirectView = {
    redirect(action: 'view', id: params.planId)
  }

  /**
   * View the plan (expect id)
   */
  def view = {
    def plan = session.plan?.find { it.id == params.id }

    if(plan)
    {
      [plan: plan]
    }
    else
    {
      flash.warning = "Plan ${params.id} not found"
    }
  }

  /**
   * Filter the plan (remove the steps that are not selected)
   */
  def filter = {
    def plan = doFilterPlan(params)

    if(plan)
    {
      session.plan = [plan]

      redirect(action: 'view', id: plan.id)
    }
    else
    {
      flash.warning = "Plan ${params.id} not found"
      redirect(action: 'view')
    }
  }

  /**
   * Execute the plan
   */
  def execute = {
    Plan plan = doFilterPlan(params)

    if(plan)
    {
      session.plan = [plan]

      if(!plan.step)
      {
        flash.warning = "Nothing to execute...."
        redirect(action: 'view', id: plan.id)
        return
      }
      
      try
      {
        CurrentDeployment currentDeployment =
          deploymentService.executeDeploymentPlan(request.system,
                                                  plan,
                                                  plan.name,
                                                  new ProgressTracker())
        session.plan = null
        redirect(action: 'deployments', id: currentDeployment.id)
      }
      catch (AccessControlException e)
      {
        flash.error = e.message
        redirect(action: 'view', id: params.id)
        return
      }
    }
    else
    {
      flash.warning = "Plan ${params.id} not found"
      redirect(action: 'view')
    }
  }

  /**
   * Create a plan
   */
  def create = {

    def args = params
    if(params.json)
    {
      args = JsonUtils.fromJSON(params.json)
    }

    def system

    if(args.systemFilter)
      system = request.system.unfilter().filterBy(args.systemFilter)
    else
      system = request.system
    args.system = system

    if(args.planType)
    {
      Plan<ActionDescriptor> plan = plannerService.computePlan(args, null)
      if(plan?.hasLeafSteps())
      {
        if(system.filters)
          plan.metadata.filter = system.filters.toString()
        session.plan = plan
        render(template: 'plan', model: [plan: plan])
      }
      else
        render "no plan"
    }
    else
      render "choose a plan"
  }

  /**
   * View deployments
   */
  def deployments = {
    if(params.id)
    {
      CurrentDeployment deployment = deploymentService.getDeployment(params.id)
      if(deployment)
        render(view: 'deploymentDetails', model: [deployment: deployment])
      else
      {
        redirect(action: 'archived', params: [id: params.id])
      }
    }
    else
    {
      def deployments = sortAndGroupDeployments(deploymentService.getDeployments(request.fabric.name))

      [groupBy: deployments]
    }
  }

  private def sortAndGroupDeployments(deployments)
  {
    deployments?.sort() { d1, d2 ->
        d2.planExecution.startTime.compareTo(d1.planExecution.startTime)
    }?.groupBy { it.planExecution.isCompleted() ? 'Completed' : 'Active'}
  }

  /**
   * Renders the deployment details (just the inner part)
   */
  def renderDeploymentDetails = {
    if(params.id)
    {
      render(template: 'deploymentDetails', model: [deployment: deploymentService.getDeployment(params.id)])
    }
  }

  /**
   * Renders only the completed deployments */
  def renderDeployments = {
    def deployments = sortAndGroupDeployments(deploymentService.getDeployments(request.fabric.name))
    render(template: 'deployments', model: [groupBy: deployments])
  }

  /**
   * View archived deployments
   */
  def archived = {
    if(params.id)
    {
      [deployment: deploymentService.getArchivedDeployment(params.id)]
    }
    else
    {
      deploymentService.getArchivedDeployments(request.fabric.name, false, params)
    }
  }

  /**
   * Remove the deployement
   */
  def archiveDeployment = {
    deploymentService.archiveDeployment(params.id)
    redirect(action: 'deployments')
  }

  /**
   * Archives all deployments */
  def archiveAllDeployments = {
    def count = deploymentService.archiveAllDeployments(request.fabric.name)
    flash.success = "Successfully archived ${count} deployment(s)."
    redirect(action: 'deployments')
  }

  /**
   * Resume the deployment
   */
  def resumeDeployment = {
    audit('plan.resume', params.id)
    deploymentService.getDeployment(params.id)?.planExecution?.resume()
    redirect(action: 'deployments', id: params.id)
  }

  /**
   * Pause the deployment
   */
  def pauseDeployment = {
    audit('plan.pause', params.id)
    deploymentService.getDeployment(params.id)?.planExecution?.pause()
    redirect(action: 'deployments', id: params.id)
  }

  /**
   * Aborts the deployment
   */
  def abortDeployment = {
    audit('plan.abort', params.id)
    deploymentService.getDeployment(params.id)?.planExecution?.cancel(true)
    redirect(action: 'deployments', id: params.id)
  }


  /**
   * Cancels a single step
   */
  def cancelStep = {
    def stepExecutor = deploymentService.getDeployment(params.id)?.progressTracker?.steps?.getAt(params.stepId)
    if(stepExecutor)
    {
      audit('plan.cancelStep', "plan: ${params.id}, step: ${stepExecutor.step.name}")
      stepExecutor.cancel(true)
    }
    redirect(action: 'deployments', id: params.id)
  }

  /**
   * Filter the plans by the steps selected.
   */
  private Plan doFilterPlan(params)
  {
    Plan plan = session.plan.find { it.id == params.id }

    if(plan)
    {
      def stepIds = new HashSet()
      params.stepId?.each {
        stepIds << it
      }

      def filter = { step ->
        stepIds.contains(step.id)
      }

      def planBuilder = plan.toPlanBuilder(filter as IStepFilter)

      plan = planBuilder.toPlan()

      return plan
    }
    else
    {
      return null
    }
  }

  /**
   * Returns the list of all plans (that have been saved)
   */
  def rest_list_plans = {
    Collection<Plan<ActionDescriptor>> plans = deploymentService.getPlans(request.fabric.name)
    if(plans)
    {
      response.setContentType('text/json')
      def map = [:]
      plans.each { plan ->
        map[plan.id] = g.createLink(absolute: true,
                                    mapping: 'restPlan',
                                    id: plan.id, params: [fabric: request.fabric]).toString()
      }
      response.addHeader("X-glu-count", map.size().toString())
      render prettyPrintJsonWhenRequested(map)
    }
    else
    {
      response.setStatus(HttpServletResponse.SC_NO_CONTENT,
                         'no plan current deployments')
      render ''
    }
  }

  /**
   * Create a plan
   */
  def rest_create_plan = {

    def args = [:]
    args.system = request.system
    args.fabric = request.fabric

    try
    {
      args.stepType = IStep.Type.valueOf((params.order ?: 'sequential').toUpperCase())
    }
    catch (IllegalArgumentException e)
    {
      render e.message
      response.sendError(HttpServletResponse.SC_BAD_REQUEST)
      return
    }

    if(params.planAction)
    {
      args = processPlanAction(params, args)

      if(!args)
      {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "invalid action: ${params.planAction}")
        render ''
        return
      }
    }
    else
    {
      if(params.planType)
      {
        args.putAll(params)
      }
      else
      {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST,
                           "missing planType or planAction!")
        render ''
        return
      }
    }

    def metadata =
      GluGroovyCollectionUtils.xorMap([origin: 'rest', *:args, filter: params.systemFilter ?: 'all'],
                                      ['action', 'controller', 'system', 'stepType'])

    Plan plan = plannerService.computePlan(args, metadata)

    if(plan?.hasLeafSteps())
    {
      if(plan.leafSteps.findAll { it.action instanceof NoOpActionDescriptor }.size() == plan.leafStepsCount)
      {
        response.sendError(HttpServletResponse.SC_NO_CONTENT,
                           'no plan created (only pending transitions)')
        render ''
      }
      else
      {
        deploymentService.savePlan(plan)
        response.addHeader('Location', g.createLink(absolute: true,
                                                    mapping: 'restPlan',
                                                    id: plan.id, params: [fabric: request.fabric]).toString())
        response.setStatus(HttpServletResponse.SC_CREATED)
        render plan.id
      }
    }
    else
    {
      response.setStatus(HttpServletResponse.SC_NO_CONTENT, 'no plan created (no delta)')
      render ''
    }
  }

  private def processPlanAction(def params, def args)
  {
    def res = processPlanAction(params, args, consoleConfig.defaults.plans)
    if(!res)
      res = processPlanAction(params, args, DEFAULT_PLANS)
    return res
  }

  private def processPlanAction(def params, def args, def plansDefinition)
  {
    if(plansDefinition)
    {
      def planDefinition =
        plansDefinition.find { planDefinition -> planDefinition.planAction == params.planAction }

      if(planDefinition)
      {
        args.planType = planDefinition.planType

        ['state', 'states', 'expectedEntryStates', 'currentEntryStates'].each { p ->
          args[p] = params[p] ?: planDefinition[p]
        }

        return args
      }
    }

    return null
  }

  /**
   * View a plan
   */
  def rest_view_plan = {
    def plan = deploymentService.getPlan(params.id)
    if(plan)
    {
      response.setContentType('text/xml')
      render plan.toXml()
    }
    else
    {
      response.sendError HttpServletResponse.SC_NOT_FOUND
    }
  }

  /**
   *  Execute a plan
   */
  def rest_execute_plan = {
    def plan = deploymentService.getPlan(params.id)
    if(plan)
    {
      CurrentDeployment currentDeployment =
        deploymentService.executeDeploymentPlan(request.system,
                                                plan,
                                                plan.name,
                                                new ProgressTracker())

      response.addHeader('Location', g.createLink(absolute: true,
                                                  mapping: 'restExecution',
                                                  id: currentDeployment.id, params: [
                                                  planId: plan.id,
                                                  fabric: request.fabric.name]).toString())
      response.setStatus(HttpServletResponse.SC_CREATED)
      render currentDeployment.id
    }
    else
    {
      response.sendError HttpServletResponse.SC_NOT_FOUND
    }
  }

  /**
   * Get plan execution progression status (percentage)
   */
  def rest_execution_status = {
    def deployment = deploymentService.getDeployment(params.id)

    if(deployment && deployment.planExecution.plan.id == params.planId)
    {
      def completionStatus

      def completion = deployment.progressTracker.completionPercentage
      if(deployment.planExecution.isCompleted())
      {
        completion = "100:${deployment.planExecution.completionStatus.status}"
      }
      else
        completion = "${completion}"
      response.setHeader("X-LinkedIn-GLU-Completion", completion)
      response.setHeader("X-glu-completion", completion)
      render ''
    }
    else
    {
      response.sendError HttpServletResponse.SC_NOT_FOUND
    }
  }

  /**
   * Get a full plan execution (xml format)
   */
  def rest_view_execution = {
    def deployment = deploymentService.getDeployment(params.id)

    if(deployment && deployment.planExecution.plan.id == params.planId)
    {
      response.setContentType('text/xml')
      render deployment.planExecution.toXml()
    }
    else
    {
      response.sendError HttpServletResponse.SC_NOT_FOUND
    }
  }

  /**
   * List all the executions for a given plan (GET /plan/<planId>/executions)
   */
  def rest_list_executions = {
    Collection<CurrentDeployment> list =
      deploymentService.getDeployments(request.fabric.name, params.id)
    if(list)
    {
      response.setContentType('text/json')
      def map = [:]
      list.each { CurrentDeployment deployment ->
        map[deployment.id] = g.createLink(absolute: true,
                                          mapping: 'restExecution',
                                          id: deployment.id, params: [
                                          planId: params.id,
                                          fabric: request.fabric.name]).toString()
      }
      response.addHeader("X-glu-count", map.size().toString())
      render prettyPrintJsonWhenRequested(map)
    }
    else
    {
      response.sendError(HttpServletResponse.SC_NO_CONTENT,
                         'no execution for this plan')
      render ''
    }
  }

  /**
   * List all current deployments (GET /deployments/current)
   */
  def rest_list_current_deployments = {
    Collection<CurrentDeployment> list = deploymentService.getDeployments(request.fabric.name)
    if(list)
    {
      response.setContentType('text/json')
      def map = [:]
      list.each { CurrentDeployment deployment ->
        def deploymentMap = buildMap(deployment)
        deploymentMap.viewURL = g.createLink(absolute: true,
                                             mapping: 'restViewCurrentDeployment',
                                             id: deployment.id,
                                             params: [fabric: request.fabric.name]).toString()
        map[deployment.id] = deploymentMap
      }
      response.addHeader("X-glu-count", map.size().toString())
      render prettyPrintJsonWhenRequested(map)
    }
    else
    {
      response.sendError(HttpServletResponse.SC_NO_CONTENT,
                         'no current deployments')
      render ''
    }
  }

  /**
   * Handle GET and HEAD /deployments/current/<deploymentId>
   */
  def rest_view_current_deployment = {
    processViewDeployment(deploymentService.getDeployment(params.id))
  }

  /**
   * Archive current deployment DELETE /deployments/current/<deploymentId>
   */
  def rest_archive_current_deployment = {
    try
    {
      boolean archived = deploymentService.archiveDeployment(params.id)
      response.addHeader("X-glu-archived", archived.toString())
      response.setStatus(HttpServletResponse.SC_OK)
      render ''
    }
    catch (IllegalStateException e)
    {
      // currently running... cannot archive!
      response.sendError HttpServletResponse.SC_CONFLICT
    }
  }

  /**
   * Archive all deployments DELETE /deployments/current
   */
  def rest_archive_all_deployments = {
    def count = deploymentService.archiveAllDeployments(request.fabric.name)
    response.addHeader("X-glu-archived", count.toString())
    response.setStatus(HttpServletResponse.SC_OK)
    render ''
  }

  /**
   * count how many archived deployments (HEAD /deployments/archived) */
  def rest_count_archived_deployments = {
    response.addHeader("X-glu-totalCount",
                       deploymentService.getArchivedDeploymentsCount(request.fabric.name).toString())
    response.setStatus(HttpServletResponse.SC_OK)
    render ''
  }

  /**
   * List archived deployments (GET /deployments/archived) */
  def rest_list_archived_deployments = {
    def map = deploymentService.getArchivedDeployments(request.fabric.name, false, params)

    if(map.deployments)
    {
      Map deploymentsMap = [:]

      map.deployments.each { ArchivedDeployment deployment ->
        def deploymentMap = buildMap(deployment)
        deploymentMap.viewURL = g.createLink(absolute: true,
                                             mapping: 'restViewArchivedDeployment',
                                             id: deployment.id,
                                             params: [fabric: request.fabric.name]).toString()
        deploymentsMap[deployment.id] = deploymentMap
      }

      response.addHeader("X-glu-count", map.deployments.size().toString())
      response.addHeader("X-glu-totalCount", map.count.toString())
      ['max', 'offset', 'sort', 'order'].each { k ->
        response.addHeader("X-glu-${k}", params[k].toString())
      }

      response.setContentType('text/json')
      render prettyPrintJsonWhenRequested(deploymentsMap)
    }
    else
    {
      response.sendError(HttpServletResponse.SC_NO_CONTENT,
                         'no current deployments')
      render ''
    }
  }

  /**
   * Handle GET and HEAD /deployments/archived/<deploymentId>
   */
  def rest_view_archived_deployment = {
    processViewDeployment(deploymentService.getArchivedDeployment(params.id))
  }

  /**
   * Handle GET and HEAD for a deployment
   */
  private void processViewDeployment(Deployment deployment)
  {
    if(deployment)
    {
      def map = buildMap(deployment)
      // put all map values as headers
      map.each { k, v ->
        if(v)
          response.addHeader("X-glu-${k}", v.toString())
      }

      if(request.method == "HEAD")
      {
        render ''
      }
      else
      {
        response.setContentType('text/xml')
        render deployment.planXml
      }
    }
    else
    {
      response.sendError HttpServletResponse.SC_NOT_FOUND
    }
  }

  private Map buildMap(CurrentDeployment deployment)
  {
    [
      startTime: deployment.planExecution.startTime,
      endTime: deployment.planExecution.completionStatus?.endTime,
      username: deployment.username,
      status: deployment.planExecution.completionStatus?.status ?: 'RUNNING',
      description: deployment.description,
      completedSteps: deployment.progressTracker.leafStepsCompletedCount,
      totalSteps: deployment.planExecution.plan.leafStepsCount,
    ]
  }

  private Map buildMap(ArchivedDeployment deployment)
  {
    [
      startTime: deployment.startDate.time,
      endTime: deployment.endDate?.time,
      username: deployment.username,
      status: deployment.status,
      description: deployment.description
    ]
  }
}

/**
 * Keep track of the progress to be able to display it in the UI.
 */
class ProgressTracker implements IPlanExecutionProgressTracker
{
  Clock clock = SystemClock.instance()

  Plan plan
  long planStartTime
  long planEndTime

  def _steps = [:]

  int _leafStepsCompletedCount = 0

  public void onPlanStart(IPlanExecution planExecution)
  {
    this.plan = planExecution.plan
    planStartTime = clock.currentTimeMillis()
  }

  public void onPlanEnd(IStepCompletionStatus stepExecutionStatus)
  {
    planEndTime = clock.currentTimeMillis()
  }

  public void onPause(IStep step)
  {

  }

  public void onResume(IStep step)
  {

  }

  public void onCancelled(IStep step)
  {

  }

  synchronized int getLeafStepsCompletedCount()
  {
    return _leafStepsCompletedCount
  }

  synchronized int getCompletionPercentage()
  {
    if(plan.leafStepsCount == 0)
      return 100;

    return (((double) _leafStepsCompletedCount / (double) plan.leafStepsCount) * 100.0) as int
  }

  def synchronized getSteps()
  {
    def res = [:]
    _steps.values().each { res[it.step.id] = it }
    return res
  }

  public synchronized void onStepStart(IStepExecution stepExecution)
  {
    _steps[stepExecution.step.id] = stepExecution
  }

  public synchronized void onStepEnd(IStepCompletionStatus stepExecutionStatus)
  {
    if(stepExecutionStatus.step.type == IStep.Type.LEAF)
      _leafStepsCompletedCount++
  }
}