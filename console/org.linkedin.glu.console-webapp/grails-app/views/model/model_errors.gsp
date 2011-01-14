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

<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Load Model</title>
</head>
<body>
<div class="body">
  <h1>The model was loaded successfully with the following warnings</h1>

  <h2><g:link controller="dashboard">Continue anyway</g:link></h2>
  <h2><g:link controller="model" action="choose">Fix the model and reload it</g:link></h2>

  <g:if test="${errors.containers}">
    <table>
      <tr>
        <th>Container</th>
        <th>Missing references (in bom)</th>
      </tr>
      <g:each in="${errors.containers}" var="c">
        <tr>
          <td>${c.key}</td>
          <td>${c.value.name.join(', ')}</td>
        </tr>
      </g:each>
    </table>
  </g:if>

  <g:if test="${errors.entries}">
    <table>
      <tr>
        <th>Entry</th>
        <th>Missing Container (in containers.xml)</th>
      </tr>
      <g:each in="${errors.entries}" var="e">
        <tr>
          <td>${e.key}</td>
          <td>${e.value.name}</td>
        </tr>
      </g:each>
    </table>
  </g:if>

  <g:if test="${errors.partial}">
    <table>
      <tr>
        <th>Partially resolved containers</th>
        <th>Missing references from new bom</th>
      </tr>
      <g:each in="${errors.partial}" var="m">
        <tr>
          <td>${m.key}</td>
          <td>${m.value.join(', ')}</td>
        </tr>
      </g:each>
    </table>
  </g:if>
</div>
</body>
</html>
