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

package org.linkedin.glu.orchestration.engine.delta;

import org.linkedin.glu.provisioner.core.model.SystemModel;

import java.util.Collection;

/**
 * @author yan@pongasoft.com
 */
public interface DeltaMgr
{
  /**
   * Computes the delta between the 2 models
   */
  SystemModelDelta computeDelta(SystemModel expectedModel, SystemModel currentModel);

  /**
   * Computes N deltas to go from state to state. If <code>state==null</code> it means empty
   * system, if <code>state==<expected></code> it means <code>expectedModel</code>.
   *
   * @return a collection of deltas (same size as <code>toStates</code>)
   */
  Collection<SystemModelDelta> computeDeltas(SystemModel expectedModel,
                                             SystemModel currentModel,
                                             Collection<String> toStates);
}