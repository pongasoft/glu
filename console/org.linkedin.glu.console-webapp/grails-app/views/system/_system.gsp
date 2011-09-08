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

<%@ page import="org.linkedin.util.lang.MemorySize; org.linkedin.glu.grails.utils.ConsoleConfig; org.linkedin.glu.console.controllers.SystemController" %>
<g:set var="columns" value="${columns == null ? ConsoleConfig.getInstance().defaults.system : columns}"/>
<g:form action="setAsCurrent" method="post">
<table>
  <thead>
  <tr>
    <th>Id</th>
    <th>Fabric</th>
    <th>Date Created</th>
    <g:each in="${columns.values()}" var="column">
      <th>${column.name}</th>
    </g:each>
    <th>Size</th>
    <th>Current</th>
  </tr>
  </thead>
  <tbody>
  <g:each in="${systems}" status="i" var="system">
    <tr class="${(i % 2) == 0 ? 'odd' : 'even'} ${(system.systemId == request.system?.id) ? 'current' : ''}">
      <td class="systemId"><g:link controller="system" action="view" id="${system.systemId}">${system.systemId}</g:link></td>
      <td>${system.fabric}</td>
      <td><cl:formatDate date="${system.dateCreated}"/></td>
      <g:set var="stats" value="${system.systemModel?.computeStats(columns.keySet()) ?: [:]}"/>
      <g:each in="${columns.keySet()}" var="columnName">
        <td>${stats[columnName] ?: 0}</td>
      </g:each>
      <td><g:if test="${system.size}">${new MemorySize(system.size)}</g:if><g:else>N/A</g:else></td>
      <td><input type="radio" name="id" value="${system.systemId}" ${system.systemId == request.system?.id ? 'checked="checked"' : ''} onclick="if(confirm('Are you sure you want to set system [${system.systemId}] as the curren one?')) {this.form.submit()} else return false;"/></td>
    </tr>
  </g:each>
  </tbody>
</table>
</g:form>