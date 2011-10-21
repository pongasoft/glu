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

package org.linkedin.glu.orchestration.engine.delta

/**
 * @author yan@pongasoft.com */
public interface CustomDeltaDefinitionStorage
{
  /**
   * Save the details
   */
  boolean save(UserCustomDeltaDefinition definition)

  /**
   * Return the unique entry for a given username by name
   * 
   * @return <code>null</code> if no such entry
   */
  UserCustomDeltaDefinition findByUsernameAndName(String username, String name)

  /**
   * Return all the entries for a given user
   *
   * params can be what grails accept for paginating queries: <code>max</code>,
   * <code>offset</code>, <code>sort</code>, <code>order</code>
   * @return a map with list: the list of {@link UserCustomDeltaDefinition} or
   *         {@link LightUserCustomDeltaDefinition} and
   *         count: the total number of entries
   */
  Map findAllByUsername(String username,
                        boolean includeDetails,
                        params)

  /**
   * Return only the shareable entries
   *
   * params can be what grails accept for paginating queries: <code>max</code>,
   * <code>offset</code>, <code>sort</code>, <code>order</code>
   * @return a map with list: the list of {@link UserCustomDeltaDefinition} or
   *         {@link LightUserCustomDeltaDefinition} and
   *         count: the total number of entries
   */
  Map findAllShareable(boolean includeDetails,
                       params)

}