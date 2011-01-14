%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
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
  <title>GLU Console - View System ${params.id}</title>
  <meta name="layout" content="main"/>
  <style type="text/css">
  textarea {
    font-family: monospace;
    margin-top: 1em;
  }
  table td {
    padding: 0.2em;
    text-align: center;
  }
  </style>
</head>
<body>
<ul class="submenu">
  <li><g:link controller="dashboard">Dashboard</g:link></li>
  <li><g:link action="list">System</g:link></li>
  <li><g:link action="delta">Current</g:link></li>
  <li class="selected">View [${params.id}]</li>
</ul>
<g:if test="${system}">
  <g:render template="system" model="[systems: [system]]"/>
  <g:form action="save" method="post">
    <textarea rows="40" cols="150" id="content" name="content">${params.content ?: system.systemModel}</textarea>
    <g:hiddenField name="id" value="${params.id}"/>
    <div class="buttons">
      <span class="button"><input class="save" type="submit" value="Save changes"/></span>
    </div>
  </g:form>
</g:if>
<g:else>
  No Such System ${params.id}
</g:else>
</body>
</html>
