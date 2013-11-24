%{--
  - Copyright (c) 2011-2013 Yan Pujante
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
<%@ page import="org.linkedin.glu.console.controllers.PlanController; org.linkedin.glu.grails.utils.ConsoleConfig; org.linkedin.groovy.util.json.JsonUtils" %>
<table class="table table-bordered xtight-table">
  <tr>
    <th>Model</th>
    <td><cl:renderSystemId system="${request.system}"/></td>
    <th>Filter</th>
    <td class="systemFilter"><cl:renderSystemFilter filter="${request.system.filters}" renderRemoveLink="false"/></td>
  </tr>
</table>
<div id="select-plan">
  <cl:form controller="plan" action="redirectView">
    <table id="select-plan-radio" class="noFullWidth table table-bordered tight-table">
      <tr>
        <th colspan="6">${title?.encodeAsHTML()}</th>
      </tr>
      <g:each in="${ConsoleConfig.getInstance().defaults.plans ?: PlanController.DEFAULT_PLANS}" var="plan">
        <g:if test="${plan.planType != 'deploy' || hasDelta}">
          <tr class="plan-type-${plan.planType.toUpperCase()}">
            <td>${plan.displayName ?: plan.planType.capitalize()}</td>
            <g:each in="${['SEQUENTIAL', 'PARALLEL']}" var="stepType">
              <td>${stepType}</td>
              <td><input type="radio" name="planDetails" value="${JsonUtils.compactPrint([*:plan, stepType: stepType, name: (plan.displayName ?: plan.planType.capitalize()) + ' - ' + title, systemFilter: filter]).encodeAsHTML()}" onclick="${remoteFunction(controller: 'plan', action:'create', update:[success:'plan-preview'], params: "'fabric=${request.fabric.name.encodeAsHTML()}&json=' + this.value")}" /></td>
            </g:each>
          </tr>
        </g:if>
      </g:each>
      <tr>
        <td colspan="6" style="text-align: center;">
          <input class="btn btn-primary" type="submit" name="view" value="Select this plan" onClick="document.getElementById('planIdSelector').value=document.getElementById('planId').value;return true;">
          <g:if test="${missingAgents}">
            <a href="#missingAgents" role="button" class="btn btn-danger" data-toggle="modal" data-backdrop="true" data-keyboard="true" >Missing Agents</a>
            <div id="missingAgents" class="modal hide" role="dialog" aria-labelledby="possibleSources" aria-hidden="true">
              <div class="modal-header">
                  <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
              <h4>Missing agents [<cl:link controller="fabric" action="listAgentFabrics">Fix it</cl:link>]</h4>
              <div class="modal-body">
                <ul>
                  <g:each in="${missingAgents}" var="agentName">
                    <li>${agentName.encodeAsHTML()}</li>
                  </g:each>
                </ul>
              </div>
            </div>
          </g:if>
          <input type="hidden" name="planId" id="planIdSelector" value="undefined" />
          <span class="spinner" style="display:none;">
            <img src="${resource(dir:'images',file:'spinner.gif')}" alt="Spinner" />
          </span>
        </td>
      </tr>
    </table>
    <div id="plan-preview">Select a plan</div>
  </cl:form>
</div>
