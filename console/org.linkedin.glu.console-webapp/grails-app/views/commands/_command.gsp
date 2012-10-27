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

<g:if test="${command}">
  <div id="shell-${command.commandId}"><div class="shell span16"><a class="close" href="#" onclick="toggleShowHide('#shell-${command.commandId}')">&times;</a><div class="cli"><span class="prompt">${command.username.encodeAsHTML()}@${command.agent.encodeAsHTML()}#</span>&nbsp;<span class="command">${command.command.encodeAsHTML()}</span> <g:if test="${command.redirectStderr}">2&gt;&amp;1 </g:if><span class="date">[<cl:formatDate time="${command.startTime}"/>]</span> <span class="exitValue">[${'$'}?=${command.exitValue?.encodeAsHTML()}]</span></div><g:if test="${!isCommandRunning}"><g:each in="['stdin', 'stderr', 'stdout']" var="streamType"><g:if test="${command.getTotalBytesCount(streamType) > 0}"><div id="${command.commandId}-${streamType}" class="${streamType}"><cl:renderCommandBytes command="${command}" streamType="${streamType}" onclick="renderCommandStream('${command.commandId}', '${streamType}', '${command.commandId}-${streamType}')"/></div></g:if></g:each><div class="cli"><span class="prompt">${command.username.encodeAsHTML()}@${command.agent.encodeAsHTML()}#</span>&nbsp;<span class="date">[<cl:formatDate time="${command.endTime}"/>]</span></div></g:if><g:else><img src="${resource(dir:'images',file:'spinner.gif')}" alt="Spinner" /></g:else></div><g:if test="${isCommandRunning}"><g:javascript>setTimeout("renderCommand('${command.commandId}', 'shell-${command.commandId}')", '${params.refreshRate ?: 2000}')</g:javascript></g:if></div>
</g:if>
