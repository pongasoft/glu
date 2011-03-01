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

package org.linkedin.glu.console.controllers

import org.linkedin.glu.console.services.AuditService
import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemFilter
import org.linkedin.glu.provisioner.core.model.TagsSystemFilter

/**
 * @author ypujante@linkedin.com
 */
class DashboardController extends ControllerBase
{
  AuditService auditService
  ConsoleConfig consoleConfig

  def beforeInterceptor = {
    // we make sure that the fabric is always set before executing any action
    return ensureCurrentFabric()
  }

  /**
   * Redirect to audit 
   */
  def index = {
    redirect(action: 'audit')
  }

  /**
   * Renders only the portion below the menus
   */
  def renderAudit = {
    render(template: '/dashboard/audit', model: doAudit())
  }

  /**
   * Audit the live system: display condensed view of all apps accross all agents
   */
  def audit = {
    return doAudit()
  }

  private def doAudit()
  {
    def auditWithAccuracy = auditService.audit(request.fabric, request.system)
    def current = auditWithAccuracy.audit
    def currentSystem = request.system

    def columnNames = consoleConfig.defaults.dashboard.collect { k, v -> k }

    def groupByColumns = consoleConfig.defaults.dashboard.findAll { k, v -> v.groupBy }.collect { k,v -> k}

    consoleConfig.defaults.dashboard.each { k, v ->
      def checked = v.checked
      if(params[k] == 'false')
      {
        checked = false
      }
      if(params[k] == 'true')
      {
        checked = true
      }

      if(!checked)
        columnNames.remove(k)
    }

    def isSummaryFilter = !(params['summary'] == 'false')
    def isErrorsFilter = params['errors'] == 'true'

    if(params.groupBy)
    {
      columnNames.remove(params.groupBy)
      columnNames = [params.groupBy, * columnNames]
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
      Set<String> filteredTags = extractFilteredTags(currentSystem) as Set

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

    def audit = [:]

    def groupByColumn0 = current.groupBy { it.getAt(column0Name) }

    groupByColumn0.each { column0, list ->
      def errors = list.findAll { it.state == 'ERROR' }

      def row = [
          instancesCount: list.size(),
          errorsCount: errors.size()
      ]

      row.state = currentSystem ? (row.errorsCount > 0 ? 'ERROR' : 'RUNNING') : 'UNKNOWN'
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
        audit[column0] = row
      }
    }

    return [
        audit: audit,
        accuracy: auditWithAccuracy.accuracy,
        sortableColumnNames: columnNames - 'tags',
        columnNames: columnNames,
        groupByColumns: groupByColumns,
        counts: counts,
        totals: totals,
        currentSystem: currentSystem
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
