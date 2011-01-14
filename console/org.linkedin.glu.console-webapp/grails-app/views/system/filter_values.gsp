%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
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

<%@ page import="org.linkedin.glu.grails.utils.ConsoleConfig" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Select a filter</title>
  <style type="text/css">

.all {
  text-align: center;
}

  table td {
  padding: 0.2em;
}
table td.current {
  background-color: #89c9dd;
}  </style>
</head>
<body>
<div class="body">
  <ul class="submenu">
    <li><g:link controller="dashboard">Dashboard</g:link></li>
    <li><g:link controller="system" action="list">System</g:link></li>
    <li class="selected">Filter [${params.id}]</li>
  </ul>

  <g:set var="columns"
         value="${ConsoleConfig.getInstance().defaults.model.find {it.name == params.id}?.header ?: []}"/>

  <g:if test="${system}">
    <h1>Select your ${params.id}</h1>
    <table>
      <tr>
        <th>${params.id}</th>
        <g:each in="${columns}" var="c">
          <th>${c}</th>
        </g:each>
      </tr>
      <g:each in="${ConsoleUtils.sortBy(values.values(), 'name')}" var="value">
        <tr>
          <td class="${currentValue == value.name ? 'current' : 'not-current'}"><g:link controller="system" action="filter_values" id="${params.id}" params="[value: value.name]">${value.name.encodeAsHTML()}</g:link></td>
          <g:each in="${columns}" var="c">
            <td class="${currentValue == value.name ? 'current' : 'not-current'}">${value[c]}</td>
          </g:each>
        </tr>
      </g:each>
      <tr>
        <td class="${currentValue == null ? 'current' : 'not-current'} all" colspan="${1 + columns.size()}"><g:link controller="system" action="filter_values" id="${params.id}" params="[value: '*']">All [${params.id}]</g:link></td>
      </tr>
    </table>
  </g:if>
  <g:else>
    No system.
  </g:else>
</div>
</body>
</html>
