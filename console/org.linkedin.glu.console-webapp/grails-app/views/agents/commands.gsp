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

<%@ page import="org.linkedin.glu.agent.api.Agent" contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>Commands - Agent [${params.id}]</title>
  <meta name="layout" content="main"/>
  <style type="text/css">
    .shell {
      white-space: pre-wrap;
      word-wrap: break-word;
      background-color: #000080;
      color: #ffff00;
      font-family: Monaco, Andale Mono, Courier New, monospace;
      font-size: 12px;
      padding: 2px;
    }

    .shell .cli {
      font-size: 120%;
    }

    .shell .stderr {
      color: #ffaaaa;
    }

    .shell .stdin {
      color: #aaffaa;
    }

    .shell .date, .shell .exitValue {
      color: #aaaaaa;
    }

    .shell .close {
      color: #ffff00;
    }

  </style>
<g:javascript>
function renderCommandStream(commandId, streamType, success)
{
%{--YP Implementation note: this is obviously hacky, but there does not seem to be a way to use g.remoteFunction + javascript variables in all places!

  ${g.remoteFunction(controller: 'commands', id: <javascript commandId>, action: 'stream', params: [streamType: <javascript streamType>], update:[success: '<javascript variable>'])}

--}%
  jQuery.ajax({type:'GET',data:{'streamType': streamType}, url:'/console/commands/' + commandId + '/stream',success:function(data,textStatus){jQuery('#' + success).html(data);},error:function(XMLHttpRequest,textStatus,errorThrown){}});
}
</g:javascript>
</head>
<body>
<ul class="tabs">
  <li><g:link controller="agents" action="list">List</g:link></li>
  <li><g:link action="view" id="${params.id}">agent [${params.id}]</g:link></li>
  <li><g:link action="plans" id="${params.id}">Plans</g:link></li>
  <li class="active"><a href="#">Commands</a></li>
  <li><g:link action="ps" id="${params.id}">All Processes</g:link></li>
</ul>
<div class="row">
  <div class="span20">
    <g:form class="form-stacked" id="${params.id}" action="executeCommand" method="post">
      <fieldset>
        <div class="clearfix">
          <g:textField name="command" value="" class="xxlarge"/>
          2&gt;&amp;1: <cl:checkBoxInitFromParams name="redirectStderr" checkedByDefault="true"/>
          <g:actionSubmit class="btn primary" action="executeCommand" value="Execute"/>
        </div>
      </fieldset>
    </g:form>
  </div>
</div>

<g:if test="${command}">
  <div id="shell-${command.commandId}" class="shell span16"><a class="close" href="#" onclick="toggleShowHide('#shell-${command.commandId}')">&times;</a><div class="cli"><span class="prompt">${command.username.encodeAsHTML()}@${command.agent.encodeAsHTML()}#</span>&nbsp;<span class="command">${command.command.encodeAsHTML()}</span> <span class="date">[<cl:formatDate time="${command.startTime}"/>]</span> <span class="exitValue">[${'$'}?=${command.exitValue?.encodeAsHTML()}]</span></div><g:each in="['stdin', 'stderr', 'stdout']" var="streamType"><g:if test="${command.getTotalBytesCount(streamType) > 0}"><div id="${command.commandId}-${streamType}" class="${streamType}"><cl:renderCommandBytes command="${command}" streamType="${streamType}" onclick="renderCommandStream('${command.commandId}', '${streamType}', '${command.commandId}-${streamType}')"/></div></g:if></g:each><div class="cli"><span class="prompt">${command.username.encodeAsHTML()}@${command.agent.encodeAsHTML()}#</span>&nbsp;<span class="date">[<cl:formatDate time="${command.endTime}"/>]</span></div></div>
</g:if>

<h2>History</h2>
<div class="commands">
  <table class="bordered-table xtight-table">
    <thead>
    <tr>
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
    <g:each in="${commands}" var="ce">
      <tr>
        <td class="commandFilter"><g:link controller="agents" action="commands" id="${params.id}" params="[commandId: ce.commandId]">${ce.command.encodeAsHTML()}</g:link></td>
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
</div>
</body>
</html>