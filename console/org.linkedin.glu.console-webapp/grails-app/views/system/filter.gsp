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

<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>System Filter: ${params.title?.encodeAsHTML()}</title>
  <style type="text/css">
  ul {
    padding: 0;
    margin: 0;
  }
  li {
   list-style: none;
  }
  input.stepCheckBox {
    display: none;
  }
  input.quickSelect {
    display: none;
  }
  #systemModel {
    padding-top: 1em;
    padding-bottom: 1em;
  }
  </style>
  <link rel="stylesheet" href="${resource(dir:'css',file:'audit.css')}"/>
  <script type="text/javascript" src="${resource(dir:'js',file:'audit.js')}"></script>
  <g:javascript>
<cl:renderAuditJS filter="${params.systemFilter}"/>
  </g:javascript>
  <script type="text/javascript" src="${resource(dir:'js',file:'console_yui.js')}"></script>
</head>
<body>
<div class="body">
  <ul class="submenu">
    <li><g:link controller="dashboard">Dashboard</g:link></li>
    <li><g:link controller="system" action="list">System</g:link></li>
    <li class="selected">${params.title}</li>
  </ul>
  <div id="systemModel">
    <g:render template="systemModel" model="[systems: [request.system]]"/>
  </div>
  <g:include controller="dashboard" action="renderAudit" params="[groupBy: params.groupBy]"/>

  <g:render template="/plan/selectDelta" model="[delta: delta, title: 'Deploy: ' + params.title]"/>

  <g:render template="/plan/selectDelta" model="[delta: bounce, title: 'Bounce: ' + params.title ]"/>

  <g:render template="/plan/selectDelta" model="[delta: redeploy, title: 'Redeploy: ' + params.title]"/>

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
