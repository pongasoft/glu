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

package org.linkedin.glu.console.filters

import javax.servlet.http.HttpServletResponse
import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.UsernamePasswordToken
import org.linkedin.glu.console.domain.RoleName
import org.slf4j.LoggerFactory
import org.slf4j.Logger
import org.linkedin.glu.groovy.utils.plugins.PluginService
import org.linkedin.glu.orchestration.engine.user.UserService

/**
 * @author ypujante@linkedin.com */
class AuthFilters
{
  public static final String MODULE = AuthFilters.class.getName()
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  PluginService pluginService

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
        return true
      }
    }

    rest(uri: '/rest/**') {
      def theFilter = delegate
      before = {
        request.isRestRequest = true

        pluginService.executePrePostMethods(UserService,
                                            "restAuthenticateAndAuthorize",
                                            [filter: theFilter]) { args ->

          if(args.pluginResult != null)
            return args.pluginResult
          
          def authString = request.getHeader('Authorization')

          if(authString)
          {
            try
            {
              def encodedPair = authString - 'Basic '
              // for some reason I do not understand encodedPair.decodeAsBase64() is not working...
              def decodedPair =  new String(encodedPair.decodeBase64(), 'UTF-8')
              def credentials = decodedPair.split(':', 2)
              SecurityUtils.subject.login(new UsernamePasswordToken(credentials[0],
                                                                    credentials[1]))
              request.user = [username: SecurityUtils.subject.principal]
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
          else
          {
            if(log.isDebugEnabled())
              log.debug('No Authorization header')
            response.sendError HttpServletResponse.SC_UNAUTHORIZED
            return false
          }

          // YP Implementation note: the accessControl closure is not even called if there is no
          // subject hence the "else" section above otherwise a 302 is issued (fixes glu-140)

          pluginService.executePrePostMethods(UserService,
                                              "authorize",
                                              [filter: theFilter]) { args2 ->
            if(args2.pluginResult != null)
              return args2.pluginResult

            accessControl {
              def minRoleToProceed = params.__roles[request.method]

              if(!minRoleToProceed)
              {
                minRoleToProceed = RoleName.ADMIN
              }

              role(minRoleToProceed)
            }
          }
        }
      }
    }

    ui(uri: "/**", uriExclude: "/rest/**") {
      def theFilter = delegate
      before = {
        if(request.isRestRequest)
        {
          // YP note: <uriExclude: "/rest/**"> which is described in the grails doc, is not
          // working, hence this workaround :(
          return true
        }

        switch(params.controller)
        {
          case "auth":
            // no restriction => always allowed
            return true
            break

          default:
            pluginService.executePrePostMethods(UserService,
                                                "authorize",
                                                [filter: theFilter]) { args ->
              
              if(args.pluginResult != null)
                return args.pluginResult

              accessControl {
                def minRoleToProceed = params.__role

                if(!minRoleToProceed)
                {
                  minRoleToProceed = RoleName.ADMIN
                }

                role(minRoleToProceed)
              }
            }

            break
        }
      }
    }
  }
}