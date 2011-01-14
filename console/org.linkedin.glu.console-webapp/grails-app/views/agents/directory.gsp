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

<%@ page import="org.linkedin.util.lang.MemorySize" contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>GLU Console - Directory listing ${params.location.encodeAsHTML()}</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'agents-list.css')}"/>
</head>
<body>
<g:if test="${dir != null}">
  <h1>Directory: <g:link action="view" id="${params.id}">${params.id}</g:link>:${params.location.encodeAsHTML()}</h1>

  <table>
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
        <td><g:link action="fileContent" id="${params.id}" params="[location: entry.canonicalPath, maxLine: 500]">${filename.encodeAsHTML()}<g:if test="${entry.isDirectory}">/</g:if></g:link></td>
        <td><cl:formatDate date="${new Date(entry.lastModified)}"/></td>
        <td><g:if test="${entry.isDirectory}">-</g:if><g:else>${entry.length} (${new MemorySize(entry.length as long).canonicalString})</g:else></td>
      </tr>
    </g:each>
  </table>
</g:if>
<g:else>
  <h1>${params.location.encodeAsHTML()}: No Such file or directory</h1>
</g:else>

</body>
</html>