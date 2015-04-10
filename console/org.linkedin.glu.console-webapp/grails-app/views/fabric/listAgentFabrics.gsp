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
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'fabric-listAgentFabrics.css')}"/>
  <script type="text/javascript" src="${resource(dir:'js', file:'console.js')}"></script>
  <meta name="layout" content="main"/>
  <title>Agents/Fabric</title>
  <style type="text/css">
  .missing-old {
    color: black;
    background: #ffdddd;
  }
  .missing-new {
    color: black;
    background: #ddddff;
  }
  .unknown {
    color: black;
    background: #dddddd;
  }
    table {
      width: auto;
    }
  </style>
</head>
<body>
<div class="body">
  <g:if test="${flash.success && flash.errors}">
    <h1>Warnings</h1>
    <table>
      <tr>
        <th>Agent</th>
        <th>Error</th>
      </tr>
      <g:each in="${flash.errors}" var="error">
        <tr>
        <td>${error.key.encodeAsHTML()}</td>
        <td>${error.value.message.encodeAsHTML()}
          <div class="flash-stackTrace">${error.value.stackTrace.join('\n').encodeAsHTML()}</div>
        </td>
        </tr>
      </g:each>
    </table>
  </g:if>

  <h1>Unassigned agents</h1>
  <h2>Fabric pre-selection is based on current fabric [${request.fabric}]!</h2>
  <g:if test="${unassignedAgents}">
    <p>Quick defaults:
      <a href="#" onclick="$('#unassignedAgents').find('select').val('${request.fabric}'); return false;">${request.fabric}</a>
      <a href="#" onclick="$('#unassignedAgents').find('select').val('-Choose-'); return false;">[None]</a>
    </p>
    <g:set var="status" value="['missing-old': 'Agent is most likely down', 'missing-new': 'Agent is most likely new', 'unknown': 'Unknown to this fabric']"/>
    <cl:form name="unassignedAgents" action="setAgentsFabrics">
      <table class="table table-bordered table-condensed">
        <tr>
          <th>Agent</th>
          <th>Fabric</th>
          <th>Status</th>
        </tr>
        <g:each in="${unassignedAgents}" var="agent">
          <tr class="${agent.value}">
            <td>${agent.key.encodeAsHTML()}</td>
            <td><g:select name="${agent.key.encodeAsHTML()}"
                          noSelection="['': '-Choose-']"
                          from="${fabrics}"
                          optionKey="name" optionValue="name"/></td>
            <td>${status[agent.value].encodeAsHTML()}</td>
          </tr>
        </g:each>
      </table>
      <g:submitButton class="btn btn-primary" name="Assign Fabric"/>
    </cl:form>
  </g:if>
  <g:else>
    There are currently no unassigned agents.
  </g:else>

  <h1>Assigned agents</h1>
  <table class="table table-bordered table-condensed">
    <tr>
      <th>Agent</th>
      <th>Fabric</th>
      <th>Cleanup Fabric</th>
      <th>Cleanup All</th>
    </tr>
    <g:each in="${assignedAgents}" var="entry">
      <tr>
        <td>${entry.key.encodeAsHTML()}</td>
        <td>${entry.value.encodeAsHTML()}</td>
        <td><cl:link controller="fabric" action="clearAgentFabric" id="${entry.key.encodeAsHTML()}" onClick="return confirm('Are you sure you want to clear the agent-fabric link?');">Fabric</cl:link></td>
        <td><cl:link controller="agents" action="clear" id="${entry.key.encodeAsHTML()}" onClick="return confirm('Are you sure you want to clear ALL knowledge of the agent?');">All</cl:link></td>
      </tr>
    </g:each>
  </table>

</div>
</body>
</html>
