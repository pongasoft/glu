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

<%@ page import="org.linkedin.glu.grails.utils.ConsoleConfig" contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>View System ${params.id}</title>
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
  .current {
    background: #aaffaa;
  }
  </style>
</head>
<body>
<g:if test="${systemDetails != null}">
  <ul class="nav nav-tabs">
    <li><cl:link action="list">List</cl:link></li>
    <li><cl:link action="choose">Load</cl:link></li>
    <li class="active"><a href="#">View [<cl:renderSystemId id="${systemDetails.systemId}" name="${systemDetails.name}" renderLinkToSystem="${false}"/>]</a></li>
  </ul>
  <g:set var="editable" value="${!ConsoleConfig.getInstance().defaults.disableModelUpdate}"/>
  <g:render template="model" model="[systems: [systemDetails]]"/>
  <cl:form action="save" method="post">
    <g:if test="${editable}">
      <div class="actions">
        <input class="save btn btn-primary" type="submit" value="Save changes"/>
      </div>
    </g:if>
    <textarea rows="40" style="width: 100%" id="content" name="content" ${editable ? '' : 'readonly="true"'}>${params.content ?: renderer.prettyPrint(systemDetails.systemModel)}</textarea>
    <g:hiddenField name="id" value="${params.id}"/>
    <g:if test="${editable}">
      <div class="actions">
        <input class="save btn btn-primary" type="submit" value="Save changes"/>
      </div>
    </g:if>
  </cl:form>
</g:if>
<g:else>
  <ul class="nav nav-tabs">
    <li><cl:link action="list">List</cl:link></li>
    <g:if test="${isReleaseUser}"><li><cl:link action="choose">Load</cl:link></li></g:if>
    <li class="active"><a href="#">View [${params.id}]</a></li>
  </ul>
  No Such System ${params.id}
</g:else>
</body>
</html>
