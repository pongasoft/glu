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
  <ul class="nav nav-tabs">
    <li><cl:link action="deployments">Recent</cl:link></li>
    <li><cl:link action="archived">Archived</cl:link></li>
    <li class="active"><a href="#">Archived [${deployment.description.encodeAsHTML()}]</a></li>
  </ul>
  <table id="deployment" class="table table-bordered table-condensed">
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
  <ul class="nav nav-tabs">
    <li><cl:link action="deployments">Recent</cl:link></li>
    <li class="active"><a href="#">Archived</a></li>
  </ul>
<g:if test="${deployments}">
  <h3>Archived deployments [${count}]</h3>
  <div class="paginateButtons">
    <g:paginate total="${count}" max="25"/>
  </div>
  <table id="deployments" class="table table-bordered table-condensed">
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
        <td><cl:link action="archived" id="${deployment.id}">${deployment.description.encodeAsHTML()}</cl:link></td>
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
  <h3>No archived deployments...</h3>
</g:else>
</g:else>
</body>
</html>
