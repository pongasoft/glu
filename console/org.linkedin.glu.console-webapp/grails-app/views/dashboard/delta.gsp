%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
  - Portions Copyright (c) 2011 Yan Pujante
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
  <title>Dashboard</title>
  <meta name="layout" content="main"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'delta.css')}"/>
</head>
<body>
<ul class="tabs">
  <li class="active" data-dropdown="dropdown">
    <cl:renderDashboardSelectDropdown/>
  </li>
  <li><g:link controller="dashboard" action="plans">Plans</g:link></li>
  <li><g:link controller="dashboard" action="customize">Customize</g:link></li>
</ul>
<div id="saveAsNew" class="modal hide">
  <a href="#" class="close">&times;</a>
  <div class="modal-header">Save as new dashboard</div>
  <div class="modal-body">
   <g:form class="form-stacked" controller="dashboard" action="saveAsNewCustomDashboard">
     <fieldset>
       <legend>New Name</legend>
       <div class="clearfix">
         <input id="customDashboardNewName" type="text" name="name" value="${request.userSession?.currentCustomDeltaDefinitionName?.encodeAsHTML()}"/>
       </div><!-- /clearfix -->
     </fieldset>
     <div class="actions">
       <input type="submit" class="btn primary" name="update" value="Save"/>
     </div>
   </g:form>
  </div>
</div>
<g:render template="delta"/>
</body>
</html>