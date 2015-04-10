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
<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
  <title>Customize Dashboard</title>
  <meta name="layout" content="main"/>
</head>
<body>
<ul class="nav nav-tabs">
  <li><cl:link
    action="delta">${request.userSession.currentCustomDeltaDefinitionName.encodeAsHTML()}</cl:link></li>
  <li><cl:link controller="dashboard" action="plans">Plans</cl:link></li>
  <li class="active"><a href="#">Customize</a></li>
</ul>
<cl:form method="post" controller="dashboard" action="customize">
  <fieldset>
    <legend>Name: ${ucdd.name.encodeAsHTML()} <span class="help-inline">(to edit this field, change it in the content)</span></legend>
    <div class="clearfix">
      <label for="cddContent">Content (json)</label>
      <textarea class="noFullWidth" id="cddContent" name="content" rows="25" cols="100">${prettyPrintedContent.encodeAsHTML()}</textarea>
    </div><!-- /clearfix -->
    <div class="clearfix">
      <label for="cddShareable">Shareable</label>
      <g:checkBox id="cddShareable" name="shareable" value="${ucdd?.shareable}"/>
  </div><!-- /clearfix -->
  </fieldset>
  <div class="actions">
    <input type="submit" class="btn btn-primary" name="update" value="Save changes"/>
    <input type="submit" class="btn" name="delete" value="Delete"
                    onclick="return confirm('Are you sure?');"/>
    <a href="#dashboardSources" role="button" class="btn" data-toggle="modal" data-backdrop="true" data-keyboard="true">View Possible Sources</a>
  </div>
</cl:form>
<div id="dashboardSources" class="modal hide" role="dialog" aria-labelledby="possibleSources" aria-hidden="true">
  <div class="modal-header">
    <button type="button" class="close" data-dismiss="modal" aria-hidden="true">×</button>
    <h4 id="possibleSources">Possible sources (based on current model)</h4>
  </div>
  <div class="modal-body">
    <table class="table-condensed">
      <g:each in="${sources}" var="source">
        <tr>
          <td>${source.encodeAsHTML()}</td>
        </tr>
      </g:each>
    </table>
  </div>
</body>
</html>