%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
  - Copyright (c) 2011 Yan Pujante
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
  <title>GLU Console - Agent: ${model.agent?.agentName}</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'agents-view.css')}"/>
  <script type="text/javascript" src="${resource(dir:'js',file:'console_yui.js')}"></script>
</head>
<body>
<g:if test="${model}">
  <ul class="submenu">
    <li><g:link controller="dashboard">Dashboard</g:link></li>
    <li><g:link controller="system" action="list">System</g:link></li>
    <li class="selected">agent [${model.agent.agentName}]</li>
    <li><g:link action="ps" id="${model.agent.agentName}">All Processes</g:link></li>
  </ul>
  <h1 class="${model.state}">${model.agent.agentName} (V${model.agent.agentProperties['glu.agent.version']})</h1>
  <ul class="summary">
    <li>Logs: <g:link action="tailLog" id="${model.agent.agentName}" params="[maxLine: 500]">main</g:link> |
      <g:link action="tailLog" id="${model.agent.agentName}" params="[log:'gc.log', maxLine: 500]">gc</g:link><g:if test="${model.agent.agentProperties['glu.agent.logDir']}"> |
      <g:link action="fileContent" id="${model.agent.agentName}" params="[location: model.agent.agentProperties['glu.agent.logDir']]">more...</g:link></g:if>
    </li>
    <li>Actions: <cl:linkToPs agent="${model.agent.agentName}" pid="${model.agent.agentProperties['glu.agent.pid']}">ps</cl:linkToPs>
      | <g:link action="sync" id="${model.agent.agentName}">ZooKeeper Sync</g:link></li>
    <li><a href="#" onclick="toggleShowHide('agent-details');return false;">View Details</a></li>
    %{--TODO MED YP: experimental: <li><a href="#" onclick="toggleShowHide('add-entry');return false;">Add Entry</a></li>--}%
  </ul>
  <div id="agent-details" class="hidden">
    <cl:mapToTable map="${model.agent.agentProperties.findAll { !it.key.startsWith('java.') }}"/>
  </div>

  <div id="add-entry" class="hidden">
    <h2>Add Entry</h2>
    <g:form action="addEntry" controller="system" method="post">
      <g:hiddenField name="agent" value="${model.agent.agentName}"/>
      <table>
        <tbody>
        <tr>
          <td>MountPoint:</td>
          <td><g:textField id="mountPoint" name="mountPoint"/></td>
        </tr>
        <tr>
          <td>GLU Script (URI):</td>
          <td><g:textField id="script" name="script" size="100"/></td>
        </tr>
        <tr>
          <td></td>
          <td>
            <div class="buttons">
              <span class="button"><input type="submit" value="Add Entry" /></span>
            </div>
          </td>
        </tr>
        </tbody>
      </table>
    </g:form>
  </div>

  <g:each in="${ConsoleUtils.sortBy(model.mountPoints.keySet(), 'path')}" var="key">
    <g:set var="mountPoint" value="${model.mountPoints[key]}"/>
    <a name="${mountPoint.mountPoint}" id="${mountPoint.mountPoint}"></a>
    <h2 class="${cl.mountPointState(mountPoint: mountPoint)}"><cl:linkToSystemFilter name="mountPoint" value="${key}">${key.encodeAsHTML()}</cl:linkToSystemFilter>
    </h2>
    <ul class="summary">
      <cl:mountPointLogs agent="${model.agent.agentName}" mountPoint="${mountPoint}"/>
      <li>Actions:
      <g:set var="actions" value="${model.actions[key]}"/>

      <g:each in="${actions}" var="action">
        <a href="${action.key}">${action.value}</a> |
      </g:each>

      <g:link controller="agents" action="forceUninstallScript" id="${model.agent.agentName}" params="[mountPoint: mountPoint.mountPoint]" onClick="return confirm('Are you sure you want to execute Force Uninstall?');">Force Uninstall</g:link>

      </li>
      <li><a href="#" onclick="toggleShowHide('mountPoint-details-${mountPoint.mountPoint}');return false;">View Details</a></li>
    </ul>
    <div id="mountPoint-details-${mountPoint.mountPoint}" class="hidden">
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
              <dl><g:remoteLink update="stackTraceBody_${mountPoint.mountPoint}" action="fullStackTrace" id="${model.agent.agentName}" params="[mountPoint: mountPoint.mountPoint]">View Full Stack Trace</g:remoteLink></dl>
            </dd>
            <dd class="errorStackTrace">
              <dl id="stackTraceBody_${mountPoint.mountPoint}">
                <g:each in="${specialEntry.value}" var="ste">
                  <dt class="stackTraceHeader">* ${ste.name.encodeAsHTML()}: "${ste.message.encodeAsHTML()}"</dt>
                </g:each>
              </dl>
            </dd>
          </g:else>
        </dl>
    </cl:mapToUL>
    </div>
  </g:each>

  <g:render template="/plan/selectDelta" model="[delta: delta, title: 'Delta: ' + title]"/>

  <g:render template="/plan/selectDelta" model="[delta: bounce, title: 'Bounce: ' + title]"/>

  <g:render template="/plan/selectDelta" model="[delta: redeploy, title: 'Redeploy: ' + title]"/>

</g:if>
<g:else>
  <h2>No such agent ${params.id} [<g:link controller="fabric" action="listAgentFabrics">Fix it</g:link>]</h2>
</g:else>
</body>
</html>