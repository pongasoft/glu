%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
  - Portions Copyright (c) 2011-2013 Yan Pujante
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

<%@ page import="org.linkedin.glu.console.domain.User" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>User List</title>
</head>
<body>
<div class="body">
  <ul class="nav nav-tabs">
    <li class="active"><a href="#">User List</a></li>
    <li><g:link action="create">New</g:link></li>
  </ul>
  <div class="paginateButtons">
    <g:paginate total="${userInstanceTotal}" max="100"/>
  </div>
  <div class="list">
    <table class="table table-bordered table-condensed noFullWidth">
      <thead>
      <tr>
        <g:sortableColumn property="username" title="Username"/>
        <th>Roles</th>
      </tr>
      </thead>
      <tbody>
      <g:each in="${userInstanceList}" status="i" var="userInstance">
        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">
          <td><g:link action="show" id="${userInstance.id}">${fieldValue(bean: userInstance, field: 'username')}</g:link></td>
          <td>${ConsoleUtils.sortByName(userInstance.roles)}</td>
        </tr>
      </g:each>
      </tbody>
    </table>
  </div>
  <div class="paginateButtons">
    <g:paginate total="${userInstanceTotal}"/>
  </div>
</div>
</body>
</html>
