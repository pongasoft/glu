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

<%@ page import="org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils; org.linkedin.util.lang.MemorySize" contentType="text/html;charset=UTF-8" %>
<g:set var="directory" value="${new File(URI.create("file:${params.location}"))}"/>
<html>
<head>
  <title>Directory listing ${params.location.encodeAsHTML()}</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'agents-list.css')}"/>
  <g:javascript>
    function toggleFullPath() {
       var toggleFullPathIcon = $('#toggle-fullpath-icon');
       var isShowingFullPath = toggleFullPathIcon.hasClass('icon-zoom-out');
       if(isShowingFullPath)
       {
         toggleFullPathIcon.removeClass('icon-zoom-out');
         toggleFullPathIcon.addClass('icon-zoom-in');
         hide('#fullpath');
         show('#filename-only');
       }
       else
       {
         toggleFullPathIcon.removeClass('icon-zoom-in');
         toggleFullPathIcon.addClass('icon-zoom-out');
         hide('#filename-only');
         show('#fullpath');
       }
    }
  </g:javascript>
</head>
<body>
<ul class="nav nav-tabs">
  <li><cl:link controller="agents" action="list">List</cl:link></li>
  <cl:whenFeatureEnabled feature="commands"><li><cl:link controller="commands" action="list">All Commands</cl:link></li></cl:whenFeatureEnabled>
  <li><cl:link action="view" id="${params.id}">agent [${params.id}]</cl:link></li>
  <li><cl:link action="plans" id="${params.id}">Plans</cl:link></li>
  <cl:whenFeatureEnabled feature="commands"><li><cl:link action="commands" id="${params.id}">Commands</cl:link></li></cl:whenFeatureEnabled>
  <li><cl:link action="ps" id="${params.id}">All Processes</cl:link></li>
  <li class="active"><a href="#" title="${directory.path.encodeAsHTML()}">Directory [${directory.name.encodeAsHTML()}]</a></li>
</ul>
<g:if test="${dir != null}">
  <table class="table table-bordered xtight-table">
    <thead>
    <tr>
      <th>Actions</th>
      <th>Directory</th>
      <th>Last Modified</th>
    </tr>
    </thead>
    <tbody>
    <tr>
      <td>
        <g:if test="${directory.parentFile}"><cl:link title="Go to parent directory [${directory.parentFile.name.encodeAsHTML()}]" action="fileContent" id="${params.id}" params="[location: directory.parentFile.toURI().rawPath]"><i class="icon-arrow-up"> </i></cl:link>
        |</g:if>
        <cl:link title="Refresh" action="fileContent" id="${params.id}" params="[location: directory.toURI().rawPath]"><i class="icon-refresh"> </i></cl:link>
      </td>
      <td><span id="filename-only">${directory.name.encodeAsHTML()}</span><span id="fullpath" class="hidden"><cl:linkFilePath file="${directory}" agent="${params.id}"/></span><div class="file-actions"><a href="#" id="toggle-fullpath-icon" title="Show/Hide full path" onclick="toggleFullPath();"><i class="icon-zoom-in"> </i></a></div></td>
      <td><cl:formatDate time="${dir['.']?.lastModified}"/></td>
    </tr>
    </tbody>
  </table>
  <table class="table table-bordered tight-table">
    <thead>
    <tr>
      <th>Name</th>
      <th>Last Modified</th>
      <th>Size</th>
    </tr>
    </thead>
    <g:if test="${directory.parentFile}">
      <tr>
        <td><cl:link action="fileContent" id="${params.id}" params="[location: new File(params.location).parentFile.toURI().rawPath]">../</cl:link><div class="file-actions"><cl:link title="Go to parent directory [${directory.parentFile.name.encodeAsHTML()}]" action="fileContent" id="${params.id}" params="[location: directory.parentFile.toURI().rawPath]"><i class="icon-arrow-up"> </i></cl:link></div></td>
        <td></td>
        <td>-</td>
      </tr>
    </g:if>
    <g:each in="${GluGroovyCollectionUtils.xorMap(dir, ['.']).keySet().sort()}" var="${filename}">
      <g:set var="entry" value="${dir[filename]}"/>
      <g:set var="file" value="${new File(directory, filename)}"/>
      <tr>
        <td><cl:link title="${file.path.encodeAsHTML()}" action="fileContent" id="${params.id}" params="[(entry.isDirectory ? 'location' : 'file'): file.toURI().rawPath]">${filename.encodeAsHTML()}<g:if test="${entry.isDirectory}">/</g:if><g:if test="${entry.containsKey('isSymbolicLink') ? entry.isSymbolicLink : (file.path != entry.canonicalPath)}">@ -&gt; ${entry.canonicalPath.encodeAsHTML()}</g:if></cl:link><div class="file-actions"><g:if test="${!entry.isDirectory}"><cl:link title="tail -f ${file.name.encodeAsHTML()}" action="fileContent" id="${params.id}" params="[file: file.toURI().rawPath]"><i class="icon-repeat"> </i></cl:link> | <cl:link title="View Raw" action="fileContent" id="${params.id}" params="[location: file.toURI().rawPath, maxSize: -1]"><i class="icon-eye-open"> </i></cl:link> | <cl:link title="Download" action="fileContent" id="${params.id}" params="[location: file.toURI().rawPath, maxSize: -1, binaryOutput: true]"><i class="icon-download-alt"> </i></cl:link></g:if><g:else><cl:link title="Go to [${file.name.encodeAsHTML()}]" action="fileContent" id="${params.id}" params="[location: file.toURI().rawPath]"><i class="icon-folder-open"> </i></cl:link></g:else></div></td>
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