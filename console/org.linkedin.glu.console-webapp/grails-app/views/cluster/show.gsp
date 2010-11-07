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

<%@ page import="org.linkedin.glu.provisioner.plan.api.IStep" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Cluster: ${cluster}</title>
  <style type="text/css">
  input.stepCheckBox {
    display: none;
  }
  ul {
    padding: 0;
    margin: 0;
  }
  li {
   list-style: none;
  }
  </style>
  <link rel="stylesheet" href="${resource(dir:'css',file:'audit.css')}"/>
  <script type="text/javascript" src="${resource(dir:'js',file:'audit.js')}"></script>
  <g:javascript>
<cl:renderAuditJS groupBy="cluster" filter="[name: 'cluster', value: cluster]"/>
  </g:javascript>
  <script type="text/javascript" src="${resource(dir:'js',file:'console_yui.js')}"></script>
</head>
<body>
<div class="body">
  <ul class="submenu">
    <li><g:link controller="dashboard">Dashboard</g:link></li>
    <li><g:link controller="system" action="list">System<g:if test="${currentSystem}">[${currentSystem.topology.topology.version}]</g:if></g:link></li>
    <li class="selected">cluster [${cluster}]</li>
  </ul>
  <table>
    <tr>
      <th>Name</th>
      <th>Kind</th>
      <th>GLU Script</th>
      <th>Skeleton</th>
      <th>Wars</th>
      <th>Description</th>
    </tr>
    <g:each in="${ConsoleUtils.sortByName(containers)}" var="app">
      <tr>
        <td>${app.name}</td>
        <td>${app.kind}</td>
        <td><cl:linkToBom bom="${currentSystem.bom}" ref="${app.scriptRef}"/></td>
        <td><cl:linkToBom bom="${currentSystem.bom}" ref="${app.skeletonRef}"/></td>
        <td>
          <g:render template="show_${app.kind}" model="[app: app]"/>
        </td>
        <td>${app.description}</td>
      </tr>
    </g:each>
  </table>

  <g:include controller="dashboard" action="renderAudit" params="[groupBy: 'cluster', columnNameFilter: 'cluster', columnValueFilter: cluster]"/>

  <g:render template="/plan/selectDelta" model="[delta: delta, title: 'Cluster Delta']"/>

  <g:render template="/plan/selectDelta" model="[delta: bounce, title: 'Bounce Cluster']"/>

  <g:render template="/plan/selectDelta" model="[delta: redeploy, title: 'Redeploy Cluster']"/>
  
  <g:if test="${missingAgents}">
    <h2>Missing agents [<g:link controller="fabric" action="listAgentFabrics">Fix it</g:link>]</h2>
    <ul>
      <g:each in="${missingAgents}" var="agentName">
        <li>${agentName.encodeAsHTML()}</li>
      </g:each>
    </ul>
  </g:if>
</div>
</body>
</html>
