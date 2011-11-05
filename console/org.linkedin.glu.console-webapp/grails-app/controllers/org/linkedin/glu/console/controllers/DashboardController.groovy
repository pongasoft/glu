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

import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.glu.orchestration.engine.delta.DeltaService

import org.linkedin.glu.orchestration.engine.delta.CustomGroupByDelta

import org.linkedin.glu.orchestration.engine.session.UserSession
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.glu.orchestration.engine.system.SystemService
import org.linkedin.glu.orchestration.engine.delta.UserCustomDeltaDefinition
import org.linkedin.glu.orchestration.engine.session.SessionService
import org.linkedin.glu.console.filters.UserPreferencesFilters
import org.json.JSONException
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinition

/**
 * @author ypujante@linkedin.com
 */
class DashboardController extends ControllerBase
{
  DeltaService deltaService
  ConsoleConfig consoleConfig
  SystemService systemService
  SessionService sessionService

  def beforeInterceptor = {
    // we make sure that the fabric is always set before executing any action
    return ensureCurrentFabric()
  }

  /**
   * Redirect to computeDelta
   */
  def index = {
    redirect(action: 'delta')
  }

  /**
   * Renders only the portion below the menus
   */
  def renderDelta = {
    render(template: '/dashboard/delta', model: [delta: doComputeDelta()])
  }

  /**
   * Delta the live system: display condensed view of all apps accross all agents
   */
  def delta = {
    return [delta: doComputeDelta()]
  }

  /**
   * Called in order to customize the dashboard
   */
  def customize = {
    def name = request.userSession.currentCustomDeltaDefinitionName
    UserCustomDeltaDefinition ucdd = deltaService.findUserCustomDeltaDefinitionByName(name)

    String prettyPrintedContent = ucdd.prettyPrintedContent

    if(params.update)
    {
      try
      {
        prettyPrintedContent = params.content
        ucdd.updateContent(params.content)
        if(!deltaService.saveUserCustomDeltaDefinition(ucdd))
        {
          flash.error = "Problem while saving your changes"
        }
        else
        {
          flash.success = "Your changes to your custom dashboard have been saved successfully."
          sessionService.clearUserSession()
          def redirectParams = [:]
          redirectParams[UserPreferencesFilters.CUSTOM_DELTA_DEFINITION_COOKIE_NAME] = ucdd.name
          redirect(action: 'customize', params: redirectParams)
          return
        }
      }
      catch(IllegalArgumentException e)
      {
        flash.error = "Error while saving your custom dashboard: ${e.message}"
      }
      catch(JSONException e)
      {
        flash.error = "Error while saving your custom dashboard: ${e.message}"
      }
      catch(Exception e)
      {
        flashException("Error while saving your custom dashboard: ${e.message}", e)
      }
    }

    if(params.delete)
    {
      deltaService.deleteUserCustomDeltaDefinition(ucdd)
      flash.success = "Your custom dashboard ${ucdd.name} has been deleted."
      if(deltaService.findAllUserCustomDeltaDefinition(false, [:]).count == 0)
      {
        flash['info'] = "A new default custom dashboard has been created..."
      }
      sessionService.clearUserSession()
      redirect(action: 'customize')
      return
    }

    Map rawDelta = deltaService.computeRawDelta(request.system).delta.flatten(new TreeMap())
    def sources = [] as TreeSet
    rawDelta.values().each {
      sources.addAll(it.keySet())
    }

    [
      prettyPrintedContent: prettyPrintedContent,
      ucdd: ucdd,
      sources: sources
    ]
  }

  /**
   * Called to save the current dashboard as a new one...
   */
  def saveAsNewCustomDashboard = {
    def redirectParams = [:]

    def name = request.userSession.currentCustomDeltaDefinitionName
    UserCustomDeltaDefinition ucdd = deltaService.findUserCustomDeltaDefinitionByName(name).clone()
    def definition = request.userSession.customDeltaDefinition.clone()
    definition.name = params.name
    ucdd.customDeltaDefinition = definition

    ucdd = deltaService.saveAsNewUserCustomDeltaDefinition(ucdd)

    if(ucdd.hasErrors())
    {
      flash.error = "Duplicate name ${params.name}"
    }
    else
    {
      sessionService.clearUserSession()
      redirectParams[UserPreferencesFilters.CUSTOM_DELTA_DEFINITION_COOKIE_NAME] = params.name
    }

    redirect(action: 'delta', params: redirectParams)
  }

  /**
   * Called for viewing the plans
   */
  def plans = {
    CustomGroupByDelta groupByDelta = doComputeDelta()

    def missingAgents = systemService.getMissingAgents(request.fabric, request.system)

    [
      title: 'tbd',
      hasDelta: groupByDelta.counts['errors'] > 0,
      missingAgents: missingAgents
    ]
  }

  private def doComputeDelta()
  {
    CustomGroupByDelta groupByDelta =
      deltaService.computeCustomGroupByDelta(request.system,
                                             request.userSession.customDeltaDefinition)
    return groupByDelta
  }
}
