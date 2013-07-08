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

<%@ page import="org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus" %>
<h3>Completed deployments [${deployments?.size()}] [<g:link action="archiveAllDeployments" controller="plan" onClick="return confirm('Are you sure you want to archive all deployments ?');">Archive All</g:link>]</h3>
<g:if test="${deployments}">
  <table class="table table-bordered table-condensed" id="completedDeployments">
    <tr>
      <th class="descriptionFilter">Description</th>
      <th class="usernameFilter">Username</th>
      <th class="startTimeFilter">Start Time</th>
      <th class="endTimeFilter">End Time</th>
      <th class="durationFilter">Duration</th>
      <th class="statusFilter">Status</th>
      <th class="actionsFilter">Actions</th>
    </tr>
    <g:each in="${deployments}" var="deployment">
      <g:set var="planExecution" value="${deployment.planExecution}"/>
      <tr class="${planExecution.completionStatus.status}">
        <td class="descriptionFilter"><g:link action="deployments" id="${deployment.id}" params="[showErrorsOnly: planExecution.completionStatus.status != IStepCompletionStatus.Status.COMPLETED]">${deployment.description}</g:link></td>
        <td class="usernameFilter">${deployment.username}</td>
        <td class="startTimeFilter"><cl:formatDate date="${new Date(planExecution.completionStatus.startTime)}"/></td>
        <td class="endTimeFilter">
          <cl:formatDate date="${new Date(planExecution.completionStatus.endTime)}"/>
        </td>
        <td class="durationFilter">
          <cl:formatDuration duration="${planExecution.completionStatus.duration}"/>
        </td>
        <td class="statusFilter">
          ${planExecution.completionStatus.status}
        </td>
        <td class="actionsFilter">
          <g:link action="archiveDeployment" id="${deployment.id}">Archive</g:link>
        </td>
      </tr>
    </g:each>
  </table>
</g:if>
<g:else>
  <h4>No Completed deployments.</h4>
</g:else>
