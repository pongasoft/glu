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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<g:set var="filters" value="[streamsFilter: 'Streams', exitValueFilter: 'Exit', usernameFilter: 'Username', startTimeFilter: 'Start Time', completionTimeFilter: 'End Time', durationFilter: 'Duration', actionsFilter: 'Actions']"></g:set>
<head>
  <title>Commands - Agent [${params.id}]</title>
  <meta name="layout" content="main"/>
  <style type="text/css">
  .paginateButtons {
    padding-top: 0.5em;
    padding-bottom: 0.5em;
  }
  .progress {
    margin: 0;
  }
  </style>
<g:render template="/commands/command_js"/>
<g:set var="offset" value="${params.offset ?: '0'}"/>
<g:set var="max" value="${params.max ?: '25'}"/>
<g:set var="isFirstPage" value="${offset == '0'}"/>
<g:javascript>
function shouldRefresh()
{
  return document.getElementById('autoRefresh').checked;
}
function autoRefresh()
{
  if(shouldRefresh())
  {
    setTimeout('refreshHistory()', ${params.refreshRate ?: '2000'});
    show('#autoRefreshSpinner');
    showHide();
  }
  else
  {
    hide('#autoRefreshSpinner');
  }
}
function refreshHistory()
{
  ${cl.remoteFunction(controller: 'commands', action: 'renderHistory', params: [agentId: params.id, offset: offset, max: max], update:[success: 'asyncDetailsHistory', failure: 'asyncErrorHistory'], onComplete: 'autoRefresh();')}
}
function refreshCommand()
{
  <g:if test="${params.commandId}">
    ${cl.remoteFunction(controller: 'commands', action: 'renderCommand', id: params.commandId, update:[success: 'asyncDetailsCommand', failure: 'asyncErrorCommand'])}
  </g:if>
}
function refresh()
{
  refreshCommand();
  refreshHistory();
}
function showHide()
{
  <g:each in="${filters.keySet()}" var="filter">
    toggleClass('#history .${filter}', !document.getElementById('${filter}').checked, 'hidden');
  </g:each>
}
</g:javascript>
</head>
<body onload="refresh();">
<ul class="nav nav-tabs">
  <li><cl:link controller="agents" action="list">List</cl:link></li>
  <li><cl:link controller="commands" action="list">All Commands</cl:link></li>
  <li><cl:link action="view" id="${params.id}">agent [${params.id}]</cl:link></li>
  <li><cl:link action="plans" id="${params.id}">Plans</cl:link></li>
  <li class="active"><a href="#">Commands</a></li>
  <li><cl:link action="ps" id="${params.id}">All Processes</cl:link></li>
</ul>
<div class="row">
  <div class="span20">
    <cl:form class="form-inline" id="${params.id}" action="executeCommand" method="post">
      <fieldset>
        <div class="clearfix">
          <g:textField name="command" value="" class="input-xxlarge"/>
          <label class="checkbox"><cl:checkBoxInitFromParams name="redirectStderr" checkedByDefault="${false}"/> 2&gt;&amp;1 </label>

          <g:actionSubmit class="btn btn-primary" action="executeCommand" value="Execute"/>
        </div>
      </fieldset>
    </cl:form>
  </div>
</div>

<g:if test="${params.commandId}">
  <div>
    <div id="asyncDetailsCommand"></div>
    <div id="asyncErrorCommand"></div>
  </div>
</g:if>

<h4>Auto Refresh: <g:if test="${isFirstPage}"><cl:checkBoxInitFromParams name="autoRefresh" id="autoRefresh" onclick="autoRefresh();"/>
    <img src="${resource(dir:'images',file:'spinner.gif')}" alt="Spinner" id="autoRefreshSpinner"/></g:if><g:else><g:checkBox name="autoRefresh" id="autoRefresh" disabled="true" checked="false"/></g:else>
<g:each in="${filters}" var="filter">
  |  ${filter.value}: <cl:checkBoxInitFromParams name="${filter.key}" id="${filter.key}" onclick="showHide();"/>
</g:each>
</h4>

<div id="asyncDetailsHistory"></div>
<div id="asyncErrorHistory"></div>

</body>
</html>