%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
  - Portions Copyright (c) 2011-2014 Yan Pujante
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

<%@ page import="org.linkedin.glu.groovy.utils.jvm.JVMInfo" contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>Agent [${model.agent?.agentName}]</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'agents-view.css')}"/>
</head>
<body>
<g:if test="${model}">
  <ul class="nav nav-tabs">
    <li><cl:link controller="agents" action="list">List</cl:link></li>
    <cl:whenFeatureEnabled feature="commands"><li><cl:link controller="commands" action="list">All Commands</cl:link></li></cl:whenFeatureEnabled>
    <li class="active"><a href="#">agent [${model.agent.agentName}]</a></li>
    <li><cl:link action="plans" id="${model.agent.agentName}">Plans</cl:link></li>
    <cl:whenFeatureEnabled feature="commands"><li><cl:link action="commands" id="${model.agent.agentName}">Commands</cl:link></li></cl:whenFeatureEnabled>
    <li><cl:link action="ps" id="${model.agent.agentName}">All Processes</cl:link></li>
  </ul>
  &nbsp;
  <h2 class="${model.state}">${model.agent.agentName} (V${model.agent.version}) <cl:renderTags tags="${model.agent.tags}" linkable="${true}"/> </h2>
  <ul class="summary">
    <li>Logs: <cl:link action="fileContent" id="${model.agent.agentName}" params="[file: model.agent.agentProperties['glu.agent.logDir'] + '/org.linkedin.glu.agent-server.out']">main</cl:link> |
    <cl:link action="fileContent" id="${model.agent.agentName}" params="[file: model.agent.agentProperties['glu.agent.logDir'] + '/gc.log']">gc</cl:link><g:if test="${model.agent.agentProperties['glu.agent.logDir']}"> |
      <cl:link action="fileContent" id="${model.agent.agentName}" params="[location: model.agent.agentProperties['glu.agent.logDir']]">more...</cl:link></g:if>
    </li>
    <li>
      <a href="#" class="btn" onclick="toggleShowHide('#agent-details');return false;">View Details</a>
      <cl:link class="btn ${hasDelta ? 'danger' : ''}" controller="agents" action="plans" id="${model.agent.agentName}">Deploy</cl:link>
      <cl:linkToPs class="btn" agent="${model.agent.agentName}" pid="${model.agent.agentProperties['glu.agent.pid']}">ps</cl:linkToPs>
      <cl:link class="btn" action="sync" id="${model.agent.agentName}">ZooKeeper Sync</cl:link></li>
  </ul>
  <div id="agent-details" class="hidden">
    <h4>Agent</h4>
    <cl:mapToTable class="table table-bordered xtight-table noFullWidth" map="${model.agent.agentProperties.findAll { !it.key.startsWith('java.') }}"/>
    <h4>JVM Info (Agent)</h4>
    <cl:mapToTable class="table table-bordered xtight-table noFullWidth" map="${JVMInfo.getJVMInfo(model.agent.agentProperties) }"/>
  </div>

  <g:each in="${ConsoleUtils.sortBy(model.mountPoints.keySet(), 'path')}" var="key" status="idx">
    <g:set var="mountPoint" value="${model.mountPoints[key]}"/>
    <a name="${mountPoint.mountPoint}" id="${mountPoint.mountPoint}"></a>
    <h3 class="${cl.mountPointState(mountPoint: mountPoint)}"><g:if test="${mountPoint.isCommand()}"><cl:link action="commands" id="${model.agent.agentName}" params="[commandId: mountPoint.mountPoint.name]">${key.encodeAsHTML()}</cl:link></g:if><g:else><cl:linkToFilteredDashboard systemFilter="mountPoint='${key}'" groupBy="mountPoint">${key.encodeAsHTML()}</cl:linkToFilteredDashboard></g:else> <cl:renderTags tags="${mountPoint.tags}" linkable="${true}"/>
    </h3>
    <ul class="summary">
      <cl:mountPointLogs agent="${model.agent.agentName}" mountPoint="${mountPoint}"/>
      <cl:mountPointProcesses agent="${model.agent.agentName}" mountPoint="${mountPoint}"/>
      <li><a href="#" class="btn" onclick="toggleShowHide('#mountPoint-details-${idx}');return false;">View Details</a>
      <g:set var="actions" value="${model.actions[key]}"/>

      <g:each in="${actions}" var="action">
        <a class="btn" href="${action.key}">${action.value}</a>
      </g:each>

      <cl:link class="btn" controller="agents" action="forceUninstallScript" id="${model.agent.agentName}" params="[mountPoint: mountPoint.mountPoint]" onClick="return confirm('Are you sure you want to execute Force Uninstall?');">Force Uninstall</cl:link>

      </li>
    </ul>
    <div id="mountPoint-details-${idx}" class="hidden">
    <cl:mapToUL map="${mountPoint.data}" specialKeys="${['error']}" var="specialEntry">
      <li class="error">
        <dl>
          <dt>${specialEntry.key}: <cl:link controller="agents" action="clearError" id="${model.agent.agentName}" params="[mountPoint: mountPoint.mountPoint]">Clear error</cl:link></dt>
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
  <ul class="nav nav-tabs">
    <li><cl:link controller="agents" action="list">List</cl:link></li>
    <li class="active"><a href="#">agent [${params.id}]</a></li>
    <cl:whenFeatureEnabled feature="commands"><li><cl:link action="commands" id="${params.id}">Commands</cl:link></li></cl:whenFeatureEnabled>
  </ul>
  <h3>No such agent ${params.id} [<cl:link controller="fabric" action="listAgentFabrics">Fix it</cl:link>]</h3>
</g:else>
</body>
</html>