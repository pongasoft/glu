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
  <title>Show User</title>
</head>

<body>
<ul class="nav nav-tabs">
  <li><cl:link action="list">User List</cl:link></li>
  <li class="active"><a href="#">User [${userInstance.username}]</a></li>
  <li><cl:link controller="user" action="edit" id="${userInstance.id}">Edit [${userInstance.username}]</cl:link></li>
</ul>

<div class="body">
  <div class="dialog">
    <table class="table table-bordered table-condensed noFullWidth">
      <tbody>

      <tr class="prop">
        <td valign="top" class="name">Id:</td>

        <td valign="top" class="value">${fieldValue(bean: userInstance, field: 'id')}</td>

      </tr>

      <tr class="prop">
        <td valign="top" class="name">Username:</td>

        <td valign="top" class="value">${fieldValue(bean: userInstance, field: 'username')}</td>

      </tr>

      <tr class="prop">
        <td valign="top" class="name">Roles:</td>

        <td valign="top" style="text-align:left;" class="value">
          <ul>
            <g:each var="r" in="${userInstance.roles}">
              <li>${r?.encodeAsHTML()}</li>
            </g:each>
          </ul>
        </td>

      </tr>

      </tbody>
    </table>
  </div>

  <div class="buttons">
    <cl:form>
      <input type="hidden" name="id" value="${userInstance?.id}"/>
      <span class="button"><g:actionSubmit class="btn btn-primary" value="Edit"/></span>
      <span class="button"><g:actionSubmit class="btn" onclick="return confirm('Are you sure?');"
                                           value="Delete"/></span>
    </cl:form>
  </div>
</div>
</body>
</html>
