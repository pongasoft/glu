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
import org.linkedin.glu.provisioner.core.model.PropertySystemFilter
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaColumnDefinition
import org.linkedin.glu.provisioner.core.model.LogicSystemFilterChain
import org.linkedin.glu.provisioner.core.model.SystemFilterHelper

/**
 * @author yan@pongasoft.com */
class UserSessionImpl implements UserSession
{
  UserCustomDeltaDefinition current
  UserCustomDeltaDefinition original
  Collection<String> customDeltaDefinitionNames
  String fabric

  @Override
  CustomDeltaDefinition getCustomDeltaDefinition()
  {
    return current.customDeltaDefinition
  }

  @Override
  String getCurrentCustomDeltaDefinitionName()
  {
    return customDeltaDefinition.name
  }

  @Override
  Collection<String> getCustomDeltaDefinitionNames()
  {
    return customDeltaDefinitionNames
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
   * <code>~</code> means {@link #resetCustomFilter()}
   * <code>-</code> means {@link #clearCustomFilter()}
   * <code>+F</code> means {@link #addCustomFilter(SystemFilter)}
   * <code>-F</code> means {@link #removeCustomFilter(SystemFilter)}
   * <code>~F</code> means {@link #resetAndAddCustomFilter(SystemFilter)}
   * <code>F</code> means {@link #clearAndSetCustomFilter(SystemFilter)}
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
      return
    }

    if(filter == '~')
    {
      resetCustomFilter()
      return
    }

    if(filter.size() < 2)
      throw new IllegalArgumentException("invalid filter ${filter}")

    switch(filter[0])
    {
      case '+':
        filter = filter[1..-1]
        addCustomFilter(SystemFilterBuilder.parse(filter))
        break

      case '-':
        filter = filter[1..-1]
        removeCustomFilter(SystemFilterBuilder.parse(filter))
        break

      case '~':
        resetCustomFilter()
        filter = filter[1..-1]
        addCustomFilter(SystemFilterBuilder.parse(filter))
        break

      default:
        clearCustomFilter()
        addCustomFilter(SystemFilterBuilder.parse(filter))
        break
    }
  }

  void addCustomFilter(SystemFilter customFilter)
  {
    customDeltaDefinition.customFilter =
      SystemFilterHelper.and(customDeltaDefinition.customFilter, customFilter)
  }

  @Override
  void removeCustomFilter(SystemFilter customFilter)
  {
    customDeltaDefinition.customFilter =
      SystemFilterHelper.unand(customDeltaDefinition.customFilter, customFilter)
  }

  void resetAndAddCustomFilter(SystemFilter customFilter)
  {
    customDeltaDefinition.customFilter =
      SystemFilterHelper.and(original.customDeltaDefinition.customFilter, customFilter)
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

  @Override
  String getCustomFilterDisplayName()
  {
    SystemFilter filter = customFilter

    if(filter == null)
      return "Fabric [${fabric}]".toString()

    return computeFilterDisplayName(filter, 0)
  }

  /**
   * Internal recursive call to build the display name
   */
  private String computeFilterDisplayName(SystemFilter filter, int level)
  {
    switch(filter)
    {
      case PropertySystemFilter:
        def name = customDeltaDefinition.columnsDefinition.find {
          it.source == filter.name
        }?.name ?: filter.name
        return "${name} [${filter.value}]".toString()

      case LogicSystemFilterChain:
        def separator = (filter.kind == 'and' ? '-' : filter.kind)
        def res = filter.filters.collect { computeFilterDisplayName(it, level + 1) }.join (" ${separator} ")
        if(level > 0)
          res = "(${res})"
        return res.toString()

      default:
        return filter.toString()
    }
  }
}