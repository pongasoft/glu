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

<%@ page import="org.linkedin.util.lang.MemorySize" contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>Directory listing ${params.location.encodeAsHTML()}</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'agents-list.css')}"/>
</head>
<body>
<ul class="tabs">
  <li><g:link controller="agents" action="list">List</g:link></li>
  <cl:whenFeatureEnabled feature="commands"><li><g:link controller="commands" action="list">All Commands</g:link></li></cl:whenFeatureEnabled>
  <li><g:link action="view" id="${params.id}">agent [${params.id}]</g:link></li>
  <li><g:link action="plans" id="${params.id}">Plans</g:link></li>
  <cl:whenFeatureEnabled feature="commands"><li><g:link action="commands" id="${params.id}">Commands</g:link></li></cl:whenFeatureEnabled>
  <li><g:link action="ps" id="${params.id}">All Processes</g:link></li>
  <li class="active"><a href="#">Directory [${params.location.encodeAsHTML()}]</a></li>
</ul>
<g:if test="${dir != null}">
  <table class="bordered-table tight-table">
    <g:if test="${new File(params.location).parent}">
      <tr>
        <td><g:link action="fileContent" id="${params.id}" params="[location: new File(params.location).parent]">../</g:link></td>
        <td></td>
        <td>-</td>
      </tr>
    </g:if>
    <g:each in="${dir.keySet().sort()}" var="${filename}">
      <g:set var="entry" value="${dir[filename]}"/>
      <tr>
        <td><g:link action="fileContent" id="${params.id}" params="[location: entry.canonicalPath, maxLine: 500]">${filename.encodeAsHTML()}<g:if test="${entry.isDirectory}">/</g:if><g:if test="${new File(params.location, filename).path != entry.canonicalPath}">@ -&gt; ${entry.canonicalPath.encodeAsHTML()}</g:if></g:link></td>
        <td><cl:formatDate date="${new Date(entry.lastModified)}"/></td>
        <td><g:if test="${entry.isDirectory}">-</g:if><g:else>${entry.length} (${new MemorySize(entry.length as long).canonicalString})</g:else></td>
      </tr>
    </g:each>
  </table>
</g:if>
<g:else>
  ${params.location.encodeAsHTML()}: No Such file or directory
</g:else>

</body>
</html>