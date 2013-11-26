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

<%@ page import="org.linkedin.glu.groovy.utils.jvm.JVMInfo" contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>Admin</title>
  <meta name="layout" content="main"/>
</head>
<body>
<h2>Admin</h2>
<h3>Agents</h3>
<ul>
  <li><cl:link controller="agents" action="listVersions">Upgrade agents</cl:link></li>
</ul>
<h3>Fabric</h3>
<ul>
  <li><cl:link controller="fabric" action="listAgentFabrics">View agents fabric</cl:link></li>
  <li><cl:link controller="fabric" action="refresh">Refresh Fabrics</cl:link></li>
  <li><cl:link controller="fabric" action="list">Create/Add Fabric</cl:link></li>
</ul>

<h3>Users</h3>
<ul>
  <li><cl:link controller="user" action="list">Manage Users</cl:link></li>
</ul>

<h3>Audit Logs</h3>
<ul>
  <li><cl:link controller="auditLog" action="list">View Audit Logs</cl:link></li>
</ul>

<h3>Encryption Keys</h3>
<ul>
  <li><cl:link controller="encryption" action="list">View Encryption Keys</cl:link></li>
  <li><cl:link controller="encryption" action="create">Create Encryption Key</cl:link></li>
  <li><cl:link controller="encryption" action="encrypt">Encrypt/Decrypt Text</cl:link></li>
</ul>

<h3>JVM Info (Console)</h3>
<pre>
${JVMInfo.getJVMInfoString().encodeAsHTML()}
</pre>
<cl:mapToTable class="table table-bordered xtight-table noFullWidth" map="${JVMInfo.getJVMInfo() }"/>
</body>
</html>