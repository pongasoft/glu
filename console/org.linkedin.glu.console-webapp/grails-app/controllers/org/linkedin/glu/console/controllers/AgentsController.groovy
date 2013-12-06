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

import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.provisioner.plan.api.IStep
import org.linkedin.glu.agent.tracker.MountPointInfo
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.util.lang.MemorySize

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
import org.linkedin.glu.orchestration.engine.fabric.FabricService
import org.linkedin.glu.orchestration.engine.system.SystemService
import org.linkedin.glu.groovy.util.state.DefaultStateMachine
import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.glu.orchestration.engine.commands.CommandsService
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.linkedin.groovy.util.config.Config

/**
 * @author ypujante@linkedin.com
 */
class AgentsController extends ControllerBase
{
  /**
   * Default agent actions when none provide
   */
  def static DEFAULT_MOUNTPOINT_ACTIONS = [
    running: [
      [planType: "transition", displayName: "Stop", state: "stopped"],
      [planType: "bounce", displayName: "Bounce"],
      [
        planAction: "reconfigure",
        planType: "transition",
        displayName: "Reconfigure",
        states: ["installed", "running"],
      ],
    ],

    stopped: [
      [
        planAction: "reconfigure",
        planType: "transition",
        displayName: "Reconfigure",
        states: ["installed", "stopped"],
      ],
      [planType: "transition", displayName: "Start", state: "running"],
    ],

    // all other states
    "-": [
      [planType: "transition", displayName: "Start", state: "running"],
    ],

    // actions to include for all states
    "*": [
      [planType: "undeploy", displayName: "Undeploy"],
      [planType: "redeploy", displayName: "Redeploy"],
    ]
  ]

  AgentsService agentsService
  CommandsService commandsService
  DeploymentService deploymentService
  PlannerService plannerService
  DeltaService deltaService
  FabricService fabricService
  SystemService systemService
  ConsoleConfig consoleConfig

  def beforeInterceptor = {
    // we make sure that the fabric is always set before executing any action
    return ensureCurrentFabric()
  }

  /**
   * List all the agents  */
  def list = {
    def agents = agentsService.getAgentInfos(request.fabric)

    def missingAgents = systemService.getMissingAgents(request.fabric, request.system.unfilter())

    def allAgents = new TreeMap<String, AgentInfo>(agents)
    missingAgents.each { agent ->
      if(!allAgents.containsKey(agent))
        allAgents[agent] = null
    }

    [
      agents: allAgents,
      missingAgents: missingAgents as Set
    ]
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
      flash.warning = "No agent to upgrade"
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
      flash.warning = "No agent to cleanup"
      redirect(action: 'listVersions')
    }
  }

  /**
   * View a single agent in an fabric
   */
  def view = {
    def agent = agentsService.getAgentInfo(request.fabric, params.id)

    def model = [:]

    def filter = "agent='${params.id}'".toString()

    def system = request.system.unfilter().filterBy(filter)
    request.system = system

    boolean hasDelta = deltaService.computeRawDelta(system).delta?.hasErrorDelta()

    if(agent)
    {
      def mountPoints = [] as Set
      system.each { mountPoints << it.mountPoint }

      SystemModel currentModel = agentsService.getCurrentSystemModel(request.fabric)
      currentModel.each { mountPoints << it.mountPoint }

      model = computeAgentModel(request.fabric,
                                agent,
                                mountPoints,
                                hasDelta)
    }

    return [
      model: model,
      hasDelta: hasDelta
    ]
  }

  /**
   * Renders the plans subtab
   */
  def plans = {
    def agent = agentsService.getAgentInfo(request.fabric, params.id)

    def title = "agent [${params.id}]".toString()
    def filter = "agent='${params.id}'".toString()

    def system = request.system.unfilter().filterBy(filter)
    request.system = system

    boolean hasDelta = deltaService.computeRawDelta(system).delta?.hasErrorDelta()

    return [
      agent: agent,
      title: title,
      filter: filter,
      hasDelta: hasDelta
    ]
  }

  def clear = {
    ensureCurrentFabric()

    try
    {
      boolean cleared = agentsService.clearAgentInfo(request.fabric, params.id)
      fabricService.clearAgentFabric(params.id, request.fabric.name)
      if(cleared)
        flash.success = "Agent [${params.id}] was cleared."
      else
        flash.info = "Agent [${params.id}] was already cleared."
    }
    catch (IllegalStateException e)
    {
      flash.error = e.message
    }

    redirect(controller: 'fabric', action: 'listAgentFabrics')
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
      agentsService.clearError(*:params, fabric: request.fabric)
      redirect(action: 'view', id: params.id)
    }
  }

  /**
   * uninstall the script
   */
  def uninstallScript = {
    handleNoAgent {
      agentsService.uninstallScript(*:params, fabric: request.fabric)
      redirect(action: 'view', id: params.id)
    }
  }

  /**
   * force uninstall the script
   */
  def forceUninstallScript = {
    handleNoAgent {
      agentsService.forceUninstallScript(*:params, fabric: request.fabric)
      redirect(action: 'view', id: params.id)
    }
  }

  /**
   * Renders the full stack trace
   */
  def fullStackTrace = {
    try
    {
      def state = agentsService.getFullState(*:params, fabric: request.fabric)
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
   * Create the plan */
  def create_plan = {
    try
    {
      def system = request.system
      system = system.unfilter().filterBy { entry ->
        entry != null && entry.agent == params.id && entry.mountPoint == params.mountPoint
      }
      request.system = system

      def args = GluGroovyCollectionUtils.xorMap(params,
                                                 ['controller', 'action', 'id', '__nvbe', '__role'])

      // turn it into a collection (rather than an array)
      if(args.states)
        args.states = args.states.collect { it }

      def metadata = [*:args, agent: params.id, mountPoint: params.mountPoint]

      args.system = system
      args.stepType = params.stepType ?: IStep.Type.SEQUENTIAL
      args.name = params.planName ?: "${params.displayName ?: params.planType.capitalize()} ${params.id}:${params.mountPoint}".toString()

      def plans = plannerService.computePlans(args, metadata)
      if(plans)
      {
        session.plan = plans
        redirect(controller: 'plan', action: 'view', id: plans[0].id)
      }
      else
      {
        flash.error = "No plan to execute ${args.planAction}"
        redirect(controller: 'agents', action: 'view', id: args.id)
      }
    }
    catch (Exception e)
    {
      flashException("Error while moving to state ${args}", e)
      redirect(action: 'view', id: args.id)
    }
  }

  /**
   * interrupt the action
   */
  def interruptAction = {
    params.action = params.transitionAction
    try
    {
      agentsService.interruptAction(*:params, fabric: request.fabric)
    }
    catch (Exception e)
    {
      flashException("Error while interrupting action ${params}", e)
    }
    redirect(action: 'view', id: params.id)
  }


  /**
   * Displays the log
   */
  def tailLog = {
    handleNoAgent {
      agentsService.tailLog(*:params, fabric: request.fabric) {
        response.contentType = "text/plain"
        response.outputStream << it
      }
    }
  }

  /**
   * getFileContent
   */
  def fileContent = {
    if(params.file)
    {
      render(view: 'file', model: [location: params.file])
      return
    }

    handleNoAgent {
      try
      {
        // this is for backward compatibility with previous agents which don't know the offset
        // parameter
        if(params.offset)
          params.maxLine = 500

        agentsService.streamFileContent(*:params, fabric: request.fabric) { res ->
          if(res instanceof InputStream)
          {
            if(params.binaryOutput)
            {
              response.contentType = "application/octet-stream"
              def filename = new File(URI.create("file:${params.location}")).name
              response.addHeader('Content-Disposition',
                                 "attachment; filename=\"${filename.encodeAsURL()}\"")
            }
            else
              response.contentType = "text/plain"

            response.outputStream << res
          }
          else
          {
            if(params.offset)
            {
              if(res?.tailStream)
              {
                GluGroovyCollectionUtils.xorMap(res, ['tailStream']).each { k, v ->
                  response.addHeader("X-glu-${k}", v.toString())
                }
                if(res.length > 0)
                  response.addHeader("X-glu-length-as-MemorySize",
                                     MemorySize.parse(res.length.toString()).canonicalString)
                if(res.lastModified)
                  response.addHeader('X-glu-lastModified-as-String',
                                     cl.formatDate(time: res.lastModified).toString())

                  response.contentType = "text/plain"
                  response.outputStream << res.tailStream
              }
              else
                render ''
            }
            else
            {
              render(view: 'directory', model: [dir: res])
            }
          }
        }
      }
      catch (AccessControlException ignored)
      {
        if(params.offset)
        {
          response.addHeader("X-glu-unauthorized", params.location)
          render ''
        }
        else
        {
          flash.error = "Not authorized to view ${params.location}"
          redirect(action: 'view', id: params.id)
        }
        return
      }
    }
  }

  /**
   * Commands view page
   */
  def commands = {
    // nothing special to do... simply renders the gsp
  }

  /**
   * Executing a command
   */
  def executeCommand = {
    handleNoAgent {
      def args = [:]
      boolean redirectStderr = Config.getOptionalBoolean(params, 'redirectStderr', false)
      if(params.command)
      {
        args.commandId = commandsService.executeShellCommand(request.fabric,
                                                             params.id,
                                                             [ command: params.command,
                                                               redirectStderr: redirectStderr
                                                             ])
      }
      else
      {
        flash.error = "Please enter a command to execute"
      }
      if(!redirectStderr)
        args.redirectStderr = false
      redirect(action: 'commands', id: params.id, params: args)
    }
  }

  /**
   * interrupt the command
   */
  def interruptCommand = {
    try
    {
      boolean interrupted = commandsService.interruptCommand(request.fabric,
                                                             params.id,
                                                             params.commandId)
      if(interrupted)
        flash.message = "Command [${params.commandId}] interrupted."
      else
        flash.message = "Command [${params.commandId}] already completed."
    }
    catch (Exception e)
    {
      flashException("Error while interrupting command ${params}", e)
    }
    redirect(action: 'commands', id: params.id, params: [commandId: params.commandId])
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
    }
  }

  /**
   * Clears the agent (DELETE /agent/<agentName>)
   */
  def rest_delete_agent = {
    try
    {
      boolean cleared = agentsService.clearAgentInfo(request.fabric, params.id)
      fabricService.clearAgentFabric(params.id, request.fabric.name)
      if(cleared)
        response.setStatus(javax.servlet.http.HttpServletResponse.SC_OK)
      else
        response.setStatus(javax.servlet.http.HttpServletResponse.SC_NOT_FOUND)
      render ''
    }
    catch (IllegalStateException e)
    {
      response.addHeader("X-glu-error", e.message)
      response.sendError(HttpServletResponse.SC_CONFLICT, e.message)
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

  /**
   * curl -v -u "glua:password" "http://localhost:8080/console/rest/v1/glu-dev-1/agent/agent-2/commands?command=/tmp/shellScriptTestShellExec.sh%20-1%20-2%20-c%20-e" -H "Content-Type: application/octet-stream" --data-binary 'abcdef'
   */
  def rest_execute_shell_command = {
    def args = GluGroovyCollectionUtils.subMap(params, ['command', 'redirectStderr', 'type'])

    if(request.contentLength != 0)
      args.stdin = request.inputStream
    
    try
    {
      def id = commandsService.executeShellCommand(request.fabric, params.id, args)

      response.setStatus(HttpServletResponse.SC_CREATED)
      response.setContentType('text/json')
      response.addHeader('X-glu-command-id', id)
      render prettyPrintJsonWhenRequested([id: id])
    }
    catch(NoSuchAgentException e)
    {
      response.sendError(HttpServletResponse.SC_NOT_FOUND)
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

  def rest_agent_get_file_content = {
    println params
    render ''
  }

  private computeAgentModel(Fabric fabric, agent, mountPoints, hasDelta)
  {
    def entry = [:]

    def mountPointActions = consoleConfig.defaults.mountPointActions ?: DEFAULT_MOUNTPOINT_ACTIONS

    entry.agent = agent
    entry.mountPoints = agentsService.getMountPointInfos(fabric, agent.agentName) ?: [:]
    if(mountPoints)
      entry.mountPoints = entry.mountPoints.findAll { k,v -> mountPoints.contains(k.path) }

    // if there is no mountPoints then it is not running...
    def state
    if(entry.mountPoints)
    {
      // if all mount points are running then the global state is RUNNING
      state = entry.mountPoints.values().findAll { it.currentState == DefaultStateMachine.DEFAULT_ENTRY_STATE }.size() == entry.mountPoints.size() ? 'RUNNING' : 'NOT_RUNNING'

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
          link = cl.createLink(controller: 'agents',
                               action: 'ps',
                               id: agent.agentName,
                               params: [pid: mp.data?.scriptState?.script?.pid])
          mpActions[link] = "ps"
        }

        if(mp.transitionState)
        {
          link = cl.createLink(controller: 'agents',
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
          if(!mp.isCommand())
          {
            def stateActions = mountPointActions[mp.currentState] ?: (mountPointActions["-"] ?: [])

            stateActions = [*stateActions, *(mountPointActions["*"] ?: [])]

            stateActions?.each { stateAction ->
              link = cl.createLink(controller: 'agents',
                                   action: 'create_plan',
                                   id: agent.agentName,
                                   params: [
                                     mountPoint: mp.mountPoint,
                                     *:stateAction,
                                   ])
              mpActions[link] = stateAction.displayName ?: stateAction.planType.capitalize()
            }
          }
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
