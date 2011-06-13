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
}
