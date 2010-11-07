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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>GLU Console - System List</title>
  <meta name="layout" content="main"/>
  <style type="text/css">
  table td {
    padding: 0.2em;
    text-align: center;
  }

  .current {
    background: #eeeeff;
  }
  </style>
</head>
<body>
<ul class="submenu">
  <li><g:link controller="dashboard">Dashboard</g:link></li>
  <li class="selected">System</li>
  <li><g:link action="delta">Current</g:link></li>
</ul>
<div class="paginateButtons">
  <g:paginate total="${total}"/>
</div>
<g:render template="system" model="[systems: systems, columns: columns, currentSystem: currentSystem]"/>
<div class="paginateButtons">
  <g:paginate total="${total}"/>
</div>
</body>
</html>
