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
   * Computes the delta between the expected model and the current one (which will be computed)
   * In this version the delta is a set
   * @return a map with 2 entries: <code>accuracy</code> and <code>delta</code>.
   */
  Map computeDelta(SystemModel expectedModel)

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
