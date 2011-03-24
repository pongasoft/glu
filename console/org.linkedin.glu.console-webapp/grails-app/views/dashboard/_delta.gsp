%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
  - Portions Copyright (c) 2011 Yan Pujante
  -
  - Licensed under the Apache License, Version 2.0 (the "License"); you may not
  - use this file except in compliance with the License. You may obtain a copy of
  - the License at
  -
  - http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  - WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  - License for the specific language governing permissions and limitations under
  - the License.
  --}%

<%@ page import="org.linkedin.glu.grails.utils.ConsoleConfig; org.linkedin.glu.agent.tracker.AgentsTracker.AccuracyLevel" %>
<g:set var="columnsTail" value="${columnNames[1..-1]}"/>
<g:set var="columns" value="${ConsoleConfig.getInstance().defaults.dashboard}"/>
<div id="__delta">
  <div id="__delta_menu">
    <ul class="submenu">
      <li>Group By:</li>
      <g:each in="${groupByColumns}" var="column" status="columnIdx">
        <li>
          <g:if test="${column == columnNames[0]}"><input type="hidden" id="groupBy" name="groupBy" value="${column}" />${columns[column].name}:${totals[column]}</g:if>
          <g:else>
            <a href="#" onclick="render('${column}');return false;">${columns[column].name}:${totals[column]}</a>
            <cl:checkBoxInitFromParams name="${column}" id="${column}" checkedByDefault="${columns[column].checked}" onclick="${columnNames.contains(column) ? 'showHideColumn(\'' + column + '\');' : 'renderSame();'}"/>
          </g:else>
        </li>
      </g:each>
    </ul>
    <ul class="submenu">
      <li>Show/Hide:</li>
      <li>Summary: <cl:checkBoxInitFromParams name="summary" id="summaryFilter" onclick="renderSame();"/></li>
      <li>Errors: <cl:checkBoxInitFromParams name="errors" checkedByDefault="${false}" id="errorsFilter" onclick="renderSame();"/></li>
      <g:each in="${columns.findAll {k,v -> !v.groupBy}}" var="e">
        <li>${e.value.name}: <cl:checkBoxInitFromParams name="${e.key}" id="${e.key}" checkedByDefault="${e.value.checked}" onclick="${columnNames.contains(e.key) ? 'showHideColumn(\'' + e.key + '\');' : 'renderSame();'}"/></li>
      </g:each>
    </ul>
  </div>
  <div id="__delta_content">
    <table>
      <tr>
        <th>${columns[columnNames[0]].name}:${counts[columnNames[0]]}</th>
        <th>I:${counts['instances']}</th>
        <th>E:${counts['errors']}</th>
        <g:each in="${columnsTail}" var="column" status="columnIdx">
          <th class="${column}">
            <g:if test="${counts[column] == null}">${columns[column].name}</g:if>
            <g:else>${columns[column].name}:${counts[column]}</g:else>
          </th>
        </g:each>
      </tr>
      <g:if test="${accuracy == AccuracyLevel.INACCURATE}">
        <tr id="__delta_inaccurate">
          <td colspan="${3 + columnsTail.size()}">
            <div class="warning">Warning!!! Warning!!! Warning!!!</div>
            The data you are seeing is not accurate as the console is in the process of loading it from ZooKeeper
            <div class="warning">Warning!!! Warning!!! Warning!!!</div>
          </td>
        </tr>
      </g:if>
      <% /* Column 0 is sorted and we do the grouping on (span multiple rows) */ %>
      <g:each in="${delta.keySet().sort()}" var="column0Value" status="line">
        <tbody id="${column0Value}">
        <% /* Entries (grouped by column0) are sorted by column 1 */ %>
        <g:set var="row" value="${delta[column0Value]}"/>
        <g:set var="entries" value="${ConsoleUtils.sortBy(row.entries, sortableColumnNames)}"/>
        <g:set var="rowState" value="${row.state + ' ' + row.na}"/>
        <g:each in="${entries}" var="entry" status="rowIdx">
          <tr class="${rowState}">
            <g:if test="${rowIdx == 0}">
              <td rowspan="${entries.size()}" class="${rowState} column0">
                <cl:formatDeltaValue columnName="${columnNames[0]}" detail="${entry}" columns="${columns}"/>
              </td>
              <td rowspan="${entries.size()}" class="${rowState} totalCount">
                ${row.instancesCount}
              </td>
              <td rowspan="${entries.size()}" class="${rowState} errorsCount">
                ${row.errorsCount}
              </td>
            </g:if>
            <g:each in="${columnsTail}" var="columnName" status="columnIdx">
              <td class="${entry.state} detail ${columnName}">
                <cl:formatDeltaValue columns="${columns}" columnName="${columnName}" detail="${entry}" row="d-${line}-${rowIdx}"/>
              </td>
            </g:each>
          </tr>
        </g:each>
        </tbody>
      </g:each>
    </table>
  </div>
</div>
