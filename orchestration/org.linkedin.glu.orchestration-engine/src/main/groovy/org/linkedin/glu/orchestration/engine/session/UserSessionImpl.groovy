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
import org.linkedin.glu.provisioner.core.model.SystemFilterBuilder
import org.linkedin.glu.orchestration.engine.delta.UserCustomDeltaDefinition
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinition

/**
 * @author yan@pongasoft.com */
class UserSessionImpl implements UserSession
{
  UserCustomDeltaDefinition current
  UserCustomDeltaDefinition original

  @Override
  CustomDeltaDefinition getCustomDeltaDefinition()
  {
    return current.customDeltaDefinition
  }

  @Override
  boolean isCustomDeltaDefinitionDirty()
  {
    return current != original
  }

  @Override
  void resetCustomDeltaDefinition()
  {
    current = original.clone()
  }

  @Override
  void setGroupBy(String groupBy)
  {
    if(groupBy)
    {
      def tmp = original.clone()
      tmp.customDeltaDefinition = tmp.customDeltaDefinition.groupBy(groupBy)
      current.customDeltaDefinition.columnsDefinition = tmp.customDeltaDefinition.columnsDefinition
    }
  }

  @Override
  String getGroupBy()
  {
    customDeltaDefinition.groupBy
  }

  @Override
  String getUsername()
  {
    return current.username
  }

  /**
   * Processes the custom filter this way assuming <code>F</code> is a parseable filter
   *
   * <code>+F</code> means {@link #addCustomFilter(SystemFilter)}
   * <code>~F</code> means {@link #resetAndAddCustomFilter(SystemFilter)}
   * <code>F</code> (or <code>-F</code>) means {@link #clearAndSetCustomFilter(SystemFilter)}
   * <code>-</code> means {@link #clearCustomFilter()}
   */
  @Override
  void setCustomFilter(String filter)
  {
    filter = filter?.trim()

    if(filter == null)
      return

    if(filter == '')
      filter = '-'

    if(filter == '-')
    {
      clearCustomFilter()
    }
    else
    {
      if(filter.size() < 2)
        throw new IllegalArgumentException("invalid filter ${filter}")

      switch(filter[0])
      {
        case '+':
          filter = filter[1..-1]
          break

        case '~':
          resetCustomFilter()
          filter = filter[1..-1]
          break

        case '-':
          clearCustomFilter()
          filter = filter[1..-1]
          break

        default:
          clearCustomFilter()
          break
      }

      addCustomFilter(SystemFilterBuilder.parse(filter))
    }
  }

  void addCustomFilter(SystemFilter customFilter)
  {
    customDeltaDefinition.customFilter =
      SystemFilterBuilder.and(customDeltaDefinition.customFilter, customFilter)
  }

  void resetAndAddCustomFilter(SystemFilter customFilter)
  {
    customDeltaDefinition.customFilter =
      SystemFilterBuilder.and(original.customDeltaDefinition.customFilter, customFilter)
  }

  void clearAndSetCustomFilter(SystemFilter customFilter)
  {
    customDeltaDefinition.customFilter = customFilter
  }

  @Override
  void resetCustomFilter()
  {
    customDeltaDefinition.customFilter = original.customDeltaDefinition.customFilter
  }

  @Override
  void clearCustomFilter()
  {
    customDeltaDefinition.customFilter = null
  }

  @Override
  SystemFilter getCustomFilter()
  {
    customDeltaDefinition.customFilter
  }
}