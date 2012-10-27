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



<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<g:set var="filters" value="[agentFilter: 'Agent', streamsFilter: 'Streams', exitValueFilter: 'Exit', usernameFilter: 'Username', startTimeFilter: 'Start Time', endTimeFilter: 'End Time', durationFilter: 'Duration', actionsFilter: 'Actions']"></g:set>
<head>
  <title>Commands - Agent [${params.id}]</title>
  <meta name="layout" content="main"/>
  <style type="text/css">
  .paginateButtons {
    padding-top: 0.5em;
    padding-bottom: 0.5em;
  }
  .shortcut {
    float: right;
  }
  </style>
<g:set var="offset" value="${params.offset ?: '0'}"/>
<g:set var="max" value="${params.max ?: '25'}"/>
<g:set var="isFirstPage" value="${offset == '0'}"/>
<g:render template="/commands/command_js"/>
<g:javascript>
<g:if test="${isFirstPage}">
function shouldRefresh()
{
  return document.getElementById('autoRefresh').checked;
}
function autoRefresh()
{
  if(shouldRefresh())
  {
    setTimeout('refresh()', ${params.refreshRate ?: '2000'});
    show('#autoRefreshSpinner');
    showHide();
  }
  else
  {
    hide('#autoRefreshSpinner');
  }
}
function refresh()
{
  if(shouldRefresh())
  {
    ${g.remoteFunction(controller: 'commands', action: 'renderHistory', params: [offset: offset, max: max], update:[success: 'asyncDetails', failure: 'asyncError'], onComplete: 'autoRefresh();')}
  }
  else
  {
    hide('#autoRefreshSpinner');
  }
}
</g:if>
function showHide()
{
  <g:each in="${filters.keySet()}" var="filter">
    toggleClass('#history .${filter}', !document.getElementById('${filter}').checked, 'hidden');
  </g:each>
}
</g:javascript>
</head>
<body onload="refresh();">
<ul class="tabs">
  <li><g:link controller="agents" action="list">List</g:link></li>
  <li class="active"><a href="#">All Commands</a></li>
</ul>

<g:if test="${params.commandId}">
  <div><g:include controller="commands" action="renderCommand" id="${params.commandId}"/></div>
</g:if>

<h4><g:if test="${isFirstPage}">Auto Refresh: <cl:checkBoxInitFromParams name="autoRefresh" id="autoRefresh" onclick="autoRefresh();"/>
    <img src="${resource(dir:'images',file:'spinner.gif')}" alt="Spinner" id="autoRefreshSpinner"/></g:if>
<g:each in="${filters}" var="filter">
  |  ${filter.value}: <cl:checkBoxInitFromParams name="${filter.key}" id="${filter.key}" onclick="showHide();"/>
</g:each>
</h4>
<g:if test="${isFirstPage}">
<div id="asyncDetails"></div>
<div id="asyncError"></div>
</g:if>
<g:else>
  <g:include controller="commands" action="renderHistory" params="[offset: offset, max: max]"/>
</g:else>

</body>
</html>