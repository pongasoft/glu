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

import org.linkedin.glu.orchestration.engine.fabric.FabricService
import org.linkedin.glu.console.domain.Fabric
import org.linkedin.util.lifecycle.CannotConfigureException
import org.linkedin.glu.console.domain.RoleName
import org.linkedin.glu.console.domain.User
import org.linkedin.glu.orchestration.engine.system.SystemService
import org.linkedin.glu.provisioner.core.model.SystemModel
import javax.servlet.http.HttpServletResponse

class FabricController extends ControllerBase
{
  FabricService fabricService
  SystemService systemService

  def index = { redirect(action:select, params:params) }

  def select = {
    def fabrics = fabricService.fabrics

    if(!fabrics)
    {
      def user = User.findByUsername(request.user?.username)
      if(user?.hasRole(RoleName.ADMIN))
      {
        redirect(action: create)
        return
      }
    }

    if(params.id)
    {
      def newFabric = fabrics.find { it.name == params.id }
      if(newFabric)
      {
        request.fabric = newFabric
        flash.success = "Selected fabric '${params.id}'"
      }
      else
        flash.error = "Unknown fabric '${params.id}'"
    }

    [ fabrics: fabrics ]
  }

  /**
   * List all the agents known in their fabric
   */
  def listAgentFabrics = {
    ensureCurrentFabric()
    def missingAgents = systemService.getMissingAgents(request.fabric)
    def agents = fabricService.getAgents()

    def unassignedAgents = new TreeMap()
    def assignedAgents = new TreeMap()

    agents.collect { name, fabricName ->
      if(missingAgents.contains(name))
      {
        if(fabricName)
          unassignedAgents[name] = 'missing-old'
        else
          unassignedAgents[name] = 'missing-new'
      }
      else
      {
        if(fabricName)
        {
          assignedAgents[name] = fabricName
        }
        else
        {
          unassignedAgents[name] = 'unknown'
        }
      }
    }

    missingAgents.each { agent ->
      if(!unassignedAgents[agent])
      {
        unassignedAgents[agent] = 'missing-new'
      }
    }

    return [unassignedAgents: unassignedAgents,
            assignedAgents: assignedAgents,
            fabrics: fabricService.fabrics]
  }

  /**
   * After selecting the agents to set the fabric, sets the provided fabric
   */
  def setAgentsFabrics = {
    ensureCurrentFabric()
    def agents = new TreeSet(fabricService.getAgents().keySet())
    agents.addAll(systemService.getMissingAgents(request.fabric))

    def errors = [:]

    agents.each { String agent ->
      String fabric = params[agent] as String
      if(fabric)
      {
        try
        {
          fabricService.setAgentFabric(agent, fabric)
          fabricService.configureAgent(InetAddress.getByName(agent), fabric)
        }
        catch(CannotConfigureException e)
        {
          errors[agent] = e
        }
      }
    }

    if(errors)
    {
      flash.warning = "There were ${errors.size()} warning(s)"
      flash.errors = errors
    }
    else
    {
      // leave a little bit of time for ZK to catch up
      Thread.sleep(1000)
    }

    redirect(action: listAgentFabrics)
  }

  def clearAgentFabric = {
    ensureCurrentFabric()

    boolean cleared = fabricService.clearAgentFabric(params.id, request.fabric.name)

    if(cleared)
      flash.success = "Fabric [${request.fabric.name}] was cleared for agent [${params.id}]."
    else
      flash.info = "Fabric [${request.fabric.name}] for agent [${params.id}] was already cleared."

    redirect(action: listAgentFabrics)
  }

  // YP Note: what is below is coming from the scaffolding (copy/pasted)

  // the delete, save and update actions only accept POST requests
  static allowedMethods = [delete: 'POST', save: 'POST', update: 'POST']

  def list = {
    params.max = Math.min(params.max ? params.max.toInteger() : 10, 100)
    [fabricInstanceList: Fabric.list(params), fabricInstanceTotal: Fabric.count()]
  }

  def show = {
    def fabricInstance = Fabric.get(params.id)

    if(!fabricInstance)
    {
      flash.warning = "Fabric not found with id ${params.id}"
      redirect(action: list)
    }
    else
    { return [fabricInstance: fabricInstance] }
  }

  def delete = {
    withLock("fabric") {
      Fabric.withTransaction {
        def fabricInstance = Fabric.get(params.id)
        if(fabricInstance)
        {
          try
          {
            Fabric.executeUpdate("delete Fabric f where f.id=?", [fabricInstance.id])
            fabricService.resetCache()
            flash.success = "Fabric ${params.id} deleted"
            audit('fabric.deleted', params.id.toString())
            redirect(action: list)
          }
          catch (org.springframework.dao.DataIntegrityViolationException e)
          {
            flash.warning = "Fabric ${params.id} could not be deleted"
            redirect(action: show, id: params.id)
          }
        }
        else
        {
          flash.warning = "Fabric not found with id ${params.id}"
          redirect(action: list)
        }
      }
    }
  }

  def edit = {
    def fabricInstance = Fabric.get(params.id)

    if(!fabricInstance)
    {
      flash.warning = "Fabric not found with id ${params.id}"
      redirect(action: list)
    }
    else
    {
      return [fabricInstance: fabricInstance]
    }
  }

  def update = {
    withLock("fabric") {
      Fabric.withTransaction {
        def fabricInstance = Fabric.get(params.id)
        if(fabricInstance)
        {
          if(params.version)
          {
            def version = params.version.toLong()
            if(fabricInstance.version > version)
            {

              fabricInstance.errors.rejectValue("version", "fabric.optimistic.locking.failure", "Another user has updated this Fabric while you were editing.")
              render(view: 'edit', model: [fabricInstance: fabricInstance])
              return
            }
          }
          fabricInstance.properties = params
          if(!fabricInstance.hasErrors() && fabricInstance.save())
          {
            fabricService.resetCache()
            flash.success = "Fabric ${params.id} updated"
            audit('fabric.updated', params.id.toString(), params.toString())
            redirect(action: show, id: fabricInstance.id)
          }
          else
          {
            render(view: 'edit', model: [fabricInstance: fabricInstance])
          }
        }
        else
        {
          flash.warning = "Fabric not found with id ${params.id}"
          redirect(action: list)
        }
      }
    }
  }

  def create = {
    def fabricInstance = new Fabric()
    fabricInstance.properties = params
    return ['fabricInstance': fabricInstance]
  }

  def save = {
    withLock("fabric") {
      Fabric.withTransaction {
        def fabricInstance = new Fabric(params)
        if(!fabricInstance.hasErrors() && fabricInstance.save())
        {
          fabricService.resetCache()
          flash.success = "Fabric ${fabricInstance.id} created"
          audit('fabric.updated', fabricInstance.id.toString(), params.toString())
          def emptySystem = new SystemModel(fabric: fabricInstance.name)
          emptySystem.metadata.name = "Empty System Model"
          systemService.saveCurrentSystem(emptySystem)
          redirect(action: show, id: fabricInstance.id)
        }
        else
        {
          render(view: 'create', model: [fabricInstance: fabricInstance])
        }
      }
    }
  }

  def refresh = {
    fabricService.resetCache()
    redirect(action: 'select')
  }

  /**
   * Retuns the list of fabrics (GET /-)
   */
  def rest_list_fabrics = {
    Collection<String> fabrics = fabricService.listFabricNames()
    response.setContentType('text/json')
    response.addHeader("X-glu-count", fabrics.size().toString())
    render prettyPrintJsonWhenRequested(fabrics)
  }

  /**
   * Returns the details about a fabric (GET /<fabric>)
   */
  def rest_view_fabric = {
    if(request.fabric?.name != params.fabric)
    {
      response.setStatus(HttpServletResponse.SC_NOT_FOUND)
      render ''
      return
    }

    response.setContentType('text/json')
    def fabric = [
      name: request.fabric.name,
      zkConnectString: request.fabric.zkConnectString,
      zkSessionTimeout: request.fabric.zkSessionTimeout.toString(),
      color: request.fabric.color
    ]
    render prettyPrintJsonWhenRequested(fabric)
  }

  /**
   * Update (or add if not existent) a fabric (PUT /<fabric>)
   */
  def rest_add_or_update_fabric = {
    withLock("fabric") {
      Fabric.withTransaction {
        // adding new fabric
        if(request.fabric?.name != params.fabric)
        {
          params.name = params.fabric
          def fabricInstance = new Fabric(params)
          if(!fabricInstance.hasErrors() && fabricInstance.save())
          {
            fabricService.resetCache()
            audit('fabric.created', fabricInstance.id.toString(), params.toString())
            def emptySystem = new SystemModel(fabric: fabricInstance.name)
            emptySystem.metadata.name = "Empty System Model"
            systemService.saveCurrentSystem(emptySystem)
            response.setStatus(HttpServletResponse.SC_OK)
            render ''
          }
          else
          {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST, fabricInstance.errors.toString())
            render fabricInstance.errors.toString()
          }
        }
        else
        {
          // updating fabric
          def fabricInstance = Fabric.findByName(params.fabric)
          fabricInstance.properties = params
          if(!fabricInstance.hasErrors() && fabricInstance.save())
          {
            fabricService.resetCache()
            audit('fabric.updated', params.fabric, params.toString())
            response.setStatus(HttpServletResponse.SC_OK)
            render ''
          }
          else
          {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST, fabricInstance.errors.toString())
            render fabricInstance.errors.toString()
          }
        }
      }
    }
  }

  /**
   * Deletes the fabric
   */
  def rest_delete_fabric = {
    withLock("fabric") {
      Fabric.withTransaction {
        // already does not exist...
        if(request.fabric?.name != params.fabric)
        {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND)
          render ''
          return
        }

        def fabricInstance = Fabric.findByName(params.fabric)
        if(fabricInstance)
        {
          try
          {
            Fabric.executeUpdate("delete Fabric f where f.id=?", [fabricInstance.id])
            fabricService.resetCache()
            audit('fabric.deleted', params.fabric.toString())
            response.setStatus(HttpServletResponse.SC_OK)
            render ''
          }
          catch (org.springframework.dao.DataIntegrityViolationException e)
          {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR)
            render e.message
          }
        }
        else
        {
          response.setStatus(HttpServletResponse.SC_NOT_FOUND)
          render ''
        }
      }
    }
  }

  /**
   * Retuns the list of agents fabrics (GET /-/agents)
   */
  def rest_list_agents_fabrics = {
    def agents = fabricService.getAgents()

    // if an agent is defined in the model but entirely missing from the map it means that
    // the agent has never been started
    fabricService.fabrics.each { fabric ->
      systemService.getMissingAgents(fabric).each { agent ->
        if(!agents.containsKey(agent))
          agents[agent] = '-'
      }
    }

    // replacing all 'null' values by '-' (json output is removing null values :(
    agents.findAll { agent, fabric -> fabric == null }.keySet().each {
      agents[it] = '-'
    }

    response.setContentType('text/json')
    response.addHeader("X-glu-count", agents.size().toString())
    render prettyPrintJsonWhenRequested(agents)
  }

  /**
   * Assign fabric to agent (PUT /<fabric>/agent/<agent>/fabric)
   */
  def rest_set_agent_fabric = {
    def fabric = request.fabric

    if(!fabric)
    {
      response.addHeader("X-glu-error", "missing (or unknown) fabric")
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing (or unknown) fabric")
      return
    }

    fabricService.setAgentFabric(params.id, fabric.name)

    def configParam = null

    if(params.host)
      configParam = InetAddress.getByName(params.host)

    if(params.uri)
      configParam = new URI(params.uri)

    if(configParam)
    {
      try
      {
        fabricService.configureAgent(configParam, fabric.name)
      }
      catch(CannotConfigureException e)
      {
        response.addHeader("X-glu-error", e.message)
        response.sendError(HttpServletResponse.SC_CONFLICT, e.message)
        return
      }
    }

    response.setStatus(HttpServletResponse.SC_OK)
    render ''
  }

  /**
   * Assign fabric to agent (DELETE /<fabric>/agent/<agent>/fabric)
   */
  def rest_clear_agent_fabric = {
    def fabric = request.fabric

    if(!fabric)
    {
      response.addHeader("X-glu-error", "missing (or unknown) fabric")
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "missing (or unknown) fabric")
      return
    }

    boolean cleared = fabricService.clearAgentFabric(params.id, fabric.name)

    if(cleared)
      response.setStatus(HttpServletResponse.SC_OK)
    else
      response.setStatus(HttpServletResponse.SC_NOT_FOUND)

    render ''
  }
}
