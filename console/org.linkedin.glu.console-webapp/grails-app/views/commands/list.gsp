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
<g:set var="filters" value="[agentFilter: 'Agent', streamsFilter: 'Streams', exitValueFilter: 'Exit', usernameFilter: 'Username', startTimeFilter: 'Start Time', completionTimeFilter: 'End Time', durationFilter: 'Duration', actionsFilter: 'Actions']"></g:set>
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
  .progress {
    margin: 0;
  }
  </style>
<g:set var="offset" value="${params.offset ?: '0'}"/>
<g:set var="max" value="${params.max ?: '25'}"/>
<g:set var="isFirstPage" value="${offset == '0'}"/>
<g:render template="/commands/command_js"/>
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
    toggleClass('#filter-autoRefresh', true, 'badge-success');
    showHide();
  }
  else
  {
    hide('#autoRefreshSpinner');
    toggleClass('#filter-autoRefresh', false, 'badge-success');
  }
}
function refreshHistory()
{
  ${cl.remoteFunction(controller: 'commands', action: 'renderHistory', params: [offset: offset, max: max], update:[success: 'asyncDetailsHistory', failure: 'asyncErrorHistory'], onComplete: 'autoRefresh();')}
}
function refresh()
{
  refreshHistory();
}
function showHide()
{
  <g:each in="${filters.keySet()}" var="filter">
    toggleClass('#history .${filter}', !document.getElementById('${filter}').checked, 'hidden');
    toggleClass('#filter-${filter}', document.getElementById('${filter}').checked, 'badge-success');
  </g:each>
}
</g:javascript>
</head>
<body onload="refresh();">
<ul class="nav nav-tabs">
  <li><cl:link controller="agents" action="list">List</cl:link></li>
  <li class="active"><a href="#">All Commands</a></li>
</ul>

<g:if test="${params.commandId}">
  <div><g:include controller="commands" action="renderCommand" id="${params.commandId}"/></div>
</g:if>

<ul class="column-filters">
  <li><label class="badge" id="filter-autoRefresh"><g:if test="${isFirstPage}"><cl:checkBoxInitFromParams name="autoRefresh" id="autoRefresh" onclick="autoRefresh();"/> Auto Refresh
      <img src="${resource(dir:'images',file:'spinner.gif')}" alt="Spinner" id="autoRefreshSpinner"/></g:if><g:else><g:checkBox name="autoRefresh" id="autoRefresh" disabled="true" checked="false"/> Auto Refresh</g:else></label></li>
  <g:each in="${filters}" var="filter">
    <li><label id="filter-${filter.key}" class="badge"><cl:checkBoxInitFromParams name="${filter.key}" id="${filter.key}" onclick="showHide();"/> ${filter.value}</label></li>
  </g:each>
</ul>

<div id="asyncDetailsHistory"></div>
<div id="asyncErrorHistory"></div>

</body>
</html>