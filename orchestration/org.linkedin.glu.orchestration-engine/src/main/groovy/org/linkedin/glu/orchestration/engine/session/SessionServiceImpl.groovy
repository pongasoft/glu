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

package org.linkedin.glu.orchestration.engine.session

import net.sf.ehcache.Ehcache
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.orchestration.engine.authorization.AuthorizationService
import org.linkedin.glu.orchestration.engine.delta.DeltaService
import org.linkedin.glu.orchestration.engine.delta.UserCustomDeltaDefinition
import net.sf.ehcache.Element

/**
 * @author yan@pongasoft.com */
public class SessionServiceImpl implements SessionService
{
  @Initializable
  DeltaService deltaService

  @Initializable
  AuthorizationService authorizationService

  @Initializable
  Ehcache userSessionCache

  void clearUserSession()
  {
    String username = authorizationService.executingPrincipal
    userSessionCache.remove(username)
  }

  @Override
  UserSession findUserSession(String defaultName)
  {
    String username = authorizationService.executingPrincipal

    UserSession res =
      userSessionCache.get(username)?.objectValue

    if(!res)
    {
      res = toSUCDD(deltaService.findDefaultUserCustomDeltaDefinition(defaultName))
    }

    userSessionCache.put(new Element(username, res))

    return res
  }

  private UserSession toSUCDD(UserCustomDeltaDefinition ucdd)
  {
    if(ucdd == null)
      return null
    new UserSessionImpl(original: ucdd.clone(),
                        current: ucdd.clone())
  }

}