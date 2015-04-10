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

<%@ page import="org.linkedin.glu.console.domain.RoleName; org.linkedin.glu.console.domain.User" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Edit User</title>
</head>

<body>
<ul class="nav nav-tabs">
  <li><cl:link action="list">User List</cl:link></li>
  <li><cl:link action="show" id="${userInstance.id}">User [${userInstance.username.encodeAsHTML()}]</cl:link></li>
  <li class="active"><a href="#">Edit [${userInstance.username.encodeAsHTML()}]</a></li>
</ul>

<div class="body">
  <h2>Change [${userInstance.username.encodeAsHTML()}] roles</h2>
  <g:hasErrors bean="${userInstance}">
    <div class="errors">
      <g:renderErrors bean="${userInstance}" as="list"/>
    </div>
  </g:hasErrors>
  <cl:form method="post">
    <input type="hidden" name="id" value="${userInstance?.id}"/>
    <input type="hidden" name="version" value="${userInstance?.version}"/>

    <div class="dialog">
      <table class="table table-bordered table-condensed noFullWidth">
        <tbody>

        <tr class="prop">
          <td valign="top" class="name">
            <label for="username">Username:</label>
          </td>
          <td valign="top"
              class="value ${hasErrors(bean: userInstance, field: 'username', 'errors')}">
            ${fieldValue(bean: userInstance, field: 'username')}
            <input type="hidden" id="username" name="username"
                   value="${fieldValue(bean: userInstance, field: 'username')}"/>
          </td>
        </tr>

        <tr class="prop">
          <td valign="top" class="name">
            <label for="roles">Roles:</label>
          </td>
          <td valign="top" class="value ${hasErrors(bean: userInstance, field: 'roles', 'errors')}">
            <g:each in="${RoleName.values()}" var="role">
              <g:checkBox name="role" value="${role}"
                          checked="${userInstance.hasRole(role)}"/>${role}<br/>
            </g:each>
          </td>
        </tr>

        </tbody>
      </table>
    </div>

    <div class="buttons">
      <span class="button"><g:actionSubmit class="btn btn-primary" value="Update"/></span>
      <span class="button"><g:actionSubmit class="btn btn-danger" onclick="return confirm('Are you sure you want to delete this user?');"
                                           value="Delete"/></span>
    </div>
  </cl:form>
  <hr >
  <h2>Reset [${userInstance.username.encodeAsHTML()}] password</h2>
  <cl:form action="resetPassword" method="post">
    <table class="noFullWidth">
      <tr>
        <th>YOUR Current Password:</th>
        <td><g:passwordField name="currentPassword"/></td>
      </tr>
      <tr>
        <th>[${userInstance.username.encodeAsHTML()}] New Password:</th>
        <td><g:passwordField name="newPassword"/></td>
      </tr>
      <tr>
        <th>[${userInstance.username.encodeAsHTML()}] New Password (again):</th>
        <td><g:passwordField name="newPasswordAgain"/></td>
      </tr>
    </table>
    <div class="buttons">
      <span class="button"><input class="btn btn-primary" type="submit" value="Reset password" onclick="return confirm('Are you sure you want to reset the password?')"/></span>
    </div>
    <input type="hidden" name="id" value="${userInstance?.id}"/>
  </cl:form>
</div>
</body>
</html>
