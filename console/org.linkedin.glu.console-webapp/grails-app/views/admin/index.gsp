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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>Admin</title>
  <meta name="layout" content="main"/>
</head>
<body>
<h1>Admin</h1>
<h2>Agents</h2>
<ul>
  <li><g:link controller="agents" action="listVersions">Upgrade agents</g:link></li>
</ul>
<h2>Fabric</h2>
<ul>
  <li><g:link controller="fabric" action="listAgentFabrics">View agents fabric</g:link></li>
  <li><g:link controller="fabric" action="refresh">Refresh Fabrics</g:link></li>
  <li><g:link controller="fabric" action="list">Create/Add Fabric</g:link></li>
</ul>

<h2>Users</h2>
<ul>
  <li><g:link controller="user" action="list">Manager User Roles</g:link></li>
</ul>

<h2>Audit Logs</h2>
<ul>
  <li><g:link controller="auditLog" action="list">View Audit Logs</g:link></li>
</ul>

<h2>Encryption Keys</h2>
<ul>
  <li><g:link controller="encryption" action="list">View Encryption Keys</g:link></li>
  <li><g:link controller="encryption" action="create">Create Encryption Key</g:link></li>
  <li><g:link controller="encryption" action="encrypt">Encrypt/Decrypt Text</g:link></li>
</ul>
</body>
</html>