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
import org.linkedin.glu.orchestration.engine.deployment.DeploymentService
import org.linkedin.glu.console.domain.DbSystemModel
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.orchestration.engine.system.SystemService
import org.linkedin.glu.orchestration.engine.planner.PlannerService
import org.linkedin.glu.orchestration.engine.delta.DeltaService

class SystemController extends ControllerBase
{
  AgentsService agentsService
  DeploymentService deploymentService
  PlannerService plannerService
  SystemService systemService
  DeltaService deltaService

  def beforeInterceptor = {
    // we make sure that the fabric is always set before executing any action
    return ensureCurrentFabric()
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

      def missingAgents = systemService.getMissingAgents(request.fabric, request.system)

      [
        title: title,
        hasDelta: deltaService.computeRawDelta(request.system).delta?.hasErrorDelta(),
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
      def missingAgents = systemService.getMissingAgents(request.fabric, request.system)

      [
        title: params.title,
        filter: params.systemFilter,
        hasDelta: deltaService.computeRawDelta(request.system).delta?.hasErrorDelta(),
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
}
