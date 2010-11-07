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
  <title>GLU Console - Dashboard</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'audit.css')}"/>
  <script type="text/javascript" src="${resource(dir:'js',file:'audit.js')}"></script>
  <g:javascript>
<cl:renderAuditJS/>
  </g:javascript>
  <script type="text/javascript" src="${resource(dir:'js',file:'console_yui.js')}"></script>
</head>
<body>
<ul class="submenu">
  <li class="selected">Dashboard</li>
  <li><g:link controller="system" action="list">System</g:link></li>
</ul>
<g:render template="audit"/>
</body>
</html>