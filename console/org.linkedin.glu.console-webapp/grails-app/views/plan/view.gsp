%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
  - Portions Copyright (c) 2011-2013 Yan Pujante
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
  <title>Plan Preview</title>
  <meta name="layout" content="main"/>
  <script type="text/javascript" src="${resource(dir:'js',file:'console.js')}"></script>
</head>
<body>
<g:if test="${plan}">
  <p>Quick Select:
    <a href="#" onClick="quickSelect('plan_${plan.id}', 'quickSelect', 0);return false;">Select None</a> |
    <a href="#" onClick="selectOne('plan_${plan.id}', 'quickSelect');return false;">Select First</a> |
    <a href="#" onClick="quickSelect('plan_${plan.id}', 'quickSelect', 100);return false;">Select All</a>
  <g:each in="['25', '33', '50', '66', '75']" var="pct">
    | <a href="#" onClick="quickSelect('plan_${plan.id}', 'quickSelect', ${pct});return false;">${pct}%</a>
  </g:each>
  </p>
  <div id="plan_${plan.id}">
  <cl:form method="post" controller="plan" action="filter" id="${plan.id}">
    <g:hiddenField name="stepId" value="${plan.id}"/>
    <g:actionSubmit class="btn btn-primary" action="execute" value="Execute" onClick="return confirm('Are you sure you want to execute this plan ?');"/>
    <g:actionSubmit class="btn" action="filter" value="Filter"/>
    <g:render template="plan" model="[plan: plan]"/>
  </cl:form>
  </div>
</g:if>
<g:else>
  <h2>No plan... System is ok.</h2>
</g:else>
</body>
</html>
