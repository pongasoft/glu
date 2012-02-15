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

/**
 * @author ypujante@linkedin.com */
class AuthFilters
{
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
            def credentials = decodedPair.split(':', 2)
            SecurityUtils.subject.login(new UsernamePasswordToken(credentials[0],
                                                                  credentials[1]))
            request.user = [username: SecurityUtils.subject.principal]
            request.isRestRequest = true
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
        accessControl {
          role(RoleName.USER)
        }
      }
    }
  }
}
