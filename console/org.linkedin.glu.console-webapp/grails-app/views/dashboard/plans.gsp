%{--
  - Copyright (c) 2011-2013 Yan Pujante
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
  <title>Plans - Dashboard</title>
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
<ul class="nav nav-tabs">
  <li><cl:link action="delta">${request.userSession.currentCustomDeltaDefinitionName.encodeAsHTML()}</cl:link></li>
  <li class="active"><a href="#">Plans</a></li>
  <li><cl:link controller="dashboard" action="customize">Customize</cl:link></li>
</ul>
<g:if test="${request.system}">
  <g:render template="/plan/selectPlan" model="[title: title, hasDelta: hasDelta]"/>
</g:if>
<g:else>
  <h2>No System selected.</h2>
</g:else>
</body>
</html>
