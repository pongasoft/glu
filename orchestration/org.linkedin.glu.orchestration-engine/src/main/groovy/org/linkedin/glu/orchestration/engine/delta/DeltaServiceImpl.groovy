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

import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.provisioner.core.model.TagsSystemFilter
import org.linkedin.glu.provisioner.core.model.SystemFilter

class DeltaServiceImpl implements DeltaService
{
  @Initializable
  AgentsService agentsService

  def computeDelta(Fabric fabric, SystemModel expectedSystem)
  {
    if(expectedSystem && expectedSystem.fabric != fabric.name)
      throw new IllegalArgumentException("fabric mismatch")

    SystemModel currentSystem = agentsService.getCurrentSystemModel(fabric)

    [
        delta: computeDelta(currentSystem, expectedSystem),
        accuracy: currentSystem.metadata.accuracy
    ]
  }

  def computeDelta(SystemModel currentSystem, SystemModel expectedSystem)
  {
    (expectedSystem, currentSystem) = SystemModel.filter(expectedSystem, currentSystem)

    def entries = []

    Set<String> emptyAgents = new HashSet<String>()
    if(currentSystem?.metadata?.emptyAgents)
      emptyAgents.addAll(currentSystem.metadata.emptyAgents)

    if(expectedSystem == null)
    {
      currentSystem.each { SystemEntry currentEntry ->
        def entry = currentEntry.flatten()
        entry.state = 'UNKNOWN'
        entry.status = 'unknown'
        entries << entry
      }
    }
    else
    {
      def allKeys = (expectedSystem.findEntries().key + currentSystem.findEntries().key) as SortedSet

      allKeys.each { key ->
        SystemEntry currentEntry = currentSystem.findEntry(key)
        def cef = currentEntry?.flatten()
        
        SystemEntry expectedEntry = expectedSystem.findEntry(key)
        def eef = expectedEntry?.flatten()

        // means that it is not deployed at all
        if(!cef)
        {
          eef.status = 'notDeployed'
          eef.state = 'ERROR'
          setTags(eef, expectedEntry.tags)
          entries << eef
          emptyAgents.remove(eef.agent)
          return
        }

        // means that it should not be deployed at all
        if(!eef)
        {
          cef.status = 'unexpected'
          cef.state = 'ERROR'
          entries << cef
          emptyAgents.remove(cef.agent)
          return
        }

        // here we have both currentEntry and expectedEntry...
        emptyAgents.remove(cef.agent)

        def initParameters = eef.keySet().findAll { it.startsWith("initParameters.") } +
            cef.keySet().findAll { it.startsWith("initParameters.") }

        initParameters.each { n ->
          if(eef[n] != cef[n])
          {
            cef.status = 'versionMismatch'
            cef.statusInfo = "${n}:${eef[n]} != ${n}:${cef[n]}".toString()
            cef.state = 'ERROR'
          }
        }

        if(!cef.state)
        {
          if(cef['metadata.currentState'] == 'running')
          {
            def error = cef['metadata.error']
            if(error)
            {
              cef.status = 'error'
              cef.statusInfo = error.toString()
              cef.state = 'ERROR'
            }
            else
            {
              cef.status = 'running'
              cef.state = 'RUNNING'
            }
          }
          else
          {
            cef.status = 'notRunning'
            cef.state = 'ERROR'
          }
        }

        if(cef.state != 'ERROR')
        {
          // copy all missing keys from expected into current
          eef.each { k,v ->
            if(!cef.containsKey(k))
              cef[k] = v
          }
        }
        else
        {
          // override all keys from expected into current
          eef.each { k,v ->
            cef[k] = v
          }
        }

        setTags(cef, expectedEntry.tags)
        entries << cef
      }
    }

    emptyAgents.each { agent ->
      def entry= [:]
      entry.agent = agent
      entry['metadata.currentState'] = 'NA'
      entry.status = 'NA'
      entry.state = 'NA'
      setTags(entry, expectedSystem?.getAgentTags(agent)?.tags)
      entries << entry
    }

    return entries
  }

  private void setTags(def entry, Collection<String> entryTags)
  {
    if(entry.key)
    {
      entryTags?.each { String tag ->
        entry["tags.${tag}".toString()] = entry.key
      }
    }
    
    if(entryTags)
      entry.tags = entryTags as SortedSet
  }

  Map computeGroupByDelta(Fabric fabric,
                          SystemModel expectedSystem,
                          Map groupByDefinition,
                          Map groupBySelection)
  {
    def deltaWithAccuracy = computeDelta(fabric, expectedSystem)
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
        if(row.tags?.size() > 1)
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

      row.state = expectedSystem ? (row.errorsCount > 0 ? 'ERROR' : 'RUNNING') : 'UNKNOWN'
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
            groupByColumns.each { column ->
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
    SystemFilter filters = model?.filters

    if(filters instanceof TagsSystemFilter)
    {
      return filters.tags
    }
    else
     return []
  }
}
