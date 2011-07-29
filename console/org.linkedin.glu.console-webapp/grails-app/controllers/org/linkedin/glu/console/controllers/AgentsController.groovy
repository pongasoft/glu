/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.provisioner.plan.api.IStep
import org.linkedin.glu.agent.tracker.MountPointInfo
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import java.security.AccessControlException
import org.linkedin.glu.orchestration.engine.agents.NoSuchAgentException
import org.linkedin.glu.provisioner.plan.api.IStep.Type
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.planner.PlannerService
import org.linkedin.glu.orchestration.engine.delta.DeltaService
import javax.servlet.http.HttpServletResponse
import org.linkedin.glu.agent.tracker.AgentInfo
import org.linkedin.glu.orchestration.engine.action.descriptor.NoOpActionDescriptor
import org.linkedin.glu.orchestration.engine.deployment.DeploymentService

/**
 * @author ypujante@linkedin.com
 */
class AgentsController extends ControllerBase
{
  AgentsService agentsService
  DeploymentService deploymentService
  PlannerService plannerService
  DeltaService deltaService

  def beforeInterceptor = {
    // we make sure that the fabric is always set before executing any action
    return ensureCurrentFabric()
  }

  /**
   * List all the agents (in the current fabric) with their version (in preparation to upgrade)
   */
  def listVersions = {
    def agents = agentsService.getAgentInfos(request.fabric)

    def versions = agents.values().groupBy { it.version }

    return [versions: versions]
  }

  /**
   * Upgrade
   */
  def upgrade = {
    if(!params.version || !params.coordinates)
    {
      flash.error = "Missing version or coordinates"
      redirect(action: 'listVersions')
      return
    }

    if(params.agents instanceof String)
    {
      params.agents = [params.agents]
    }

    params.fabric = request.fabric
    params.type = Type.PARALLEL

    def plan =
      plannerService.computeAgentsUpgradePlan(params,
                                              [name: "Agent upgrade to version ${params.version}".toString()])

    if(plan)
    {
      session.plan = [plan]
      redirect(controller: 'plan', action: 'view', id: plan.id)
    }
    else
    {
      flash.message = "No agent to upgrade"
      redirect(action: 'listVersions')
    }
  }

  /**
   * cleanup
   */
  def cleanup = {
    params.name = "Agent upgrade cleanup"
    params.system = request.system
    params.type = Type.PARALLEL

    def plan = plannerService.computeAgentsCleanupUpgradePlan(params, null)

    if(plan)
    {
      session.plan = [plan]
      redirect(controller: 'plan', action: 'view', id: plan.id)
    }
    else
    {
      flash.message = "No agent to cleanup"
      redirect(action: 'listVersions')
    }
  }

  /**
   * View a single agent in an fabric
   */
  def view = {
    def agent = agentsService.getAgentInfo(request.fabric, params.id)

    def model = [:]
    def allPlans = [:]

    def title = "agent [${params.id}]".toString()
    def filter = "agent='${params.id}'".toString()

    def system = request.system.filterBy(filter)

    if(agent)
    {
      def mountPoints = [] as Set
      system.each { mountPoints << it.mountPoint }

      SystemModel currentModel = agentsService.getCurrentSystemModel(request.fabric)
      currentModel.each { mountPoints << it.mountPoint }

      model = computeAgentModel(request.fabric,
                                agent,
                                mountPoints,
                                allPlans.deploy && allPlans.deploy[0]?.hasLeafSteps())
    }

    return [
      model: model,
      title: title,
      filter: filter,
      hasDelta: deltaService.computeRawDelta(system).delta?.hasErrorDelta()
    ]
  }

  /**
   * List all processes running on the agent 
   */
  def ps = {
    handleNoAgent {
      return [ps: agentsService.ps(fabric: request.fabric, id: params.id)]
    }
  }

  /**
   * Send a signal to a process
   */
  def kill = {
    handleNoAgent {
      agentsService.kill(fabric: request.fabric, id: params.id, pid: params.pid, signal: params.signal)
      redirect(action: 'ps', id: params.id)
    }
  }

  /**
   * Runs a sync command on the agent
   */
  def sync = {
    handleNoAgent {
      agentsService.sync(fabric: request.fabric, id: params.id)
      redirect(action: 'view', id: params.id)
    }
  }

  /**
   * clear an error in a script
   */
  def clearError = {
    handleNoAgent {
      agentsService.clearError(fabric: request.fabric, *:params)
      redirect(action: 'view', id: params.id)
    }
  }

  /**
   * uninstall the script
   */
  def uninstallScript = {
    handleNoAgent {
      agentsService.uninstallScript(fabric: request.fabric, *:params)
      redirect(action: 'view', id: params.id)
    }
  }

  /**
   * force uninstall the script
   */
  def forceUninstallScript = {
    handleNoAgent {
      agentsService.forceUninstallScript(fabric: request.fabric, *:params)
      redirect(action: 'view', id: params.id)
    }
  }

  /**
   * Renders the full stack trace
   */
  def fullStackTrace = {
    try
    {
      def state = agentsService.getFullState(fabric: request.fabric, *:params)
      if(state?.scriptState?.stateMachine?.error)
      {
        render(template: 'fullStackTrace', model: [exception: state.scriptState.stateMachine.error])
      }
      else
      {
        render ''
      }
    }
    catch(NoSuchAgentException e)
    {
      render "missing agent [${e.message}]"
    }
  }


  /**
   * Start */
  def start = {
    params.state = 'running'
    params.name = params.name ?: 'Start'
    redirectToActionPlan('start') { p, metadata ->
      plannerService.computeTransitionPlans(p, metadata)
    }
  }

  /**
   * Stop */
  def stop = {
    params.state = 'stopped'
    params.name = params.name ?: 'Stop'
    redirectToActionPlan('stop') { p, metadata ->
      plannerService.computeTransitionPlans(p, metadata)
    }
  }

  /**
   * Bounce */
  def bounce = {
    params.name = params.name ?: 'Bounce'
    redirectToActionPlan('bounce') { p, metadata ->
      plannerService.computeBouncePlans(p, metadata)
    }
  }

  /**
   * Undeploy */
  def undeploy = {
    params.name = params.name ?: 'Undeploy'
    redirectToActionPlan('undeploy') { p, metadata ->
      plannerService.computeUndeployPlans(p, metadata)
    }
  }

  /**
   * Redeploy */
  def redeploy = {
    params.name = params.name ?: 'Redeploy'
    redirectToActionPlan('redeploy') { p, metadata ->
      plannerService.computeRedeployPlans(p, metadata)
    }
  }

  /**
   * create action plan
   */
  private def redirectToActionPlan(String action, Closure closure)
  {
    try
    {
      def system = request.system
      system = system.filterBy {
        it.agent == params.id && it.mountPoint == params.mountPoint
      }
      request.system = system

      params.system = system
      params.type = IStep.Type.SEQUENTIAL
      params.name = "${params.name} ${params.id}:${params.mountPoint}".toString()

      def metadata = [
        action: action,
        agent: params.id,
        mountPoint: params.mountPoint
      ]

      def plans = closure(params, metadata)
      if(plans)
      {
        session.plan = plans
        redirect(controller: 'plan', action: 'view', id: plans[0].id)
      }
      else
      {
        flash.error = "No plan to execute ${action}"
        redirect(controller: 'agent', action: 'view', id: params.id)
      }
    }
    catch (Exception e)
    {
      flashException("Error while moving to state ${params}", e)
      redirect(action: 'view', id: params.id)
    }
  }

  /**
   * interrupt the action
   */
  def interruptAction = {
    params.action = params.transitionAction
    try
    {
      agentsService.interruptAction(fabric: request.fabric, *:params)
    }
    catch (Exception e)
    {
      e.printStackTrace()
      flash.error = "Error while interrupting action ${params}"
    }
    redirect(action: 'view', id: params.id)
  }


  /**
   * Displays the log
   */
  def tailLog = {
    handleNoAgent {
      agentsService.tailLog(fabric: request.fabric, *:params) {
        response.contentType = "text/plain"
        response.outputStream << it
      }
    }
  }

  /**
   * getFileContent
   */
  def fileContent = {
    handleNoAgent {
      try
      {
        agentsService.streamFileContent(fabric: request.fabric, *:params) { res ->
          if(res instanceof InputStream)
          {
            response.contentType = "text/plain"
            response.outputStream << res
          }
          else
          {
            render(view: 'directory', model: [dir: res])
          }
        }
      }
      catch (AccessControlException e)
      {
        flash.error = "Not authorized to view ${params.location}"
        redirect(action: 'view', id: params.id)
        return
      }
    }
  }

  /**
   * Retuns the count of agents (HEAD /agents)
   */
  def rest_count_agents = {
    Collection<AgentInfo> agents = agentsService.getAgentInfos(request.fabric)?.values()

    if(agents)
    {
      response.addHeader("X-glu-count", agents.size().toString())
      render ''
    }
    else
    {
      response.sendError(HttpServletResponse.SC_NO_CONTENT,
                         'no agents')
      render ''
    }
  }

  /**
   * Retuns the list of agents (GET /agents)
   */
  def rest_list_agents = {
    Collection<AgentInfo> agents = agentsService.getAgentInfos(request.fabric)?.values()

    if(agents)
    {
      def map = [:]
      agents.each { AgentInfo agent ->
        def agentMap = agent.agentProperties.findAll { !it.key.startsWith('java.') }
        agentMap.viewURL = g.createLink(absolute: true,
                                        mapping: 'restViewAgent',
                                        id: agent.agentName,
                                        params: [fabric: request.fabric.name]).toString()
        map[agent.agentName] = agentMap
      }
      response.setContentType('text/json')
      response.addHeader("X-glu-count", map.size().toString())
      render prettyPrintJsonWhenRequested(map)
    }
    else
    {
      response.sendError(HttpServletResponse.SC_NO_CONTENT,
                         'no agents')
      render ''
    }
  }

  /**
   * Retuns the agent view (GET /agent/<agentName>)
   */
  def rest_view_agent = {
    AgentInfo agent = agentsService.getAgentInfo(request.fabric, params.id)

    if(agent)
    {
      def map = [:]
      map['details'] = agent.agentProperties.findAll { !it.key.startsWith('java.') }
      response.setContentType('text/json')
      render prettyPrintJsonWhenRequested(map)
    }
    else
    {
      response.sendError(HttpServletResponse.SC_NOT_FOUND,
                         'no such agent')
      render ''
    }
  }

  /**
   * Retuns the list of agents versions (GET /agents/versions)
   */
  def rest_list_agents_versions = {
    Collection<AgentInfo> agents = agentsService.getAgentInfos(request.fabric)?.values()

    if(agents)
    {
      def map = [:]
      agents.each { AgentInfo agent ->
        map[agent.agentName] = agent.version
      }
      response.setContentType('text/json')
      response.addHeader("X-glu-count", map.size().toString())
      render prettyPrintJsonWhenRequested(map)
    }
    else
    {
      response.sendError(HttpServletResponse.SC_NO_CONTENT,
                         'no agents')
      render ''
    }
  }

  /**
   * Creates a plan for upgrading the agent (POST /agents/versions)
   */
  def rest_upgrade_agents_versions = {
    if(!params.version || !params.coordinates)
    {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST)
      return
    }

    if(params.agents instanceof String)
    {
      params.agents = [params.agents]
    }

    def availableAgents = agentsService.getAgentInfos(request.fabric).keySet()

    params.agents = params.agents?.findAll { availableAgents.contains(it) }

    params.fabric = request.fabric
    params.type = Type.valueOf(params.type ?: 'PARALLEL')

    def plan =
      plannerService.computeAgentsUpgradePlan(params,
                                              [origin: 'rest', action: 'upgradeAgents', version: params.version])


    if(plan?.hasLeafSteps())
    {
      if(plan.leafSteps.findAll { it.action instanceof NoOpActionDescriptor }.size() == plan.leafStepsCount)
      {
        response.sendError(HttpServletResponse.SC_NO_CONTENT,
                           'no plan created (already upgrading all agents)')
        render ''
      }
      else
      {
        deploymentService.savePlan(plan)
        response.addHeader('Location', g.createLink(absolute: true,
                                                    mapping: 'restPlan',
                                                    id: plan.id,
                                                    params: [fabric: request.fabric]).toString())
        response.setStatus(HttpServletResponse.SC_CREATED)
        render plan.id
      }
    }
    else
    {
      response.sendError(HttpServletResponse.SC_NO_CONTENT, 'no plan created (nothing to upgrade)')
      render ''
    }
  }

  private def handleNoAgent(Closure closure)
  {
    try
    {
      closure()
    }
    catch(NoSuchAgentException e)
    {
      flash.error = "Missing agent [${e.message}]"
      redirect(action: 'view', id: params.id)
    }
  }

  private computeAgentModel(Fabric fabric, agent, mountPoints, hasDelta)
  {
    def entry = [:]

    entry.agent = agent
    entry.mountPoints = agentsService.getMountPointInfos(fabric, agent.agentName) ?: [:]
    if(mountPoints)
      entry.mountPoints = entry.mountPoints.findAll { k,v -> mountPoints.contains(k.path) }

    // if there is no mountPoints then it is not running...
    def state
    if(entry.mountPoints)
    {
      // if all mount points are running then the global state is RUNNING
      state = entry.mountPoints.values().findAll { it.currentState == 'running' }.size() == entry.mountPoints.size() ? 'RUNNING' : 'NOT_RUNNING'

      // if 1 mount point is in transition then the state is TRANSITION
      state = entry.mountPoints.values().find { it.transitionState } ? 'TRANSITION' : state

      // if 1 mount point is in error then the state is ERROR
      state = entry.mountPoints.values().find { it.error } ? 'ERROR' : state

      def actions = [:]
      entry.mountPoints.values().each { MountPointInfo mp ->
        def mpActions = [:]
        def link

        def pid = mp.data?.scriptState?.script?.pid

        if(pid)
        {
          link = g.createLink(controller: 'agents',
                                  action: 'ps',
                                  id: agent.agentName,
                                  params: [pid: mp.data?.scriptState?.script?.pid])
          mpActions[link] = "ps"
        }

        if(mp.transitionState)
        {
          link = g.createLink(controller: 'agents',
                              action: 'interruptAction',
                              id: agent.agentName,
                              params: [mountPoint: mp.mountPoint,
                                       transitionAction: mp.transitionAction,
                                       state: mp.currentState,
                                           timeout: '10s'])
          mpActions[link] = "interrupt ${mp.transitionAction}"
        }
        else
        {
          // when running
          if(mp.currentState == 'running')
          {
            // stop
            link = g.createLink(controller: 'agents',
                                action: 'stop',
                                id: agent.agentName,
                                params: [mountPoint: mp.mountPoint, name: 'Stop'])
            mpActions[link] = "Stop"

            // bounce
            link = g.createLink(controller: 'agents',
                                action: 'bounce',
                                id: agent.agentName,
                                params: [mountPoint: mp.mountPoint, name: 'Bounce'])
            mpActions[link] = "Bounce"
          }
          else
          {
            // start
            link = g.createLink(controller: 'agents',
                                action: 'start',
                                id: agent.agentName,
                                params: [mountPoint: mp.mountPoint, name: 'Start'])
            mpActions[link] = "Start"
          }

          // When not in transition always allow to undeploy
          link = g.createLink(controller: 'agents',
                              action: 'undeploy',
                              id: agent.agentName,
                              params: [mountPoint: mp.mountPoint, name: 'Undeploy'])
          mpActions[link] = "Undeploy"

          // When not in transition always allow to undeploy
          link = g.createLink(controller: 'agents',
                              action: 'redeploy',
                              id: agent.agentName,
                              params: [mountPoint: mp.mountPoint, name: 'Redeploy'])
          mpActions[link] = "Redeploy"
        }

        actions[mp.mountPoint] = mpActions
      }
      entry.actions = actions
    }
    else
    {
      state = 'NOT_RUNNING'
    }



    entry.state = state
    entry.hasDelta = hasDelta

    return entry
  }
}
