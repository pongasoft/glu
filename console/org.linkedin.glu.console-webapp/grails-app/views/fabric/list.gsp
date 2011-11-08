%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
  - Portions Copyright (c) 2011 Yan Pujante
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

<%@ page import="org.linkedin.glu.console.domain.Fabric" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Fabric List</title>
</head>
<body>
<ul class="tabs">
  <li class="active"><a href="#">Fabric List</a></li>
  <li><g:link action="create">New</g:link></li>
</ul>
<div class="body">
  <div class="list">
    <table class="bordered-table condensed-table">
      <thead>
      <tr>

        <g:sortableColumn property="id" title="Id"/>

        <g:sortableColumn property="name" title="Name"/>

        <g:sortableColumn property="zkConnectString" title="Zk Connect String"/>

        <g:sortableColumn property="zkSessionTimeout" title="Zk Session Timeout"/>

        <g:sortableColumn property="color" title="Color"/>

      </tr>
      </thead>
      <tbody>
      <g:each in="${fabricInstanceList}" status="i" var="fabricInstance">
        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">

          <td><g:link action="show" id="${fabricInstance.id}">${fieldValue(bean: fabricInstance, field: 'id')}</g:link></td>

          <td>${fieldValue(bean: fabricInstance, field: 'name')}</td>

          <td>${fieldValue(bean: fabricInstance, field: 'zkConnectString')}</td>

          <td>${fieldValue(bean: fabricInstance, field: 'zkSessionTimeout')}</td>

          <td style="background-color: ${fieldValue(bean: fabricInstance, field: 'color')};"><span style="color: white;">${fieldValue(bean: fabricInstance, field: 'color')}</span></td>

        </tr>
      </g:each>
      </tbody>
    </table>
  </div>
  <div class="paginateButtons">
    <g:paginate total="${fabricInstanceTotal}"/>
  </div>
</div>
</body>
</html>
