%{--
  - Copyright 2010-2010 LinkedIn, Inc
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

<%@ page import="org.linkedin.glu.grails.utils.ConsoleConfig; org.linkedin.glu.console.controllers.SystemController" %>
<g:set var="columns" value="${columns ?: ConsoleConfig.getInstance().defaults.system}"/>
<g:set var="system" value="${system ?: request.system}"/>
<table>
  <thead>
  <tr>
    <th>Id</th>
    <th>Fabric</th>
    <g:each in="${columns.values()}" var="column">
      <th>${column.name}</th>
    </g:each>
    <th>Filters</th>
  </tr>
  </thead>
  <tbody>
  <tr>
    <td class="systemId"><g:link controller="system" action="view" id="${system.id}">${system.id}</g:link></td>
    <td>${system.fabric}</td>
    <g:set var="stats" value="${system.computeStats(columns.keySet())}"/>
    <g:each in="${columns.keySet()}" var="columnName">
      <td>${stats[columnName] ?: 0}</td>
    </g:each>
    <td>
      <g:render template="systemFilter" model="[filter: system.filters]"/>
    </td>
  </tr>
  </tbody>
</table>
