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

<g:if test="${delta}">
  <h2>${title ?: 'Delta'}</h2>
  <g:form controller="plan" action="redirectView">
    <select name="planId" onchange="${remoteFunction(controller: 'plan', action:'renderDelta', update:[success:'selectedDelta-' + System.identityHashCode(delta)], params: "'id=' + this.value")}">
      <option value="0" selected="true">Choose Plan</option>
      <g:each in="${delta}" var="plan">
        <option value="${plan.id}">${plan.name} [${plan.leafStepsCount}]</option>
      </g:each>
    </select>
    <input type="submit" name="view" value="Select this plan">
    <span class="spinner" style="display:none;">
      <img src="${resource(dir:'images',file:'spinner.gif')}" alt="Spinner" />
    </span>
  </g:form>
  <div id="selectedDelta-${System.identityHashCode(delta)}" class="deltaDetails">
  </div>
</g:if>
