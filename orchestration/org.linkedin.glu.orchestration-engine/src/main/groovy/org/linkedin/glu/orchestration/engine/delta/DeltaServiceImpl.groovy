/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.provisioner.core.model.TagsSystemFilter
import org.linkedin.glu.provisioner.core.model.SystemFilter
import org.linkedin.glu.provisioner.core.model.LogicSystemFilterChain
import org.linkedin.glu.orchestration.engine.fabric.FabricService

class DeltaServiceImpl implements DeltaService
{
  @Initializable
  AgentsService agentsService

  @Initializable
  FabricService fabricService

  @Initializable(required = true)
  DeltaMgr deltaMgr

  @Initializable
  boolean notRunningOverridesVersionMismatch = false

  Map computeDelta(SystemModel expectedModel)
  {
    if(!expectedModel)
      return null

    Fabric fabric = fabricService.findFabric(expectedModel.fabric)

    if(!fabric)
      throw new IllegalArgumentException("unknown fabric ${expectedModel.fabric}")

    SystemModel currentSystem = agentsService.getCurrentSystemModel(fabric)

    [
        delta: computeDelta(expectedModel, currentSystem),
        accuracy: currentSystem.metadata.accuracy
    ]
  }

  Collection<Map<String, Object>> computeDelta(SystemModel expectedModel,
                                               SystemModel currentModel)
  {
    SystemModelDelta delta = deltaMgr.computeDelta(expectedModel, currentModel)

    Collection<Map<String, Object>> flattenedDelta =
      delta.flatten(new TreeMap<String, Map<String, Object>>()).values()

    if(notRunningOverridesVersionMismatch)
    {
      flattenedDelta.each { Map m ->
        if(m.status == 'delta')
        {
          SystemEntryValueWithDelta<String> entryStateDelta =
            delta.findEntryDelta(m.key).findEntryStateDelta()
          if(entryStateDelta)
          {
            m.status = 'notExpectedState'
            m.statusInfo = "${entryStateDelta.expectedValue}!=${entryStateDelta.currentValue}"
          }
        }
      }
    }

    return flattenedDelta
  }

  Map computeGroupByDelta(SystemModel expectedSystem,
                          Map groupByDefinition,
                          Map groupBySelection)
  {
    def deltaWithAccuracy = computeDelta(expectedSystem)
    def current = deltaWithAccuracy.delta

    def columnNames = groupByDefinition.collect { k, v -> k }

    def groupByColumns = groupByDefinition.findAll { k, v -> v.groupBy }.collect { k,v -> k}

    groupByDefinition.each { k, v ->
      def checked = v.checked
      if(groupBySelection[k] == 'false')
      {
        checked = false
      }
      if(groupBySelection[k] == 'true')
      {
        checked = true
      }

      if(!checked)
        columnNames.remove(k)
    }

    def isSummaryFilter = !(groupBySelection['summary'] == 'false')
    def isErrorsFilter = groupBySelection['errors'] == 'true'

    if(groupBySelection.groupBy)
    {
      columnNames.remove(groupBySelection.groupBy)
      columnNames = [groupBySelection.groupBy, * columnNames]
    }

    boolean removeStatusInfoColumn = false
    if(columnNames.contains("status") && !columnNames.contains("statusInfo"))
    {
      removeStatusInfoColumn = true
      columnNames << "statusInfo"
    }
    
    def column0Name = columnNames[0]

    def allTags = new HashSet()

    def counts = [errors: 0, instances: 0]
    def totals = [errors: 0, instances: 0]
    groupByColumns.each { column ->
      counts[column] = new HashSet()
      totals[column] = new HashSet()
    }

    if(column0Name == 'tag')
    {
      Set<String> filteredTags = extractFilteredTags(expectedSystem) as Set

      def newCurrent = []
      current.each { row ->
        if(row.tags?.size() > 0)
        {
          row.tags.each { tag ->
            if(!filteredTags || filteredTags.contains(tag))
            {
              def newRow = [*:row]
              newRow.tag = tag
              newCurrent << newRow
            }
          }
        }
        else
          newCurrent << row
      }
      current = newCurrent
    }

    current.each { row ->
      def column0Value = row[column0Name]
      groupByColumns.each { column ->
        if(column != 'tag' && row[column])
        {
          if(column0Value)
            counts[column] << row[column]

          totals[column] << row[column]
        }
      }

      if(row.tags)
        allTags.addAll(row.tags)

      if(column0Value)
      {
        if(row.state == 'ERROR')
          counts.errors++
        counts.instances++
      }
      if(row.state == 'ERROR')
        totals.errors++
      totals.instances++
    }

    groupByColumns.each { column ->
      counts[column] = counts[column].size()
      totals[column] = totals[column].size()
    }

    counts.tag = allTags.size()
    totals.tag = allTags.size()
    counts.tags = allTags.size()
    totals.tags = allTags.size()

    // removes rows where the first column is null or empty
    current = current.findAll { it[column0Name] }

    def delta = [:]

    def groupByColumn0 = current.groupBy { it.getAt(column0Name) }

    groupByColumn0.each { column0, list ->
      def errors = list.findAll { it.state == 'ERROR' }

      def row = [
          instancesCount: list.size(),
          errorsCount: errors.size()
      ]

      row.state = expectedSystem ? (row.errorsCount > 0 ? 'ERROR' : 'OK') : 'UNKNOWN'
      row.na = list.find { it.state == 'NA'} ? 'NA' : ''

      def entries = isErrorsFilter ? errors : list


      if(entries)
      {
        if(isSummaryFilter)
        {
          def summary = entries[0]
          if(entries.size() > 1)
          {
            summary = [:]
            columnNames.each { column ->
              if(column != 'tags')
                summary[column] = entries."${column}".unique()
            }

            def tags = new TreeSet()
            entries.each { entry ->
              if(entry.tags)
                tags.addAll(entry.tags)
            }

            if(tags)
              summary.tags = tags
          }
          entries = [summary]
        }
        row.entries = entries
        delta[column0] = row
      }
    }

    if(removeStatusInfoColumn)
      columnNames.remove("statusInfo")

    return [
        delta: delta,
        accuracy: deltaWithAccuracy.accuracy,
        sortableColumnNames: columnNames - 'tags',
        columnNames: columnNames,
        groupByColumns: groupByColumns,
        counts: counts,
        totals: totals,
        currentSystem: expectedSystem
    ]
  }

  private Collection<String> extractFilteredTags(SystemModel model)
  {
    extractFilteredTags(model?.filters)
  }

  private Collection<String> extractFilteredTags(SystemFilter filters)
  {
    if(filters instanceof TagsSystemFilter)
    {
      return filters.tags
    }

    if(filters instanceof LogicSystemFilterChain)
    {
      Set<String> tags = new HashSet<String>()
      filters.filters?.each {
        Collection<String> filteredTags = extractFilteredTags(it)
        if(filteredTags)
          tags.addAll(filteredTags)
      }
      if(tags)
        return tags
      else
        return null
    }
    
    return null
  }
}
