%{--
  - Copyright (c) 2012 Yan Pujante
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
<div id="history">
  <div class="paginateButtons">
    <g:if test="${params.agentId}">
      <g:paginate total="${count}" controller="agents" action="commands" id="${params.agentId}"/>
    </g:if>
    <g:else>
      <g:paginate total="${count}" controller="commands" action="list"/>
    </g:else>
  </div>
  <table class="bordered-table xtight-table">
    <thead>
    <tr>
      <g:if test="${!params.agentId}"><th class="agentFilter">Agent</th></g:if>
      <th class="commandFilter">Command</th>
      <th class="streamsFilter">Streams</th>
      <th class="exitValueFilter">Exit</th>
      <th class="usernameFilter">User</th>
      <th class="startTimeFilter">Start Time</th>
      <th class="endTimeFilter">End Time</th>
      <th class="durationFilter">Dur.</th>
    </tr>
    </thead>
    <tbody>
    <g:each in="${commandExecutions}" var="ce">
      <tr>
        <g:if test="${!params.agentId}"><td class="agentFilter"><g:link controller="agents" action="commands" id="${ce.agent}">${ce.agent.encodeAsHTML()}</g:link><g:link controller="agents" action="view" id="${ce.agent}"><img class="shortcut" src="${g.resource(dir: 'images', file: 'magnifier.png')}" alt="view agent"/></g:link></td></g:if>
        <td class="commandFilter"><g:link controller="agents" action="commands" id="${ce.agent}" params="[commandId: ce.commandId]" title="${ce.commandId.encodeAsHTML()}">${ce.command.encodeAsHTML()}</g:link></td>
        <td class="streamsFilter shell"><g:each in="['stdin', 'stderr', 'stdout']" var="streamType"><div class="${streamType}"><cl:renderCommandBytes command="${ce}" streamType="${streamType}"/></div></g:each></td>
        <td class="exitValueFilter">${ce.exitValue?.encodeAsHTML()}</td>
        <td class="usernameFilter">${ce.username.encodeAsHTML()}</td>
        <td class="startTimeFilter"><cl:formatDate time="${ce.startTime}"/></td>
        <td class="endTimeFilter"><cl:formatDate time="${ce.endTime}"/></td>
        <td class="durationFilter"><cl:formatDuration duration="${ce.duration}"/></td>
      </tr>
    </g:each>
    </tbody>
  </table>
  <div class="paginateButtons">
    <g:if test="${params.agentId}">
      <g:paginate total="${count}" controller="agents" action="commands" id="${params.agentId}"/>
    </g:if>
    <g:else>
      <g:paginate total="${count}" controller="commands" action="list"/>
    </g:else>
  </div>
</div>
