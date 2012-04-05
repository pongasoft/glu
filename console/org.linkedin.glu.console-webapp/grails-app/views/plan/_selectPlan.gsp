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
  <g:form controller="plan" action="redirectView">
    <table id="select-plan-radio">
      <g:each in="${['SEQUENTIAL', 'PARALLEL']}" var="stepType">
        <tr>
          <th colspan="3">${title}</th>
        </tr>
        <g:each in="['Deploy', 'Bounce', 'Redeploy', 'Undeploy']" var="planType">
          <g:if test="${planType != 'Deploy' || hasDelta}">
            <tr class="${planType.toUpperCase()}">
              <td>${planType}</td>
              <td>${stepType}</td>
              <td><input type="radio" name="planDetails" value="${JsonUtils.compactPrint([planType: planType, stepType: stepType, name: planType + ' - ' + title, systemFilter: filter]).encodeAsHTML()}" onclick="${remoteFunction(controller: 'plan', action:'create', update:[success:'plan-preview'], params: "'json=' + this.value")}" /></td>
            </tr>
          </g:if>
        </g:each>
      </g:each>
      <tr>
        <td colspan="3" align="center">
          <input type="submit" name="view" value="Select this plan" onClick="document.getElementById('planIdSelector').value=document.getElementById('planId').value;return true;">
          <input type="hidden" name="planId" id="planIdSelector" value="undefined" />
          <span class="spinner" style="display:none;">
            <img src="${resource(dir:'images',file:'spinner.gif')}" alt="Spinner" />
          </span>
        </td>
      </tr>
    </table>
    <div id="plan-preview" class="planDetails">Select a plan</div>
  </g:form>
</div>
