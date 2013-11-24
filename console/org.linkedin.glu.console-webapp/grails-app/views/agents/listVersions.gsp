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
  <title>Agents</title>
  <meta name="layout" content="main"/>
  <script type="text/javascript" src="${resource(dir:'js',file:'console.js')}"></script>
  <style type="text/css">
    .separator {
      border-top: 1px solid black;
      padding-top: 1.5em;
    }
    table {
      width: auto;
    }
  </style>
</head>
<body>
<h1>Agent Upgrade</h1>
<cl:form method="post" controller="agents" action="upgrade">
  <ul>
  <li>New Version: <g:textField name="version"/></li>
  <li>Coordinates: <g:textField name="coordinates" size="100"/></li>
  </ul>
  <g:actionSubmit class="btn btn-primary" action="upgrade" value="Upgrade"/>
<g:each in="${versions.keySet().sort()}" var="version">
  <h2>${version}</h2>
  <p>Quick Select:
    <a href="#" onClick="quickSelect('agent_${version}', 'agentCheckbox', 0);return false;">Select None</a> |
    <a href="#" onClick="selectOne('agent_${version}', 'agentCheckbox');return false;">Select First</a> |
    <a href="#" onClick="quickSelect('agent_${version}', 'agentCheckbox', 100);return false;">Select All</a>
  <g:each in="['25', '33', '50', '66', '75']" var="pct">
    | <a href="#" onClick="quickSelect('agent_${version}', 'agentCheckbox', ${pct});return false;">${pct}%</a>
  </g:each>
  </p>
  <table class="table table-bordered table-condensed noFullWidth" id="agent_${version}">
    <g:each in="${versions[version].agentName.sort()}" var="agentName">
      <tr>
        <td>${agentName}</td>
        <td><input class="agentCheckbox" type="checkbox" name="agents" value="${agentName}"/></td>
      </tr>
    </g:each>
  </table>
</g:each>
</cl:form>
<h2 class="separator">Agent Cleanup Upgrade</h2>
<cl:form method="post" controller="agents" action="upgrade">
  <g:actionSubmit class="btn btn-primary" action="cleanup" value="Cleanup"/>
  <span class="help">Cleanup all previously leftover upgrade</span>
</cl:form>
</body>
</html>