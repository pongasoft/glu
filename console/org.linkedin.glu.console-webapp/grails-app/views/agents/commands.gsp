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
    ${g.remoteFunction(controller: 'commands', action: 'renderHistory', params: [agentId: params.id, offset: offset, max: max], update:[success: 'asyncDetails', failure: 'asyncError'], onComplete: 'autoRefresh();')}
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
<ul class="nav nav-tabs">
  <li><g:link controller="agents" action="list">List</g:link></li>
  <li><g:link controller="commands" action="list">All Commands</g:link></li>
  <li><g:link action="view" id="${params.id}">agent [${params.id}]</g:link></li>
  <li><g:link action="plans" id="${params.id}">Plans</g:link></li>
  <li class="active"><a href="#">Commands</a></li>
  <li><g:link action="ps" id="${params.id}">All Processes</g:link></li>
</ul>
<div class="row">
  <div class="span20">
    <g:form class="form-inline" id="${params.id}" action="executeCommand" method="post">
      <fieldset>
        <div class="clearfix">
          <g:textField name="command" value="" class="input-xxlarge"/>
          <label class="checkbox"><cl:checkBoxInitFromParams name="redirectStderr" checkedByDefault="${false}"/> 2&gt;&amp;1 </label>

          <g:actionSubmit class="btn btn-primary" action="executeCommand" value="Execute"/>
        </div>
      </fieldset>
    </g:form>
  </div>
</div>

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
  <g:include controller="commands" action="renderHistory" params="[agentId: params.id, offset: offset, max: max]"/>
</g:else>

</body>
</html>