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

import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.utils.security.ExpectPrincipal

interface DeltaService
{
  /**
   * @params.expectedModel expected model
   * @params.currentModel current model (<code>null</code> means compute it)
   * @params.errorsOnly filter to show only errors (<code>true/false</code>)
   * @params.prettyPrint if json should be pretty printed (<code>true/false</code>)
   * @params.flatten flatten the model (<code>true/false</code>)
   * @return json representation of the delta
   */
  String computeDeltaAsJSON(params)

  /**
   * Computes the delta between the expected model and the current one (which will be computed)
   * In this version the delta is a set
   * @return a map with 2 entries: <code>accuracy</code> and <code>delta</code>.
   */
  Map computeDelta(SystemModel expectedModel)

  /**
   * Computes the raw delta between the expected model and the current one (which will be computed)
   * @return a map with 2 entries <code>[accuracy: accuracy, delta: SystemModelDelta]</code>
   */
  Map computeRawDelta(SystemModel expectedModel)

  /**
   * This method used to be in <code>DashboardController</code> and as been moved here to
   * be able to share/test more easily
   *
   * @param groupByDefinition TODO MED YP (in the meantime look at consoleConfig.defaults.dashboard)
   * @param groupBySelection TODO MED YP (params from the request)
   * @return the delta grouped by according to the definition and selection
   */
  Map computeGroupByDelta(SystemModel expectedSystem,
                          Map groupByDefinition,
                          Map groupBySelection)

  /**
   * Compute the custom "group by" delta using the custom delta definition provided
   */
  CustomGroupByDelta computeCustomGroupByDelta(SystemModel expectedModel,
                                               CustomDeltaDefinition deltaDefinition)

  /**
   * Compute the custom "group by" delta using the custom delta definition provided
   */
  CustomGroupByDelta computeCustomGroupByDelta(SystemModel expectedModel,
                                               UserCustomDeltaDefinition userDeltaDefinition)

  /**
   * Save the details
   */
  @ExpectPrincipal
  boolean saveUserCustomDeltaDefinition(UserCustomDeltaDefinition definition)

  /**
   * Initializes the default user custome delta definition
   */
  boolean saveDefaultCustomDeltaDefinition(CustomDeltaDefinition definition)

  /**
   * Return the default value
   */
  UserCustomDeltaDefinition findDefaultCustomDeltaDefinition()

  /**
   * Return the default configuration for the user with the given name but if not found will
   * create a default one based on {@link #findDefaultCustomDeltaDefinition()}
   */
  @ExpectPrincipal
  UserCustomDeltaDefinition findDefaultUserCustomDeltaDefinition(String defaultName)

  /**
   * Return the unique entry for a given username by name
   *
   * @return <code>null</code> if no such entry
   */
  @ExpectPrincipal
  UserCustomDeltaDefinition findUserCustomDeltaDefinitionByName(String name)

  /**
   * Return all the entries for the 'current' user
   *
   * params can be what grails accept for paginating queries: <code>max</code>,
   * <code>offset</code>, <code>sort</code>, <code>order</code>
   * @return a map with list: the list of {@link UserCustomDeltaDefinition} or
   *         {@link LightUserCustomDeltaDefinition} and
   *         count: the total number of entries
   */
  @ExpectPrincipal
  Map findAllUserCustomDeltaDefinition(boolean includeDetails,
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
  Map findAllUserCustomDeltaDefinitionShareable(boolean includeDetails,
                                                params)
}
