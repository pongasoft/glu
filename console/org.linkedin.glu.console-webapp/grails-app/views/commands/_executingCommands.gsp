%{--
  - Copyright (c) 2012-2013 Yan Pujante
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
<g:if test="${commandExecutions}">
  <div id="history-current-command-executions">
    <table class="table table-bordered table-condensed">
      <thead>
      <tr>
        <g:if test="${!params.agentId}"><th class="agentFilter">Agent</th></g:if>
        <th class="commandFilter">Command</th>
        <th class="streamsFilter">Streams</th>
        <th class="usernameFilter">User</th>
        <th class="startTimeFilter">Start Time</th>
        <th class="durationFilter">Dur.</th>
        <th>Progress</th>
        <th>Actions</th>
      </tr>
      </thead>
      <tbody>
      <g:each in="${commandExecutions}" var="ce">
        <tr>
          <g:if test="${!params.agentId}"><td class="agentFilter"><cl:link controller="agents" action="commands" id="${ce.agent}">${ce.agent.encodeAsHTML()}</cl:link><cl:link controller="agents" action="view" id="${ce.agent}"><img class="shortcut" src="${g.resource(dir: 'images', file: 'magnifier.png')}" alt="view agent"/></cl:link></td></g:if>
          <td class="commandFilter"><cl:link controller="agents" action="commands" id="${ce.agent}" params="[commandId: ce.commandId]" title="${ce.commandId.encodeAsHTML()}">${ce.command.encodeAsHTML()}</cl:link></td>
          <td class="streamsFilter shell"><g:each in="['stdin', 'stderr', 'stdout']" var="streamType"><div class="${streamType}"><cl:renderCommandBytes command="${ce}" streamType="${streamType}"/></div></g:each></td>
          <td class="usernameFilter">${ce.username.encodeAsHTML()}</td>
          <td class="startTimeFilter"><cl:formatDate time="${ce.startTime}"/></td>
          <td class="durationFilter"><cl:formatDuration time="${ce.startTime}"/></td>
          <td>
            <div class="progress progress-striped active">
              <div class="bar" style="width: 100%"></div>
            </div>
          </td>
          <td><cl:link controller="agents" action="interruptCommand" id="${ce.agent}" params="[commandId: ce.commandId]">Interrupt</cl:link></td>
        </tr>
      </g:each>
      </tbody>
    </table>
  </div>
</g:if>
