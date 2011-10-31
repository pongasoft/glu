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
  <title>Dashboard</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'delta.css')}"/>
  <script type="text/javascript" src="${resource(dir:'js',file:'delta.js')}"></script>
  <g:javascript>
<cl:renderDeltaJS/>
  </g:javascript>
  <script type="text/javascript" src="${resource(dir:'js',file:'console_yui.js')}"></script>
</head>
<body>
<ul class="tabs">
  <li class="active"><a href="#">View</a></li>
  <li><g:link controller="dashboard" action="deploy">Deploy<g:if test="${delta.counts['errors']}"> / <span class="label important">delta</span></g:if></g:link></li>
  <li><g:link controller="dashboard" action="customize">Customize</g:link></li>
</ul>
<g:render template="delta"/>
</body>
</html>