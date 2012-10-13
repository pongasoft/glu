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
    pre {
      padding: 1px;
      margin: 0;
    }
  </style>
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
  <div class="span12">
    <g:form class="form-stacked" id="${params.id}" action="executeCommand" method="post">
      <fieldset>
        <div class="clearfix">
          <g:textField name="command" value="" class="xxlarge"/>
          <g:actionSubmit class="btn primary" action="executeCommand" value="Execute"/>
        </div>
      </fieldset>
    </g:form>
  </div>
</div>

<div class="commands">
  <table class="bordered-table xtight-table">
    <thead>
    <tr>
      <th class="commandFilter">Command</th>
      <th class="stdinFilter">Stdin</th>
      <th class="stdoutFilter">Stdout</th>
      <th class="stderrFilter">Stderr</th>
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
        <td class="commandFilter">${ce.command.encodeAsHTML()}</td>
        <td class="stdinFilter"><g:if test="${ce.stdinFirstBytes}"><pre>${ce.stdinFirstBytes?.encodeAsHTML()}</pre></g:if></td>
        <td class="stdoutFilter"><g:if test="${ce.stdoutFirstBytes}"><pre>${ce.stdoutFirstBytes?.encodeAsHTML()}</pre></g:if></td>
        <td class="stderrFilter"><g:if test="${ce.stderrFirstBytes}"><pre>${ce.stderrFirstBytes?.encodeAsHTML()}</pre></g:if></td>
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