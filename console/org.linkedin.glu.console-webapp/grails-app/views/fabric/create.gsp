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

<%@ page import="org.linkedin.glu.console.domain.Fabric" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Create Fabric</title>
</head>
<body>
<ul class="nav nav-tabs">
  <li><cl:link action="list">Fabric List</cl:link></li>
  <li class="active"><a href="#">New</a></li>
</ul>
<div class="body">
  <g:hasErrors bean="${fabricInstance}">
    <div class="errors">
      <g:renderErrors bean="${fabricInstance}" as="list"/>
    </div>
  </g:hasErrors>
  <g:form action="save" method="post">
    <div class="dialog">
      <table class="table table-bordered table-condensed noFullWidth">
        <tbody>

        <tr class="prop">
          <td valign="top" class="name">
            <label for="name">Name:</label>
          </td>
          <td valign="top" class="value ${hasErrors(bean: fabricInstance, field: 'name', 'errors')}">
            <input type="text" id="name" name="name" value="${fieldValue(bean: fabricInstance, field: 'name')}"/>
          </td>
        </tr>

        <tr class="prop">
          <td valign="top" class="name">
            <label for="zkConnectString">Zk Connect String:</label>
          </td>
          <td valign="top" class="value ${hasErrors(bean: fabricInstance, field: 'zkConnectString', 'errors')}">
            <input type="text" id="zkConnectString" name="zkConnectString" value="${fieldValue(bean: fabricInstance, field: 'zkConnectString')}"/>
          </td>
        </tr>

        <tr class="prop">
          <td valign="top" class="name">
            <label for="zkSessionTimeout">Zk Session Timeout:</label>
          </td>
          <td valign="top" class="value ${hasErrors(bean: fabricInstance, field: 'zkSessionTimeout', 'errors')}">
            <input type="text" id="zkSessionTimeout" name="zkSessionTimeout" value="${fieldValue(bean: fabricInstance, field: 'zkSessionTimeout')}"/>
          </td>
        </tr>

        <tr class="prop">
          <td valign="top" class="name">
            <label for="zkSessionTimeout">Color:</label>
          </td>
          <td valign="top" class="value ${hasErrors(bean: fabricInstance, field: 'color', 'errors')}">
            <input type="text" id="color" name="color" value="${fieldValue(bean: fabricInstance, field: 'color')}"/>
          </td>
        </tr>

        </tbody>
      </table>
    </div>
    <div class="buttons">
      <span class="button"><input class="btn btn-primary save" type="submit" value="Create"/></span>
    </div>
  </g:form>
</div>
</body>
</html>
