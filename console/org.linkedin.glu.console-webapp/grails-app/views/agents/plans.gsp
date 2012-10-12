%{--
  - Copyright (c) 2011 Yan Pujante
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
<head>
  <title>Plans - agent [${params.id}]</title>
  <meta name="layout" content="main"/>
  <style type="text/css">
  ul {
    padding: 0;
    margin: 0;
  }
  li {
   list-style: none;
  }
  input.stepCheckBox {
    display: none;
  }
  input.quickSelect {
    display: none;
  }
  </style>
</head>
<body>
<g:if test="${agent}">
  <ul class="tabs">
    <li><g:link controller="agents" action="list">List</g:link></li>
    <li><g:link action="view" id="${params.id}">agent [${params.id}]</g:link></li>
    <li class="active"><a href="#">Plans</a></li>
    <li><g:link action="commands" id="${params.id}">Commands</g:link></li>
    <li><g:link action="ps" id="${params.id}">All Processes</g:link></li>
  </ul>
  <g:render template="/plan/selectPlan" model="[title: title, hasDelta: hasDelta]"/>
</g:if>
<g:else>
  <ul class="tabs">
    <li><g:link controller="agents" action="list">List</g:link></li>
    <li class="active"><a href="#">agent [${params.id}]</a></li>
  </ul>
  <h2>No such agent ${params.id} [<g:link controller="fabric" action="listAgentFabrics">Fix it</g:link>]</h2>
</g:else>
</body>
</html>
