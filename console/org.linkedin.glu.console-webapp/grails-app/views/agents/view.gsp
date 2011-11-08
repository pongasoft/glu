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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>Agent [${model.agent?.agentName}]</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'agents-view.css')}"/>
  <script type="text/javascript" src="${resource(dir:'js',file:'console_jquery.js')}"></script>
</head>
<body>
<g:if test="${model}">
  <ul class="tabs">
    <li><g:link controller="agents" action="list">List</g:link></li>
    <li class="active"><a href="#">agent [${model.agent.agentName}]</a></li>
    <li><g:link action="plans" id="${model.agent.agentName}">Plans</g:link></li>
    <li><g:link action="ps" id="${model.agent.agentName}">All Processes</g:link></li>
  </ul>
  &nbsp;
  <h1 class="${model.state}">${model.agent.agentName} (V${model.agent.version}) <cl:renderTags tags="${model.agent.tags}" linkable="${true}"/> </h1>
  <ul class="summary">
    <li>Logs: <g:link action="tailLog" id="${model.agent.agentName}" params="[maxLine: 500]">main</g:link> |
      <g:link action="tailLog" id="${model.agent.agentName}" params="[log:'gc.log', maxLine: 500]">gc</g:link><g:if test="${model.agent.agentProperties['glu.agent.logDir']}"> |
      <g:link action="fileContent" id="${model.agent.agentName}" params="[location: model.agent.agentProperties['glu.agent.logDir']]">more...</g:link></g:if>
    </li>
    <li>
      <a class="btn" data-controls-modal="agent-details" data-backdrop="true" data-keyboard="true">View Details</a>
      <g:link class="btn ${hasDelta ? 'danger' : ''}" controller="agents" action="plans" id="${model.agent.agentName}">Deploy</g:link>
      <cl:linkToPs class="btn" agent="${model.agent.agentName}" pid="${model.agent.agentProperties['glu.agent.pid']}">ps</cl:linkToPs>
      <g:link class="btn" action="sync" id="${model.agent.agentName}">ZooKeeper Sync</g:link></li>
  </ul>
  <div id="agent-details" class="modal hide">
    <a href="#" class="close">&times;</a>
    <div class="modal-header">Agent Details</div>
    <div class="modal-body">
     <cl:mapToTable map="${model.agent.agentProperties.findAll { !it.key.startsWith('java.') }}"/>
    </div>
  </div>

  <g:each in="${ConsoleUtils.sortBy(model.mountPoints.keySet(), 'path')}" var="key" status="idx">
    <g:set var="mountPoint" value="${model.mountPoints[key]}"/>
    <a name="${mountPoint.mountPoint}" id="${mountPoint.mountPoint}"></a>
    <h2 class="${cl.mountPointState(mountPoint: mountPoint)}"><cl:linkToFilteredDashboard systemFilter="mountPoint='${key}'" groupBy="mountPoint">${key.encodeAsHTML()}</cl:linkToFilteredDashboard> <cl:renderTags tags="${mountPoint.tags}" linkable="${true}"/>
    </h2>
    <ul class="summary">
      <cl:mountPointLogs agent="${model.agent.agentName}" mountPoint="${mountPoint}"/>
      <li><a href="#" class="btn" onclick="toggleShowHide('#mountPoint-details-${idx}');return false;">View Details</a>
      <g:set var="actions" value="${model.actions[key]}"/>

      <g:each in="${actions}" var="action">
        <a class="btn" href="${action.key}">${action.value}</a>
      </g:each>

      <g:link class="btn" controller="agents" action="forceUninstallScript" id="${model.agent.agentName}" params="[mountPoint: mountPoint.mountPoint]" onClick="return confirm('Are you sure you want to execute Force Uninstall?');">Force Uninstall</g:link>

      </li>
    </ul>
    <div id="mountPoint-details-${idx}" class="hidden">
    <cl:mapToUL map="${mountPoint.data}" specialKeys="${['error']}" var="specialEntry">
      <li class="error">
        <dl>
          <dt>${specialEntry.key}: <g:link controller="agents" action="clearError" id="${model.agent.agentName}" params="[mountPoint: mountPoint.mountPoint]">Clear error</g:link></dt>
          <g:if test="${specialEntry.value instanceof String}">
             <dd>
               <dl>${specialEntry.value.encodeAsHTML()}</dl>
             </dd>
          </g:if>
          <g:else>
            <dd>
              <dl><g:remoteLink update="stackTraceBody-${idx}" action="fullStackTrace" id="${model.agent.agentName}" params="[mountPoint: mountPoint.mountPoint]">View Full Stack Trace</g:remoteLink></dl>
            </dd>
            <dd class="errorStackTrace">
              <dl id="stackTraceBody-${idx}">
                <g:each in="${specialEntry.value}" var="ste">
                  <dt class="stackTraceHeader">* ${ste.name.encodeAsHTML()}: "${ste.message?.encodeAsHTML()}"</dt>
                </g:each>
              </dl>
            </dd>
          </g:else>
        </dl>
    </cl:mapToUL>
    </div>
  </g:each>
</g:if>
<g:else>
  <ul class="tabs">
    <li><g:link controller="agents" action="list">List</g:link></li>
    <li class="active"><a href="#">agent [${params.id}]</a></li>
  </ul>
  <h2>No such agent ${params.id} [<g:link controller="fabric" action="listAgentFabrics">Fix it</g:link>]</h2>
</g:else>
</body>
</html>