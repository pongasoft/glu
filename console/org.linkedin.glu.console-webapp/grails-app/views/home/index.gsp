%{--
  - Copyright 2010-2010 LinkedIn, Inc
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

<html>
<head>
  <title>GLU Console - ${user.username.encodeAsHTML()}</title>
  <meta name="layout" content="main"/>
</head>
<body>
<h2>You are currently logged in as user '<span id="body-username">${user.username.encodeAsHTML()}</span>'</h2>
<h2>User Roles</h2>
  <ul id="body-user-roles">
    <g:each in="${ConsoleUtils.sortByName(roles)}" var="role">
    <li id="body-user-role-${role.name}">${role.name}</li>
    </g:each>
  </ul>

<h2><g:link controller="user" action="credentials">Manage your credentials</g:link></h2>
<g:if test="${request.fabric}">
  Your current fabric is '<span id="body-fabric">${request.fabric.name.encodeAsHTML()}</span>'.
</g:if>
<g:else>
  You currently have not selected a fabric. <g:link controller="fabric" action="select">Select a fabric</g:link>
</g:else>
<h2><g:link controller="auth" action="signOut">Sign Out</g:link></h2>
</body>
</html>