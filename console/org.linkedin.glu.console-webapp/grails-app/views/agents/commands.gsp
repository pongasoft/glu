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
<g:set var="filters" value="[streamsFilter: 'Streams', exitValueFilter: 'Exit', usernameFilter: 'Username', startTimeFilter: 'Start Time', endTimeFilter: 'End Time', durationFilter: 'Duration', actionsFilter: 'Actions']"></g:set>
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
    ${g.remoteFunction(controller: 'commands', action: 'renderHistory', params: [agentId: params.id], update:[success: 'asyncDetails', failure: 'asyncError'], onComplete: 'autoRefresh();')}
  }
  else
  {
    hide('#autoRefreshSpinner');
  }
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
          2&gt;&amp;1 <cl:checkBoxInitFromParams name="redirectStderr" checkedByDefault="${false}"/>
          <g:actionSubmit class="btn primary" action="executeCommand" value="Execute"/>
        </div>
      </fieldset>
    </g:form>
  </div>
</div>

<g:if test="${params.commandId}">
  <div id="included-command"><g:include controller="commands" action="renderCommand" id="${params.commandId}"/></div>
</g:if>

<h3>History: Auto Refresh: <cl:checkBoxInitFromParams name="autoRefresh" id="autoRefresh" onclick="autoRefresh();"/>
    <img src="${resource(dir:'images',file:'spinner.gif')}" alt="Spinner" id="autoRefreshSpinner"/>
<g:each in="${filters}" var="filter">
  |  ${filter.value}: <cl:checkBoxInitFromParams name="${filter.key}" id="${filter.key}" onclick="showHide();"/>
</g:each>
</h3>

<div id="asyncDetails"></div>
<div id="asyncError"></div>

</body>
</html>