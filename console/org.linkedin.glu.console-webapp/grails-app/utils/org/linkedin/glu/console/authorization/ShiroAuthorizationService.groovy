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

package org.linkedin.glu.console.authorization

import org.linkedin.glu.orchestration.engine.authorization.AuthorizationService
import org.linkedin.util.annotations.Initializable
import org.apache.shiro.SecurityUtils
import org.linkedin.glu.console.domain.RoleName
import org.apache.shiro.authz.AuthorizationException
import java.security.AccessControlException
import org.apache.shiro.UnavailableSecurityManagerException

/**
 * @author yan@pongasoft.com */
public class ShiroAuthorizationService implements AuthorizationService
{
  @Initializable
  String unrestrictedLocation

  @Override
  void checkStreamFileContent(String location)
  {
    // do not allow non admin users to access files:
    // -- outside of <nonAdminRootLocation> area
    if(unrestrictedLocation && !location.startsWith(unrestrictedLocation))
    {
      try
      {
        SecurityUtils.getSubject().checkRole(RoleName.ADMIN.name())
      }
      catch (AuthorizationException e)
      {
        AccessControlException re = new AccessControlException(location)
        re.initCause(e)
        throw re
      }
    }
  }

  @Override
  String getExecutingPrincipal()
  {
    try
    {
      return SecurityUtils.getSubject()?.principal?.toString()
    }
    catch(UnavailableSecurityManagerException e)
    {
      return null
    }
  }
}