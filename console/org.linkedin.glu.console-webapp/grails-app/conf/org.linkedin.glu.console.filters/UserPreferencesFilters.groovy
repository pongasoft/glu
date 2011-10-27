/*
 * Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.console.filters

import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.glu.grails.utils.ConsoleHelper
import org.linkedin.glu.orchestration.engine.fabric.FabricService
import org.linkedin.glu.orchestration.engine.system.SystemService
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.fabric.Fabric

import org.linkedin.glu.orchestration.engine.session.UserSession
import org.linkedin.glu.orchestration.engine.session.SessionService

/**
 * @author yan@pongasoft.com */
class UserPreferencesFilters
{
  public static final String CUSTOM_DELTA_DEFINITION_COOKIE_NAME = "ucdd"

  SystemService systemService
  FabricService fabricService
  ConsoleConfig consoleConfig
  SessionService sessionService

  def dependsOn = [AuthFilters]

  static filters = {
    all(controller: '*', action: '*') {
      before = {

        if(!request.user)
        {
          return
        }

        // Initializes the fabric
        Fabric fabric = initFabric(params, request)

        // Initializes the custom delta definition
        UserSession userSession =
          initUserSession(params, request)

        // Initializes the system
        initSystemModel(fabric, userSession, params, flash, request)

        return true
      }

      after = {
        if(request.user)
        {
          ['fabric', *consoleConfig.defaults.model.name].each {
            ConsoleHelper.saveCookie(response, it, request[it]?.name)
          }

          // saving the name in the cookie
          if(request.sessionUserCustomDeltaDefinition)
            ConsoleHelper.saveCookie(response,
                                     CUSTOM_DELTA_DEFINITION_COOKIE_NAME,
                                     request.sessionUserCustomDeltaDefinition.name)
        }
      }
    }
  }

  /**
   * look for the fabric in request and or cookies
   */
  Fabric initFabric(params, request)
  {
    // 1) request first
    def fabric = ConsoleHelper.getRequestValue(params, request, 'fabric')

    if(fabric)
    {
      fabric = fabricService.findFabric(fabric)
    }

    // 2) last resort (when only 1 fabric, simply select it...)
    if(!fabric)
    {
      def fabricNames = fabricService.listFabricNames()
      if(fabricNames.size() == 1)
      {
        fabric = fabricService.findFabric(fabricNames[0])
      }
    }

    // save the fabric in the request
    request.fabric = fabric

    return fabric
  }

  /**
   * Initializes the current system: it will be filtered automatically
   */
  SystemModel initSystemModel(Fabric fabric,
                              UserSession userSession,
                              params,
                              flash,
                              request)
  {
    if(fabric)
    {
      SystemModel system = systemService.findCurrentSystem(fabric.name)

      if(system)
      {
        system = system.clone()

        consoleConfig.defaults.model.each { info ->
          def value = ConsoleHelper.getRequestValue(params, flash, request, info.name)
          if(value)
          {
            value = system.metadata[info.name]?.getAt(value)
            if(value)
            {
              system = system.filterByMetadata(info.name, value.name)
              request."${info.name}" = value
            }
          }
        }

        system = system.filterBy(userSession?.customFilter)

        // save the system in the request
        request.system = system
      }

      return system
    }
    else
    {
      return null
    }
  }

  /**
   * Initializes user session
   */
  UserSession initUserSession(params, request)
  {
    String cddName =
      ConsoleHelper.getRequestValue(params, request, CUSTOM_DELTA_DEFINITION_COOKIE_NAME)

    UserSession userSession  =
      sessionService.findUserSession(cddName)

    String filter = ConsoleHelper.getRequestValue(params, request, 'systemFilter')

    userSession.setCustomFilter(filter)

    println "request filter = ${filter} => ${userSession.customFilter?.toDSL()}"

    request.userSession = userSession

    return userSession
  }
}
