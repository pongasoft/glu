
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
        <meta name="layout" content="main" />
        <title>Create User</title>
    </head>
    <body>
    <ul class="nav nav-tabs">
      <li><cl:link action="list">User List</cl:link></li>
      <li class="active"><a href="#">New</a></li>
    </ul>
        <div class="body">
            <g:hasErrors bean="${userInstance}">
            <div class="errors">
                <g:renderErrors bean="${userInstance}" as="list" />
            </div>
            </g:hasErrors>
            <cl:form method="post" action="save">
              <div class="dialog">
                <table class="table table-bordered table-condensed noFullWidth">
                  <tbody>
                        
                  <tr class="prop">
                    <td valign="top" class="name">
                      <label for="username">Username:</label>
                    </td>
                    <td valign="top" class="value ${hasErrors(bean:userInstance,field:'username','errors')}">
                      <g:textField name="username" value="${fieldValue(bean:userInstance,field:'username')}"/>
                    </td>
                  </tr>
                        
                  <tr class="prop">
                    <td valign="top" class="name">
                      <label for="password">Password:</label>
                    </td>
                    <td valign="top" class="value">
                      <g:passwordField name="password"/>
                    </td>
                  </tr>

                  <tr class="prop">
                    <td valign="top" class="name">
                      <label for="passwordAgain">Password (again):</label>
                    </td>
                    <td valign="top" class="value">
                      <g:passwordField name="passwordAgain"/>
                    </td>
                  </tr>

                  </tbody>
                </table>
              </div>
              <div class="buttons">
                <span class="button"><g:actionSubmit class="btn btn-primary" value="Create" action="save"/></span>
              </div>
            </cl:form>
        </div>
    </body>
</html>
