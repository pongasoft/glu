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

import org.linkedin.glu.provisioner.services.agents.AgentsService
import org.linkedin.glu.console.services.SystemService
import org.linkedin.glu.console.services.AuditService
import org.linkedin.glu.provisioner.plan.api.IStep
import org.linkedin.glu.agent.tracker.MountPointInfo
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.provisioner.services.fabric.Fabric
import java.security.AccessControlException

/**
 * @author ypujante@linkedin.com
 */
class AgentsController extends ControllerBase
{
  AgentsService agentsService
  SystemService systemService
  AuditService auditService

  def beforeInterceptor = {
    // we make sure that the fabric is always set before executing any action
    return ensureCurrentFabric()
  }

  /**
   * List all the agents (in the current fabric)
   */
  def list = {
    def agents = new TreeMap(agentsService.getAgentInfos(request.fabric))

    def mountPoints = [:]

    def model = [:]

    def hosts = systemService.getHostsWithDeltas(fabric: request.fabric.name)

    agents.values().each { agent ->
      model[agent.agentName] = computeAgentModel(request.fabric, agent, null, hosts.contains(agent.agentName))
    }

    return [model: model, count: model.size(), instances: model.values().sum { it.mountPoints?.size() }]
  }

  /**
   * List all the agents (in the current fabric) with their version (in preparation to upgrade)
   */
  def listVersions = {
    def agents = agentsService.getAgentInfos(request.fabric)

    def versions = agents.values().groupBy { agent ->
      agent.agentProperties['org.linkedin.glu.agent.version']
    }

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

    def plan = agentsService.createAgentsUpgradePlan(params)

    session.delta = [plan]

    redirect(controller: 'plan', action: 'view', id: plan.id)
  }

  /**
   * cleanup
   */
  def cleanup = {
    if(!params.version)
    {
      flash.error = "Missing version"
      redirect(action: 'listVersions')
      return
    }

    if(params.agents instanceof String)
    {
      params.agents = [params.agents]
    }

    params.fabric = request.fabric
    
    def plan = agentsService.createAgentsCleanupUpgradePlan(params)

    session.delta = [plan]

    redirect(controller: 'plan', action: 'view', id: plan.id)
  }

  /**
   * View a single agent in an fabric
   */
  def view = {
    def agent = agentsService.getAgentInfo(request.fabric, params.id)

    def model = [:]
    def plans
    def bouncePlans
    def redeployPlans

    def title = "agent [${params.id}]"
    
    if(agent)
    {
      params.fabric = request.fabric
      params.name = title

      def system = request.system
      system = system?.filterBy {
        it.agent == params.id
      }

      request.system = system
      params.system = system

      Plan plan = systemService.computeDeploymentPlan(params) { true }

      session.delta = []

      plans = systemService.groupByInstance(plan, [type: 'deploy', agent: params.id])

      session.delta.addAll(plans)

      def bouncePlan = systemService.computeBouncePlan(params)  { true }
      if(bouncePlan)
      {
        bouncePlan.name = "Bounce: ${title}"
        bouncePlans = systemService.groupByInstance(bouncePlan, [type: 'bounce', agent: params.id])
        session.delta.addAll(bouncePlans)
      }

      def redeployPlan = systemService.computeRedeployPlan(params) { true }
      if(redeployPlan)
      {
        redeployPlan.name = "Redeploy: ${title}"
        redeployPlans = systemService.groupByInstance(redeployPlan, [type: 'redeploy', agent: params.id])
        session.delta.addAll(redeployPlans)
      }

      def mountPoints = [] as Set
      system.each { mountPoints << it.mountPoint }

      model = computeAgentModel(request.fabric,
                                agent,
                                mountPoints,
                                plan?.hasLeafSteps())
    }

    return [
        model: model,
        delta: plans,
        bounce: bouncePlans,
        redeploy: redeployPlans,
        title: title
    ]
  }

  /**
   * List all processes running on the agent 
   */
  def ps = {
    return [ps: agentsService.ps(fabric: request.fabric, id: params.id)]
  }

  /**
   * Send a signal to a process
   */
  def kill = {
    agentsService.kill(fabric: request.fabric, id: params.id, pid: params.pid, signal: params.signal)

    redirect(action: 'ps', id: params.id)
  }

  /**
   * Runs a sync command on the agent
   */
  def sync = {
    agentsService.sync(fabric: request.fabric, id: params.id)
    redirect(action: 'view', id: params.id)
  }

  /**
   * clear an error in a script
   */
  def clearError = {
    agentsService.clearError(fabric: request.fabric, *:params)
    redirect(action: 'view', id: params.id)
  }

  /**
   * uninstall the script
   */
  def uninstallScript = {
    agentsService.uninstallScript(fabric: request.fabric, *:params)
    redirect(action: 'view', id: params.id)
  }

  /**
   * force uninstall the script
   */
  def forceUninstallScript = {
    agentsService.forceUninstallScript(fabric: request.fabric, *:params)
    redirect(action: 'view', id: params.id)
  }

  /**
   * Renders the full stack trace
   */
  def fullStackTrace = {
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


  /**
   * Start */
  def start = {
    params.state = 'running'
    params.name = params.name ?: 'Start'
    redirectToActionPlan('start') { p ->
      systemService.computeTransitionPlan(p) { true }
    }
  }

  /**
   * Stop */
  def stop = {
    params.state = 'stopped'
    params.name = params.name ?: 'Stop'
    redirectToActionPlan('stop') { p ->
      systemService.computeTransitionPlan(p) { true }
    }
  }

  /**
   * Bounce */
  def bounce = {
    params.name = params.name ?: 'Bounce'
    redirectToActionPlan('bounce') { p ->
      systemService.computeBouncePlan(p) { true }
    }
  }

  /**
   * Undeploy */
  def undeploy = {
    params.name = params.name ?: 'Undeploy'
    redirectToActionPlan('undeploy') { p ->
      systemService.computeUndeployPlan(p) { true }
    }
  }

  /**
   * Redeploy */
  def redeploy = {
    params.name = params.name ?: 'Redeploy'
    redirectToActionPlan('redeploy') { p ->
      systemService.computeRedeployPlan(p) { true }
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
      params.fabric = request.fabric

      def plan = closure(params)

      if(params.name)
      {
        plan.name = "${params.name} ${params.id}:${params.mountPoint}".toString()
      }
      plan = systemService.groupByInstance(plan,
                                           IStep.Type.SEQUENTIAL,
                                           [action: action,
                                            agent: params.id,
                                            mountPoint: params.mountPoint])
      session.delta = [plan]
      redirect(controller: 'plan', action: 'view', id: plan.id)
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
    agentsService.tailLog(fabric: request.fabric, *:params) {
      response.contentType = "text/plain"
      response.outputStream << it
    }
  }

  /**
   * getFileContent
   */
  def fileContent = {
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
