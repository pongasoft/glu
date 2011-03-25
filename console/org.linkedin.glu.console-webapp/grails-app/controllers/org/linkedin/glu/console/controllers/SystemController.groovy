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
import org.linkedin.glu.provisioner.services.deployment.DeploymentService
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.console.domain.DbSystemModel
import org.linkedin.glu.provisioner.core.model.JSONSystemModelSerializer
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.services.system.SystemService

class SystemController extends ControllerBase
{
  AgentsService agentsService
  DeploymentService deploymentService
  SystemService systemService

  def beforeInterceptor = {
    // we make sure that the fabric is always set before executing any action
    return ensureCurrentFabric()
  }

  /**
   * Listing all systems (paginated)
   */
  def list = {
    params.max = Math.min(params.max ? params.max.toInteger() : 10, 100)
    params.sort = 'id'
    params.order = 'desc'

    def systems = DbSystemModel.findAllByFabric(request.fabric.name, params)

    [
        currentSystem: DbSystemModel.findCurrent(request.fabric),
        systems: systems,
        total: DbSystemModel.count()
    ]
  }

  /**
   * Viewing a single system (json textarea)
   */
  def view = {
    def system = DbSystemModel.findBySystemId(params.id)
    [system: system]
  }

  /**
   * Saving the current system
   */
  def save = {
    def system = DbSystemModel.findBySystemId(params.id)

    try
    {
      def systemModel = DbSystemModel.SERIALIZER.deserialize(params.content)
      if(systemModel.fabric != request.fabric.name)
      {
        flash.error = "Fabric mismatch: ${systemModel.fabric} != ${request.fabric.name}"
        render(view: 'view', id: params.id, model: [system: system])
        return
      }
      systemModel.id = null
      if(systemService.saveCurrentSystem(systemModel))
        flash.message = "New system properly saved [${systemModel.id}]"
      else
        flash.message = "Already current system"
      redirect(action: 'view', id: systemModel.id)
    }
    catch(Throwable th)
    {
      flashException("Could not save the new model: ${th.message}", th)
      render(view: 'view', id: params.id, model: [system: system])
    }
  }

  /**
   * Add a single entry to the model
   */
  def addEntry = {
    def currentSystem = DbSystemModel.findCurrent(request.fabric)?.systemModel?.clone()

    if(currentSystem)
    {
      currentSystem.addEntry(new SystemEntry(agent: params.agent,
                                             mountPoint: params.mountPoint,
                                             script: params.script))

      currentSystem.id = null

      def newCurrentSystem = systemService.saveCurrentSystem(currentSystem).systemModel

      flash.message = "Successfully added ${params.agent}:${params.mountPoint} entry."

      redirect(action: 'view', id: newCurrentSystem.systemId)
    }
    else
    {
      flash.error = "No current system"
      redirect(action: 'list')
    }
  }

  def delta = {
    if(request.system)
    {
      def title = "Fabric [${request.fabric}]"

      params.system = request.system
      params.fabric = request.fabric
      params.name = "Deploy: ${title}".toString()

      def missingAgents = systemService.getMissingAgents(request.fabric, request.system)

      Plan plan = deploymentService.computeDeploymentPlan(params)

      def plans =
        deploymentService.groupByInstance(plan,
                                          [type: 'deploy',
                                          fabric: request.fabric.name])

      session.delta = []

      session.delta.addAll(plans)

      def bouncePlans

      def bouncePlan = deploymentService.computeBouncePlan(params) { true }
      if(bouncePlan)
      {
        bouncePlan.name = "Bounce: ${title}"
        bouncePlans =
          deploymentService.groupByInstance(bouncePlan,
                                            [type: 'bounce',
                                            fabric: request.fabric.name])
        session.delta.addAll(bouncePlans)
      }

      def redeployPlans

      def redeployPlan = deploymentService.computeRedeployPlan(params) { true }
      if(redeployPlan)
      {
        redeployPlan.name = "Redeploy: ${title}"
        redeployPlans =
          deploymentService.groupByInstance(redeployPlan,
                                            [type: 'redeploy',
                                            fabric: request.fabric.name])
        session.delta.addAll(redeployPlans)
      }

      def undeployPlans

      def undeployPlan = deploymentService.computeUndeployPlan(params) { true }
      if(undeployPlan)
      {
        undeployPlan.name = "Undeploy: ${title}"
        undeployPlans =
          deploymentService.groupByInstance(undeployPlan,
                                            [type: 'undeploy',
                                            fabric: request.fabric.name])
        session.delta.addAll(undeployPlans)
      }

      [
          delta: plans,
          bounce: bouncePlans,
          redeploy: redeployPlans,
          undeploy: undeployPlans,
          title: title,
          executingDeploymentPlan: deploymentService.isExecutingDeploymentPlan(request.fabric.name),
          missingAgents: missingAgents
      ]
    }
  }

  /**
   * Filter the model by the provided filter
   */
  def filter = {
    if(request.system)
    {
      session.delta = []

      def missingAgents = systemService.getMissingAgents(request.fabric, request.system)

      def args = [:]
      args.system = request.system
      args.fabric = request.fabric
      args.name = params.title

      Plan plan = deploymentService.computeDeploymentPlan(args)

      def plans =
        deploymentService.groupByInstance(plan, [type: 'deploy', name: args.name])

      session.delta.addAll(plans)

      def bouncePlans

      def bouncePlan = deploymentService.computeBouncePlan(args) { true }
      if(bouncePlan)
      {
        bouncePlan.name = "Bounce ${params.title}"
        bouncePlans =
          deploymentService.groupByInstance(bouncePlan, [type: 'bounce', name: args.name])
        session.delta.addAll(bouncePlans)
      }

      def redeployPlans

      def redeployPlan = deploymentService.computeRedeployPlan(args) { true }
      if(redeployPlan)
      {
        redeployPlan.name = "Redeploy ${params.title}"
        redeployPlans =
          deploymentService.groupByInstance(redeployPlan, [type: 'redeploy', name: args.name])
        session.delta.addAll(redeployPlans)
      }

      [
          delta: plans,
          bounce: bouncePlans,
          redeploy: redeployPlans,
          missingAgents: missingAgents
      ]
    }
  }

  /**
   * List/select the values for a filter.
   */
  def filter_values = {
    def system = DbSystemModel.findCurrent(request.fabric)?.systemModel
    if(system)
    {
      if(params.value)
      {
        if(params.value == '*')
        {
          request."${params.id}" = null
          flash.message = "Selected All [${params.id}]"
        }
        else
        {
          def selection = system.metadata[params.id]?.getAt(params.value)
          if(selection?.name)
          {
            request."${params.id}" = selection
            flash.message = "Selected ${params.id}=${params.value}"
          }
          else
          {
            flash.error = "No such value ${params.value} for ${params.id}"
          }
        }
      }

      [
          currentValue: request."${params.id}"?.name,
          system: system,
          values: system.metadata[params.id]
      ]
    }
  }


  /**
   * Handle GET /system/live (live system as opposed to model)
   */
  def rest_get_live_system = {
    def model = agentsService.getCurrentSystemModel(request.fabric)
    model = model.filterBy(request.system.filters)

    if(params.prettyPrint)
      model = model.toString()
    else
      model = JSONSystemModelSerializer.INSTANCE.serialize(model)

    response.setContentType('text/json')
    render model

  }
}
