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
import org.linkedin.glu.agent.tracker.AgentsTracker.AccuracyLevel

/**
 * @author yan@pongasoft.com */
public class CustomGroupByDelta
{
  SystemModel expectedModel
  CustomDeltaDefinition deltaDefinition
  Map<String, Object> groupByDelta
  AccuracyLevel accuracy

  /**
   * key is column name, value is the number of unique values for this column in the visible rows
   * (rows that are excluded are the ones where the first column is undefined or the ones not
   * in error if <code>errorsOnly</code> has been specified)
   */
  Map<String, Integer> counts

  /**
   * key is column name, value is the number of unique values for this column in all rows (whether
   * visible or not)
   */
  Map<String, Integer> totals

  CustomDeltaColumnDefinition getFirstColumn()
  {
    return deltaDefinition.firstColumn
  }

  Collection<CustomDeltaColumnDefinition> getTailColumns()
  {
    return deltaDefinition.tailColumns
  }
}
