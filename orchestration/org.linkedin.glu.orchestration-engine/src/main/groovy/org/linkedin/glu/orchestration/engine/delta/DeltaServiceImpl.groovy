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
import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta.DeltaState
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.glu.utils.core.Externable
import org.linkedin.glu.agent.tracker.AgentsTracker.AccuracyLevel
import org.linkedin.glu.orchestration.engine.delta.impl.DeltaUtils
import org.linkedin.glu.utils.collections.ComparableTreeSet
import org.linkedin.glu.orchestration.engine.authorization.AuthorizationService
import java.security.AccessControlException

class DeltaServiceImpl implements DeltaService
{
  public static final String DEFAULT_CUSTOM_DELTA_DEFINITION_NAME = "<default>"

  @Initializable
  AgentsService agentsService

  @Initializable
  FabricService fabricService

  @Initializable(required = true)
  DeltaMgr deltaMgr

  @Initializable
  boolean stateDeltaOverridesDelta = false

  @Initializable(required = true)
  CustomDeltaDefinitionStorage customDeltaDefinitionStorage

  @Initializable
  AuthorizationService authorizationService

  String prettyPrint(SystemModelDelta delta)
  {
    computeDeltaAsJSON([delta: delta, prettyPrint: true])
  }

  @Override
  String computeDeltaAsJSON(def params)
  {
    SystemModelDelta delta = params.delta

    if(!delta)
    {
      SystemModel expectedModel = params.expectedModel

      if(!expectedModel)
        return null

      SystemModel currentModel = params.currentModel

      if(currentModel == null)
      {
        Fabric fabric = fabricService.findFabric(expectedModel.fabric)

        if(!fabric)
          throw new IllegalArgumentException("unknown fabric ${expectedModel.fabric}")

        currentModel = agentsService.getCurrentSystemModel(fabric)
      }

      delta = deltaMgr.computeDelta(expectedModel, currentModel, null)
    }

    boolean flatten = params.flatten?.toString() == "true"

    def map

    if(flatten)
      map = delta.flatten(new TreeMap())
    else
    {
      map = new TreeMap()
      delta.keys.each { String key ->
        map[key] = delta.findEntryDelta(key)
      }
    }

    // filter by error only if necessary
    boolean errorsOnly = params.errorsOnly?.toString() == "true"
    if(errorsOnly)
      map = map.findAll {k, v -> v.deltaState != DeltaState.OK}

    if(!flatten)
    {
      map = GroovyCollectionsUtils.collectKey(map, new TreeMap()) { String key, SystemEntryDelta sed ->
        def res = GroovyCollectionsUtils.collectKey(sed.values, new TreeMap()) { String valueKey, SystemEntryValue sev ->
          if(sev.hasDelta())
            return [ev: toExternalValue(sev.expectedValue), cv: toExternalValue(sev.currentValue)]
          else
            return toExternalValue(sev.expectedValue)
        }
        if(sed.hasErrorValue())
          res.errorValueKeys = sed.getErrorValueKeys(new TreeSet())
        res
      }
    }
    
    // build map for json
    map = [
      accuracy: delta.currentSystemModel.metadata.accuracy,
      delta: map
    ]

    def jsonDelta

    boolean prettyPrint = params.prettyPrint?.toString() == "true"

    if(prettyPrint)
      jsonDelta = JsonUtils.prettyPrint(map)
    else
      jsonDelta = JsonUtils.compactPrint(map)

    return jsonDelta
  }

  protected Object toExternalValue(Object value)
  {
    if(value instanceof Externable)
      return value.toExternalRepresentation()
    return value
  }


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

  @Override
  Map computeRawDelta(SystemModel expectedModel)
  {
    if(!expectedModel)
      return null

    Fabric fabric = fabricService.findFabric(expectedModel.fabric)

    if(!fabric)
      throw new IllegalArgumentException("unknown fabric ${expectedModel.fabric}")

    SystemModel currentModel = agentsService.getCurrentSystemModel(fabric)

    [
        delta: deltaMgr.computeDelta(expectedModel, currentModel, null),
        accuracy: currentModel.metadata.accuracy
    ]
  }

  Collection<Map<String, Object>> computeDelta(SystemModel expectedModel,
                                               SystemModel currentModel)
  {
    SystemModelDelta delta = deltaMgr.computeDelta(expectedModel, currentModel, null)

    Collection<Map<String, Object>> flattenedDelta =
      delta.flatten(new TreeMap<String, Map<String, Object>>()).values()

    if(stateDeltaOverridesDelta)
    {
      flattenedDelta.each { Map m ->
        if(m.status == 'delta')
        {
          SystemEntryValueWithDelta<String> entryStateDelta =
            delta.findEntryDelta(m.key).findEntryStateDelta()
          if(entryStateDelta)
          {
            m.status = 'notExpectedState'
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
      def errors = list.findAll { it.state == DeltaState.ERROR }
      def deltas = list.findAll { it.status == "delta" }

      def row = [
          instancesCount: list.size(),
          errorsCount: errors.size(),
          deltasCount: deltas.size()
      ]

      row.state = expectedSystem ? (row.errorsCount > row.deltasCount ? 'ERROR' : row.deltasCount > 0 ? 'DELTA' : 'OK') : 'UNKNOWN'
      row.na = list.find { it.state == DeltaState.NA} ? 'NA' : ''

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
  public static final String HIDDEN_TAG_COLUMN = "__tag"

  CustomGroupByDelta computeCustomGroupByDelta(SystemModel expectedModel,
                                               SystemModel currentModel,
                                               CustomDeltaDefinition deltaDefinition)
  {
    CustomGroupByDelta res = new CustomGroupByDelta(expectedModel: expectedModel,
                                                    deltaDefinition: deltaDefinition,
                                                    accuracy: currentModel.metadata.accuracy as AccuracyLevel)

    def current = computeDelta(expectedModel, currentModel)

    List<CustomDeltaColumnDefinition> columnsDefinitions = deltaDefinition.columnsDefinition

    def column0Source = columnsDefinitions[0].source

    // when the first column is tags, we temporarily introduce a new column and new rows
    // in order to have only one tag => as a result there will be duplicate rows
    if(column0Source == 'tags')
    {
      deltaDefinition = deltaDefinition.clone()
      CustomDeltaColumnDefinition column0 = deltaDefinition.columnsDefinition[0]

      column0.source = HIDDEN_TAG_COLUMN
      column0Source = column0.source
      columnsDefinitions = [column0, * columnsDefinitions[1..-1]]
      deltaDefinition.columnsDefinition = columnsDefinitions

      Set<String> filteredTags = extractFilteredTags(expectedModel) as Set

      def newCurrent = []
      current.each { row ->
        if(row.tags?.size() > 0)
        {
          row.tags.each { tag ->
            if(!filteredTags || filteredTags.contains(tag))
            {
              def newRow = [*:row]
              newRow[HIDDEN_TAG_COLUMN] = new ComparableTreeSet([tag])
              newCurrent << newRow
            }
          }
        }
        else
          newCurrent << row
      }
      current = newCurrent
    }

    def orderBy = deltaDefinition.tailOrderBy

    def allTags = new HashSet()

    Map<String, ? extends Object> counts = [errors: 0, instances: 0]
    Map<String, ? extends Object> totals = [errors: 0, instances: 0]

    columnsDefinitions.each { CustomDeltaColumnDefinition columnDefinition ->
      def column = columnDefinition.name
      counts[column] = new HashSet()
      totals[column] = new HashSet()
    }

    current.each { row ->
      def column0Value = row[column0Source]
      columnsDefinitions.each { CustomDeltaColumnDefinition columnDefinition ->
        def columnSource = columnDefinition.source
        def columnName = columnDefinition.name
        if(columnSource != HIDDEN_TAG_COLUMN && row[columnSource])
        {
          if(column0Value)
            counts[columnName] << row[columnSource]

          totals[columnName] << row[columnSource]
        }
      }

      if(row.tags)
        allTags.addAll(row.tags)

      if(column0Value)
      {
        if(row.state == DeltaState.ERROR)
        {
          counts.errors++
          counts.instances++
        }
        else
        {
          if(!deltaDefinition.errorsOnly)
            counts.instances++
        }
      }
      if(row.state == DeltaState.ERROR)
        totals.errors++
      totals.instances++
    }

    columnsDefinitions.each { CustomDeltaColumnDefinition columnDefinition ->
      def column = columnDefinition.name
      counts[column] = counts[column].size()
      totals[column] = totals[column].size()
    }

    counts.tag = allTags.size()
    totals.tag = allTags.size()
    counts.tags = allTags.size()
    totals.tags = allTags.size()

    // removes rows where the first column is null or empty
    current = current.findAll { it[column0Source] }

    String column0OrderBy = deltaDefinition.firstColumn.orderBy
    String column0Name = deltaDefinition.firstColumn.name

    def delta

    if(column0OrderBy == null)
      delta = [:]
    else
      delta = new TreeMap(DeltaUtils.delatRowsComparators.get(column0OrderBy))

    def groupByColumn0 = current.groupBy { it.getAt(column0Source) }

    groupByColumn0.each { column0, Collection list ->
      def errors = list.findAll { it.state == DeltaState.ERROR }
      def deltas = list.findAll { it.status == "delta" }

      Map<String, ? extends Object> row = [
          instancesCount: list.size(),
          errorsCount: errors.size(),
          deltasCount: deltas.size()
      ]

      row.state = expectedModel ?
        (row.errorsCount > row.deltasCount ? 'ERROR' : row.deltasCount > 0 ? 'DELTA' : 'OK') :
        'UNKNOWN'
      row.na = list.find { it.state == DeltaState.NA} ? 'NA' : ''

      def entries = deltaDefinition.errorsOnly ? errors : list

      if(entries)
      {
        if(deltaDefinition.summary)
        {
          def summary = [:]
          columnsDefinitions.each { CustomDeltaColumnDefinition column ->
            if(column.name == column0Name)
              summary[column.name] = column0
            else
            {
              summary[column.name] = column.groupBy(entries."${column.source}".findAll { it != null })
            }
          }
          entries = [summary]
        }
        else
        {
          def newEntries = []
          entries.each { entry ->
            def newEntry = [:]
            columnsDefinitions.each { CustomDeltaColumnDefinition column ->
              if(column.name == column0Name)
                newEntry[column.name] = column0
              else
                newEntry[column.name] = entry."${column.source}"
            }
            newEntries << newEntry
          }
          entries = newEntries
        }

        // if more than one entry we sort them
        if(entries.size() > 1)
        {
          DeltaUtils.sortBy(entries, orderBy)
        }

        row.entries = entries
        delta[column0] = row
      }
    }

    res.counts = counts
    res.totals = totals
    res.groupByDelta = delta

    return res
  }

  @Override
  CustomGroupByDelta computeCustomGroupByDelta(SystemModel expectedModel,
                                               CustomDeltaDefinition deltaDefinition)
  {
    if(!expectedModel)
      return null

    Fabric fabric = fabricService.findFabric(expectedModel.fabric)

    if(!fabric)
      throw new IllegalArgumentException("unknown fabric ${expectedModel.fabric}")

    SystemModel currentModel = agentsService.getCurrentSystemModel(fabric)

    computeCustomGroupByDelta(expectedModel, currentModel, deltaDefinition)
  }

  @Override
  CustomGroupByDelta computeCustomGroupByDelta(SystemModel expectedModel,
                                               UserCustomDeltaDefinition userDeltaDefinition)
  {
    computeCustomGroupByDelta(expectedModel, userDeltaDefinition?.customDeltaDefinition)
  }

  @Override
  boolean saveUserCustomDeltaDefinition(UserCustomDeltaDefinition definition)
  {
    if(definition.username != authorizationService.executingPrincipal)
      throw new AccessControlException("${definition.username} is not the owner!")
    
    customDeltaDefinitionStorage.save(definition)
  }

  @Override
  UserCustomDeltaDefinition saveAsNewUserCustomDeltaDefinition(UserCustomDeltaDefinition definition)
  {
    if(definition.username != authorizationService.executingPrincipal)
      throw new AccessControlException("${definition.username} is not the owner!")

    definition = cloneForUser(definition.username, definition)

    customDeltaDefinitionStorage.save(definition)

    return definition
  }

  @Override
  boolean deleteUserCustomDeltaDefinition(UserCustomDeltaDefinition definition)
  {
    if(definition.username != authorizationService.executingPrincipal)
      throw new AccessControlException("${definition.username} is not the owner!")

    customDeltaDefinitionStorage.delete(definition)
  }

  @Override
  UserCustomDeltaDefinition findUserCustomDeltaDefinitionByName(String name)
  {
    customDeltaDefinitionStorage.findByUsernameAndName(authorizationService.executingPrincipal,
                                                       name)
  }

  @Override
  Map findAllUserCustomDeltaDefinition(boolean includeDetails, Object params)
  {
    customDeltaDefinitionStorage.findAllByUsername(authorizationService.executingPrincipal,
                                                   includeDetails,
                                                   params)
  }

  @Override
  Map findAllUserCustomDeltaDefinitionShareable(boolean includeDetails, Object params)
  {
    customDeltaDefinitionStorage.findAllShareable(includeDetails, params)
  }

  @Override
  UserCustomDeltaDefinition findDefaultCustomDeltaDefinition()
  {
    customDeltaDefinitionStorage.findByUsernameAndName(null,
                                                       DEFAULT_CUSTOM_DELTA_DEFINITION_NAME)
  }

  @Override
  UserCustomDeltaDefinition findDefaultUserCustomDeltaDefinition(String defaultName)
  {
    String username = authorizationService.executingPrincipal

    UserCustomDeltaDefinition res = findUserCustomDeltaDefinitionByName(defaultName)

    if(!res && defaultName != DEFAULT_CUSTOM_DELTA_DEFINITION_NAME)
    {
      res = findUserCustomDeltaDefinitionByName(DEFAULT_CUSTOM_DELTA_DEFINITION_NAME)
    }

    if(!res)
    {
      UserCustomDeltaDefinition ucdd =
        cloneForUser(username, findDefaultCustomDeltaDefinition())
      saveUserCustomDeltaDefinition(ucdd)
      res = ucdd
    }

    return res
  }

  private UserCustomDeltaDefinition cloneForUser(String username, UserCustomDeltaDefinition ucdd)
  {
    ucdd = ucdd?.clone()
    ucdd?.username = username
    return ucdd
  }

  @Override
  boolean saveDefaultCustomDeltaDefinition(CustomDeltaDefinition definition)
  {
    UserCustomDeltaDefinition ucdd = findDefaultCustomDeltaDefinition()
    if(!ucdd)
      ucdd = new UserCustomDeltaDefinition()
    definition.name = DEFAULT_CUSTOM_DELTA_DEFINITION_NAME
    ucdd.customDeltaDefinition = definition
    return saveUserCustomDeltaDefinition(ucdd)
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
