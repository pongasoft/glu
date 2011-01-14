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
  <title>GLU Console - Agents</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'agents-list.css')}"/>
</head>
<body>
<h3>Hosts: ${count}, Instances: ${instances}</h3>
<table>
  <tr>
    <th class="delta-header">&Delta;</th>
    <th>Hostname</th>
    <th>MountPoints</th>
    <th>Started on</th>
  </tr>
  <g:if test="${model}">
    <g:each in="${model.values()}" var="entry">
      <tr class="${entry.state}">
        <td class="${entry.hasDelta ? 'entry-delta' : ''}"></td>
        <td><g:link class="agentName" controller="agents" action="view" id="${entry.agent.agentName}">${entry.agent.agentName}</g:link></td>
        <td>
          <ul>
            <g:each in="${entry.mountPoints}" var="mountPoint">
              <li class="${cl.mountPointState(mountPoint: mountPoint.value)}">${mountPoint.key} [${(mountPoint.value.transitionState ?: mountPoint.value.currentState).encodeAsHTML()}] - <cl:formatDate date="${new Date(mountPoint.value.modifiedTime)}"/>
              </li>
            </g:each>
          </ul>
        </td>
        <td><cl:formatDate date="${new Date(entry.agent.creationTime)}"/></td>
      </tr>
    </g:each>
  </g:if>
</table>
</body>
</html>