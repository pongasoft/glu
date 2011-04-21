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

/**
 * @author yan@pongasoft.com  */
public interface AuthorizationService
{
  /**
   * @throws AccessControlException if the current user does not have the rights to stream the
   * content of the file
   */
  void checkStreamFileContent(String location) throws AccessControlException

  /**
   * @return the executing principal (who is executing the code)
   */
  String getExecutingPrincipal()
}