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
  <title>Deployments (Archived)</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'status-colors.css')}"/>
  <style type="text/css">
  table#deployments td {
    padding: 0.2em;
  }
  table#deployment td {
    padding: 0.5em;
  }
  .step {
    padding: .2em;
    margin: .4em;
  }
  </style>
</head>
<body>
<g:if test="${deployment}">
  <ul class="tabs">
    <li><g:link action="deployments">Recent</g:link></li>
    <li><g:link action="archived">Archived</g:link></li>
    <li class="active"><a href="#">Archived [${deployment.description.encodeAsHTML()}]</a></li>
  </ul>
  <table id="deployment" class="bordered-table condensed-table">
    <tr>
      <th>Username</th>
      <th>Start Date</th>
      <th>End Date</th>
      <th>Duration</th>
      <th>Status</th>
    </tr>
    <tr class="${deployment.status ?: 'RUNNING'}">
      <td>${deployment.username}</td>
      <td>
        <cl:formatDate date="${deployment.startDate}"/>
      </td>
      <td>
        <g:if test="${deployment.endDate}">
          <cl:formatDate date="${deployment.endDate}"/>
        </g:if>
      </td>
      <td>
        <g:if test="${deployment.duration}">
          ${deployment.duration}
        </g:if>
      </td>
      <td>
        <g:if test="${deployment.status}">
          ${deployment.status}
        </g:if>
      </td>
    </tr>
  </table>
  <pre>
${deployment.details.encodeAsHTML()}
  </pre>
</g:if>
<g:else>
  <ul class="tabs">
    <li><g:link action="deployments">Recent</g:link></li>
    <li class="active"><a href="#">Archived</a></li>
  </ul>
<g:if test="${deployments}">
  <h2>Archived deployments [${count}]</h2>
  <div class="paginateButtons">
    <g:paginate total="${count}" max="25"/>
  </div>
  <table id="deployments" class="bordered-table condensed-table">
    <tr>
      <th>Description</th>
      <th>Username</th>
      <th>Start Date</th>
      <th>End Date</th>
      <th>Duration</th>
      <th>Status</th>
    </tr>
    <g:each in="${deployments}" var="deployment">
      <tr class="${deployment.status ?: 'RUNNING'}">
        <td><g:link action="archived" id="${deployment.id}">${deployment.description.encodeAsHTML()}</g:link></td>
        <td>${deployment.username}</td>
        <td>
          <cl:formatDate date="${deployment.startDate}"/>
        </td>
        <td>
          <g:if test="${deployment.endDate}">
            <cl:formatDate date="${deployment.endDate}"/>
          </g:if>
        </td>
        <td>
          <g:if test="${deployment.duration}">
            ${deployment.duration}
          </g:if>
        </td>
        <td>
          <g:if test="${deployment.status}">
            ${deployment.status}
          </g:if>
        </td>
      </tr>
    </g:each>
  </table>
  <div class="paginateButtons">
    <g:paginate total="${count}" max="25"/>
  </div>
</g:if>
<g:else>
  <h2>No archived deployments...</h2>
</g:else>
</g:else>
</body>
</html>
