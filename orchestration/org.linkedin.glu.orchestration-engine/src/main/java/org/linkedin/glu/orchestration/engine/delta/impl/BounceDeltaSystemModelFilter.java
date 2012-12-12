/*
 * Copyright (c) 2011-2012 Yan Pujante
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

package org.linkedin.glu.orchestration.engine.delta.impl;

import org.linkedin.glu.provisioner.core.model.LogicSystemFilterChain;
import org.linkedin.glu.provisioner.core.model.SystemEntryStateSystemFilter;
import org.linkedin.glu.provisioner.core.model.SystemFilter;

/**
 * For bounce we take all the entries where the current state is running or stopped
 * and the expected state is 'running'.
 * 
 * @author yan@pongasoft.com
 */
public class BounceDeltaSystemModelFilter extends SystemFiltersDeltaSystemModelFilter
{
  /**
   * Constructor
   */
  public BounceDeltaSystemModelFilter(SystemFilter expectedSystemFilter)
  {
    this(expectedSystemFilter, "running", "stopped");
  }

  /**
   * Constructor
   */
  public BounceDeltaSystemModelFilter(SystemFilter expectedSystemFilter,
                                      String expectedState,
                                      String bounceState)
  {
    super(LogicSystemFilterChain.and(expectedSystemFilter,
                                     SystemEntryStateSystemFilter.create(expectedState)),
          SystemEntryStateSystemFilter.create(expectedState, bounceState));
  }
}
