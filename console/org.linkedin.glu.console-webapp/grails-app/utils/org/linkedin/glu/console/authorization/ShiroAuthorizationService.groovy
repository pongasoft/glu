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
import org.apache.shiro.SecurityUtils
import org.apache.shiro.UnavailableSecurityManagerException
import java.security.AccessControlException
import org.apache.shiro.authz.AuthorizationException
import java.security.Permission
import org.apache.shiro.subject.Subject

/**
 * @author yan@pongasoft.com */
public class ShiroAuthorizationService implements AuthorizationService
{
  @Override
  void checkRole(String role, String message, Permission permission)
  {
    try
    {
      Subject subject = SecurityUtils.getSubject()

      if(subject == null)
        throw new AccessControlException(message, permission)
      
      subject.checkRole(role)
    }
    catch (AuthorizationException e)
    {
      AccessControlException re = new AccessControlException(message, permission)
      re.initCause(e)
      throw re
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