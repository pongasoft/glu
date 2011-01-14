%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
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
  <title>Show Fabric</title>
</head>
<body>
<div class="nav">
  <span class="menuButton"><g:link class="list" action="list">Fabric List</g:link></span>
  <span class="menuButton"><g:link class="create" action="create">New Fabric</g:link></span>
</div>
<div class="body">
  <h1>Show Fabric</h1>
  <div class="dialog">
    <table>
      <tbody>

      <tr class="prop">
        <td valign="top" class="name">Id:</td>

        <td valign="top" class="value">${fieldValue(bean: fabricInstance, field: 'id')}</td>

      </tr>

      <tr class="prop">
        <td valign="top" class="name">Name:</td>

        <td valign="top" class="value">${fieldValue(bean: fabricInstance, field: 'name')}</td>

      </tr>

      <tr class="prop">
        <td valign="top" class="name">Zk Connect String:</td>

        <td valign="top" class="value">${fieldValue(bean: fabricInstance, field: 'zkConnectString')}</td>

      </tr>

      <tr class="prop">
        <td valign="top" class="name">Zk Session Timeout:</td>

        <td valign="top" class="value">${fieldValue(bean: fabricInstance, field: 'zkSessionTimeout')}</td>

      </tr>

      <tr class="prop">
        <td valign="top" class="name">Color:</td>

        <td valign="top" class="value">${fieldValue(bean: fabricInstance, field: 'color')}</td>

      </tr>

      </tbody>
    </table>
  </div>
  <g:link controller="fabric" action="edit" id="${fabricInstance.id}">Edit</g:link>
</div>
</body>
</html>
