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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>GLU Console - Delta</title>
  <meta name="layout" content="main"/>
  <script type="text/javascript" src="${resource(dir:'js',file:'console.js')}"></script>
</head>
<body>
<g:if test="${delta}">
  <p>Quick Select:
    <a href="#" onClick="quickSelect('delta_${delta.id}', 'quickSelect', 0);return false;">Select None</a> |
    <a href="#" onClick="selectOne('delta_${delta.id}', 'quickSelect');return false;">Select First</a> |
    <a href="#" onClick="quickSelect('delta_${delta.id}', 'quickSelect', 100);return false;">Select All</a>
  <g:each in="['25', '33', '50', '66', '75']" var="pct">
    | <a href="#" onClick="quickSelect('delta_${delta.id}', 'quickSelect', ${pct});return false;">${pct}%</a>
  </g:each>
  </p>
  <div id="delta_${delta.id}">
  <g:form method="post" controller="plan" action="filter" id="${delta.id}">
    <g:hiddenField name="stepId" value="${delta.id}"/>
    <g:actionSubmit action="filter" value="Filter"/>
    <g:actionSubmit action="execute" value="Execute" onClick="return confirm('Are you sure you want to execute this plan ?');"/>
    <g:render template="delta" model="[delta: delta]"/>
  </g:form>
  </div>
</g:if>
<g:else>
  <h2>No delta... System is ok.</h2>
</g:else>
</body>
</html>
