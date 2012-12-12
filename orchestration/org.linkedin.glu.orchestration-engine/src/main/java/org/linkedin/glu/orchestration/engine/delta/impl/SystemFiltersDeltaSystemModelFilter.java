/*
 * Copyright (c) 2012 Yan Pujante
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

import org.linkedin.glu.orchestration.engine.delta.DeltaSystemModelFilter;
import org.linkedin.glu.provisioner.core.model.SystemEntry;
import org.linkedin.glu.provisioner.core.model.SystemFilter;

/**
 * Filter by only the states provided.
 * 
 * @author yan@pongasoft.com
 */
public class SystemFiltersDeltaSystemModelFilter implements DeltaSystemModelFilter
{
  private final SystemFilter _expectedEntrySystemFilter;
  private final SystemFilter _currentEntrySystemFilter;

  /**
   * Constructor
   */
  public SystemFiltersDeltaSystemModelFilter(SystemFilter expectedEntrySystemFilter,
                                             SystemFilter currentEntrySystemFilter)
  {
    _expectedEntrySystemFilter = expectedEntrySystemFilter;
    _currentEntrySystemFilter = currentEntrySystemFilter;
  }

  @Override
  public boolean filter(SystemEntry expectedEntry, SystemEntry currentEntry)
  {
    return (_currentEntrySystemFilter == null  || _currentEntrySystemFilter.filter(currentEntry)) &&
           (_expectedEntrySystemFilter == null || _expectedEntrySystemFilter.filter(expectedEntry));
  }
}
