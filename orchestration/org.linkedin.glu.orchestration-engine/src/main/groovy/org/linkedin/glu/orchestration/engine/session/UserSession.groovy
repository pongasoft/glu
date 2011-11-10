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

package org.linkedin.glu.orchestration.engine.session

import org.linkedin.glu.provisioner.core.model.SystemFilter
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinition

/**
 * @author yan@pongasoft.com */
interface UserSession
{
  String getUsername()

  CustomDeltaDefinition getCustomDeltaDefinition()
  String getCurrentCustomDeltaDefinitionName()
  Collection<String> getCustomDeltaDefinitionNames()

  boolean isCustomDeltaDefinitionDirty()

  void resetCustomDeltaDefinition()

  void setGroupBy(String groupBy)
  String getGroupBy()

  /**
   * Processes the custom filter this way assuming <code>F</code> is a parseable filter
   *
   * <code>~</code> means {@link #resetCustomFilter()}
   * <code>-</code> means {@link #clearCustomFilter()}
   * <code>+F</code> means {@link #addCustomFilter(SystemFilter)}
   * <code>-F</code> means {@link #removeCustomFilter(SystemFilter)}
   * <code>~F</code> means {@link #resetAndAddCustomFilter(SystemFilter)}
   * <code>F</code> means {@link #clearAndSetCustomFilter(SystemFilter)}
   */
  void setCustomFilter(String customFilter)

  // custom filter manipulation
  void addCustomFilter(SystemFilter customFilter)
  void removeCustomFilter(SystemFilter customFilter)
  void resetAndAddCustomFilter(SystemFilter customFilter)
  void clearAndSetCustomFilter(SystemFilter customFilter)
  void resetCustomFilter()
  void clearCustomFilter()

  SystemFilter getCustomFilter()

  String getCustomFilterDisplayName()

  String getFabric()
  void setFabric(String fabric)
}