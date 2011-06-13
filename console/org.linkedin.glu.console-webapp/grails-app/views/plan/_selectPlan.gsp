%{--
  - Copyright (c) 2011 Yan Pujante
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
<%@ page import="org.linkedin.groovy.util.json.JsonUtils; org.linkedin.glu.provisioner.plan.api.IStep.Type" %>
<div id="select-plan">
<h1 class="${hasDelta ? 'hasDelta' : ''}">${title} ${hasDelta ? '(delta)' : ''}</h1>
<g:form controller="plan" action="redirectView">
  <select name="planDetails" onchange="${remoteFunction(controller: 'plan', action:'create', update:[success:'plan-preview'], params: "'json=' + this.value")}">
    <option value="{}" selected="true">Choose Plan</option>
    <g:if test="${hasDelta}">
      <g:each in="${['SEQUENTIAL', 'PARALLEL']}" var="stepType">
          <option value="${JsonUtils.toJSON([planType: 'Deploy', stepType: stepType, name: 'Deploy - ' + title, systemFilter: filter]).encodeAsHTML()}">Deploy - ${title} - ${stepType} [*]</option>
      </g:each>
    </g:if>
    <g:each in="['Bounce', 'Redeploy', 'Undeploy']" var="planType">
      <g:each in="${['SEQUENTIAL', 'PARALLEL']}" var="stepType">
          <option value="${JsonUtils.toJSON([planType: planType, stepType: stepType, name: planType + ' - ' + title, systemFilter: filter]).encodeAsHTML()}">${planType} - ${title} - ${stepType}</option>
      </g:each>
    </g:each>
  </select>
  <input type="submit" name="view" value="Select this plan" onClick="document.getElementById('planIdSelector').value=document.getElementById('planId').value;return true;">
  <input type="hidden" name="planId" id="planIdSelector" value="undefined" />
  <span class="spinner" style="display:none;">
    <img src="${resource(dir:'images',file:'spinner.gif')}" alt="Spinner" />
  </span>
</g:form>
<div id="plan-preview" class="planDetails"></div>
</div>