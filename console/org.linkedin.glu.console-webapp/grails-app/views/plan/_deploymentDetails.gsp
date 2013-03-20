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

<g:if test="${!deployment.planExecution.completed}">
  <g:if test="${deployment.planExecution.cancelled}">
    Aborted
  </g:if>
  <g:else>
    <h3>
      <g:link action="deployments" id="${deployment.id}">Refresh</g:link>
    <g:if test="${deployment.planExecution.paused}">
      | <g:link action="resumeDeployment" id="${deployment.id}">Resume</g:link>
    </g:if>
    <g:else>
      | <g:link action="pauseDeployment" id="${deployment.id}">Pause</g:link>
    </g:else>
    | <g:link action="abortDeployment" id="${deployment.id}">Abort</g:link>
    </h3>
  </g:else>
</g:if>
<g:set var="progress" value="${deployment.progressTracker.steps}"/>

<h4>
  <div class="progress">
    <div class="bar" style="width: ${deployment.progressTracker.completionPercentage}%;"></div>
  </div>
  ${deployment.progressTracker.leafStepsCompletedCount}/${deployment.planExecution.plan.leafStepsCount} - <span id="progress">${deployment.progressTracker.completionPercentage}</span>%
</h4>

<div id="deployment-details">
  <dl class="plan">
    <dt>${deployment.planExecution.plan.name.encodeAsHTML()}</dt>
    <dd>
      <cl:renderStepExecution deployment="${deployment}" progress="${progress}"/>
    </dd>
  </dl>
</div>
