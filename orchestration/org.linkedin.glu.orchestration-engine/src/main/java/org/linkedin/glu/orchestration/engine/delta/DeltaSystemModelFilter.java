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

import org.linkedin.glu.provisioner.core.model.SystemEntry;

/**
 * Filter to filter out entries when doing a delta based on the 2 entries
 * @author yan@pongasoft.com
 */
public interface DeltaSystemModelFilter
{
  /**
   * @param expectedEntry the entry in the expected model (can be <code>null</code>!)
   * @param currentEntry the entry in the current model (can be <code>null</code>!)
   * @return <code>true</code> if the entry should be part of the filter, <code>false</code>
   *         otherwise
   */
  boolean filter(SystemEntry expectedEntry, SystemEntry currentEntry);
}