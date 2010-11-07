/*
 * Copyright 2010-2010 LinkedIn, Inc
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

import org.linkedin.glu.console.domain.RoleName
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.linkedin.glu.grails.utils.ConsoleHelper
import org.linkedin.glu.console.domain.DbSystemModel
import org.linkedin.glu.console.services.FabricService
import org.linkedin.glu.grails.utils.ConsoleConfig
import javax.servlet.http.HttpServletResponse

/**
 * @author ypujante@linkedin.com */
class AuthFilters
{
  FabricService fabricService
  ConsoleConfig consoleConfig

  def onUnauthorized(subject, filter)
  {
    if(filter.name == 'rest')
      filter.response.sendError HttpServletResponse.SC_UNAUTHORIZED
    else
      filter.redirect(controller: "auth", action: "unauthorized")
  }

  static filters = {

    userInRequest(controller: '*', action: '*') {
      before = {
        def subject = SecurityUtils.getSubject()
        if(subject?.principal)
          request.user = [username: subject.principal]
      }
    }

    release(uri: '/release/**') {
      before = {
        accessControl {
          role(RoleName.RELEASE)
        }
      }
    }

    admin(uri: '/admin/**') {
      before = {
        accessControl {
          role(RoleName.ADMIN)
        }
      }
    }

    rest(uri: '/rest/**') {
      before = {
        def authString = request.getHeader('Authorization')

        if(authString)
        {
          try
          {
            def encodedPair = authString - 'Basic '
            // for some reason I do not understand encodedPair.decodeAsBase64() is not working...
            def decodedPair =  new String(encodedPair.decodeBase64(), 'UTF-8')
            def credentials = decodedPair.split(':')
            SecurityUtils.subject.login(new UsernamePasswordToken(credentials[0],
                                                                  credentials[1]))
          }
          catch(Exception e)
          {
            log.warn "Authorization failure: ${e.message}"
            if(log.isDebugEnabled())
              log.debug('Authorization failure', e)
            response.sendError HttpServletResponse.SC_UNAUTHORIZED
            return false
          }
        }

        // YP Note: for now all gets are available to USERS, and everything else to ADMIN only
        accessControl {
          if(request.method == 'GET' || request.method == 'HEAD')
            role(RoleName.USER)
          else
            role(RoleName.ADMIN)
        }
      }
    }

    all(controller: '*', action: '*') {
      before = {
        def authorized = accessControl {
          role(RoleName.USER)
        }

        if(!authorized)
          return false

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

        if(fabric)
        {
          def system = DbSystemModel.findCurrent(fabric)?.systemModel

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

            def filter = ConsoleHelper.getRequestValue(params, request, 'systemFilter')

            if(filter)
              system = system.filterBy(filter)

            request.system = system
          }
        }
      }

      after = {
        ['fabric', *consoleConfig.defaults.model.name].each {
          ConsoleHelper.saveCookie(response, it, request[it]?.name)
        }
      }
    }
  }

}
