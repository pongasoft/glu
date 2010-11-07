%{--
  - Copyright 2010-2010 LinkedIn, Inc
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

<%@ page import="org.linkedin.glu.provisioner.core.model.LogicSystemFilterChain" %>
<g:if test="${filter}">
  <g:if test="${filter instanceof LogicSystemFilterChain}">
    <ul>
      <g:each in="${filter.filters}" var="${f}" status="i">
        <g:if test="${i > 0}"><li>${filter.kind.encodeAsHTML()}</li></g:if>
        <li><g:render template="systemFilter" model="[filter: f]"/></li>
      </g:each>
    </ul>
  </g:if>
  <g:else>
    ${filter}
  </g:else>
</g:if>