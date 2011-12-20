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

package org.linkedin.glu.orchestration.engine.authorization

import java.security.AccessControlException
import java.security.Permission

/**
 * @author yan@pongasoft.com  */
public interface AuthorizationService
{
  /**
   * Make sure that the current executing principal has the provided role
   *
   * @param message the (optional) message to provide to the exception
   * @param permission the (optional) permission to provide to the exception
   * @throws AccessControlException if it does not have the provided
   */
  void checkRole(String role, String message, Permission permission) throws AccessControlException

  /**
   * @return the executing principal (who is executing the code)
   */
  String getExecutingPrincipal()
}