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

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>Encryption Keys</title>
  <meta name="layout" content="main"/>
</head>
<body>
<ul class="nav nav-tabs">
  <li class="active"><a href="#">List</a></li>
  <li><g:link action="create">New</g:link></li>
  <li><g:link action="encrypt">Encrypt/Decrypt</g:link></li>
</ul>
<h3>Number of keys in KeyStore: ${count}</h3>
<table class="table table-bordered condensed-table noFullWidth">
  <tr>
    <th>KeyName</th>
    <th>Base64 Encoded Value</th>
  </tr>
  <g:if test="${encryptionKeys}">
    <g:each in="${encryptionKeys}" var="entry">
      <tr>
        <td>${entry.key}</td>
        <td>${entry.value}</td>
      </tr>
    </g:each>
  </g:if>
</table>
</body>
</html>