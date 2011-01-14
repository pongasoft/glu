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

<%@ page import="org.linkedin.glu.console.domain.AuditLog" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>AuditLog List</title>
  <style type="text/css">
  table {
    width: 100%;
  }
  td {
    vertical-align: top;
  }
  .fullInfo {
    display: none; 
  }
  </style>
</head>
<body>
<div class="body">
  <h1>AuditLog List</h1>
  <div class="paginateButtons">
    <g:paginate total="${auditLogInstanceTotal}" max="100"/>
  </div>
  <div class="list">
    <table>
      <thead>
      <tr>

        <th>Date</th>

        <th>Username</th>

        <th>Type</th>

        <th>Details</th>

        <th>Info</th>

      </tr>
      </thead>
      <tbody>
      <g:each in="${auditLogInstanceList}" status="i" var="auditLogInstance">
        <tr class="${(i % 2) == 0 ? 'odd' : 'even'}">

          <td><cl:formatDate date="${auditLogInstance.dateCreated}"/></td>

          <td>${auditLogInstance.username?.encodeAsHTML()}</td>

          <td>${auditLogInstance.type?.encodeAsHTML()}</td>

          <td>${auditLogInstance.details?.encodeAsHTML()}</td>

          <td>
            <cl:truncate text="${auditLogInstance.info}" size="20" var="truncated">
              <g:if test="${truncated}">
                ${truncated.encodeAsHTML()}...
                <div class="fullInfo">${auditLogInstance.info.encodeAsHTML()}</div>
              </g:if>
              <g:else>
                ${auditLogInstance.info?.encodeAsHTML()}
              </g:else>
            </cl:truncate>
          </td>
        </tr>
      </g:each>
      </tbody>
    </table>
  </div>
  <div class="paginateButtons">
    <g:paginate total="${auditLogInstanceTotal}" max="100"/>
  </div>
</div>
</body>
</html>
