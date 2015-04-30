%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
  - Portions Copyright (c) 2011-2015 Yan Pujante
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
  <title>System List</title>
  <meta name="layout" content="main"/>
  <style type="text/css">
  table.alternate-row-colors tbody tr.current {
    background: #aaffaa;
  }
  </style>
</head>
<body>
<ul class="nav nav-tabs">
  <li class="active"><a href="#">List</a></li>
  <li><cl:link action="choose">Load</cl:link></li>
</ul>
<div class="paginateButtons">
  <g:paginate total="${total}"/>
</div>
<g:render template="model" model="[systems: systems, columns: [:], currentSystem: currentSystem]"/>
<div class="paginateButtons">
  <g:paginate total="${total}"/>
</div>
</body>
</html>
