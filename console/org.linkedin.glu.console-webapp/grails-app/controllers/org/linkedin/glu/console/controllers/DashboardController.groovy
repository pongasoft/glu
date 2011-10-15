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
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinition
import org.linkedin.glu.orchestration.engine.delta.CustomGroupByDelta
import org.linkedin.glu.agent.tracker.AgentsTracker.AccuracyLevel
import org.linkedin.groovy.util.json.JsonUtils

/**
 * @author ypujante@linkedin.com
 */
class DashboardController extends ControllerBase
{
  DeltaService deltaService
  ConsoleConfig consoleConfig

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
    Map rawDelta = deltaService.computeRawDelta(request.system).delta.flatten(new TreeMap())

    def sources = [] as TreeSet

    rawDelta.values().each {
      sources.addAll(it.keySet())
    }

    [
      sources: sources
    ]
  }

  /*
          mountPoint: [checked: true, name: 'mountPoint', groupBy: true, linkFilter: true],
          agent: [checked: true, name: 'agent', groupBy: true],
          'tag': [checked: false, name: 'tag', groupBy: true, linkFilter: true],
          'tags': [checked: true, name: 'tags', linkFilter: true],
          'metadata.container.name': [checked: true, name: 'container', groupBy: true, linkFilter: true],
          'metadata.version': [checked: true, name: 'version', groupBy: true],
          'metadata.product': [checked: true, name: 'product', groupBy: true, linkFilter: true],
          'metadata.cluster': [checked: true, name: 'cluster', groupBy: true, linkFilter: true],
          'initParameters.skeleton': [checked: false, name: 'skeleton', groupBy: true],
          script: [checked: false, name: 'script', groupBy: true],
          'metadata.modifiedTime': [checked: false, name: 'Last Modified', groupBy: false],
          status: [checked: true, name: 'status', groupBy: true]

   */
  private def doComputeDelta()
  {
//    def cdd = [
//      name: 'dashboard-1',
//      errorsOnly: false,
//      summary: true,
//      columnsDefinition: [
//        [
//          name: 'mountPoint-name',
//          source: 'mountPoint',
//        ],
//        [
//          name: 'container',
//          source: 'metadata.container.name',
//        ],
//      ]
//    ]
//
//    CustomGroupByDelta groupByDelta =
//      deltaService.computeCustomGroupByDelta(request.system,
//                                             CustomDeltaDefinition.fromExternalRepresentation(cdd))
//
//    println CustomDeltaDefinition.fromDashboard(consoleConfig.defaults.dashboard).toExternalRepresentation()

    CustomDeltaDefinition cdd =
      CustomDeltaDefinition.fromDashboard(consoleConfig.defaults.dashboard)

    cdd = cdd.groupBy(params.groupBy)

    CustomGroupByDelta groupByDelta =
      deltaService.computeCustomGroupByDelta(request.system,
                                             cdd)

    //groupByDelta.accuracy = AccuracyLevel.INACCURATE

    return groupByDelta
//    deltaService.computeGroupByDelta(request.system,
//                                     consoleConfig.defaults.dashboard,
//                                     params)
  }
}
