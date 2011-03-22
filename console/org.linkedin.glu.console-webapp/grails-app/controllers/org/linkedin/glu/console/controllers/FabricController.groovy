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

package org.linkedin.glu.console.controllers

import org.linkedin.glu.provisioner.services.fabric.FabricService
import org.linkedin.glu.console.domain.Fabric
import org.linkedin.glu.console.services.SystemService
import org.linkedin.util.lifecycle.CannotConfigureException
import org.linkedin.glu.console.domain.RoleName
import org.linkedin.glu.console.domain.User

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
        flash.message = "Selected fabric '${params.id}'"
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
      if(params[agent])
      {
        try
        {
          fabricService.setAgentFabric(agent, params[agent] as String)
        }
        catch(CannotConfigureException e)
        {
          errors[agent] = e
        }
      }
    }

    if(errors)
    {
      flash.message = "There were ${errors.size()} warning(s)"
      flash.errors = errors
    }
    else
    {
      // leave a little bit of time for ZK to catch up
      Thread.sleep(1000)
    }

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
      flash.message = "Fabric not found with id ${params.id}"
      redirect(action: list)
    }
    else
    { return [fabricInstance: fabricInstance] }
  }

  def delete = {
    def fabricInstance = Fabric.get(params.id)
    if(fabricInstance)
    {
      try
      {
        fabricInstance.delete(flush: true)
        fabricService.resetCache()
        flash.message = "Fabric ${params.id} deleted"
        audit('fabric.deleted', params.id.toString())
        redirect(action: list)
      }
      catch (org.springframework.dao.DataIntegrityViolationException e)
      {
        flash.message = "Fabric ${params.id} could not be deleted"
        redirect(action: show, id: params.id)
      }
    }
    else
    {
      flash.message = "Fabric not found with id ${params.id}"
      redirect(action: list)
    }
  }

  def edit = {
    def fabricInstance = Fabric.get(params.id)

    if(!fabricInstance)
    {
      flash.message = "Fabric not found with id ${params.id}"
      redirect(action: list)
    }
    else
    {
      return [fabricInstance: fabricInstance]
    }
  }

  def update = {
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
        flash.message = "Fabric ${params.id} updated"
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
      flash.message = "Fabric not found with id ${params.id}"
      redirect(action: list)
    }
  }

  def create = {
    def fabricInstance = new Fabric()
    fabricInstance.properties = params
    return ['fabricInstance': fabricInstance]
  }

  def save = {
    def fabricInstance = new Fabric(params)
    if(!fabricInstance.hasErrors() && fabricInstance.save())
    {
      fabricService.resetCache()
      flash.message = "Fabric ${fabricInstance.id} created"
      audit('fabric.updated', fabricInstance.id.toString(), params.toString())
      redirect(action: show, id: fabricInstance.id)
    }
    else
    {
      render(view: 'create', model: [fabricInstance: fabricInstance])
    }
  }

  def refresh = {
    fabricService.resetCache()
    redirect(action: 'select')
  }
}
