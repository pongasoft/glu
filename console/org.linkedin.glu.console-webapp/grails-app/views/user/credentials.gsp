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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>Credentials Manager</title>
  <meta name="layout" content="main"/>
  <style type="text/css">
  .passwordStatus {
    color: green;
    padding-bottom: 0.5em;
    font-size: 1.3em;
  }
  </style>
</head>
<body>

<h1>Manage your credentials</h1>

<h2>Change your password</h2>
<div class="passwordStatus">
<g:if test="${!(credentials?.oneWayHashPassword)}">
You currently do not have a GLU specific password and your LDAP password credentials will be used.
</g:if>
<g:else>
  You have setup a GLU specific password that you can change below.
</g:else>
</div>
<cl:form action="updatePassword" method="post">
  <table class="noFullWidth">
    <tr>
      <th>Current Password:</th>
      <td><g:passwordField name="currentPassword"/></td>
    </tr>
    <tr>
      <th>New Password:</th>
      <td><g:passwordField name="newPassword"/></td>
    </tr>
    <tr>
      <th>New Password (again):</th>
      <td><g:passwordField name="newPasswordAgain"/></td>
    </tr>
  </table>
  <div class="buttons">
    <span class="button"><input class="btn btn-primary" type="submit" value="Save password"/></span>
  </div>
</cl:form>

<% /*
<h2>Change your pem (X.509 Base64 encoded DER certificate)</h2>
<cl:form action="updatePem" method="post">
  <textarea rows="10" cols="100" id="pem" name="pem">${params.pem ?: credentials?.x509Pem}</textarea>
  <div class="buttons">
    <span class="button"><input class="updatePem" type="submit" value="Save PEM"/></span>
  </div>
</cl:form>
*/ %>

</body>
</html>
