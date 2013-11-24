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

<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Load Model</title>
</head>
<body>
<div class="body">
  <ul class="nav nav-tabs">
    <li><cl:link action="list">List</cl:link></li>
    <li class="active"><a href="#">Load</a></li>
  </ul>

  <g:if test="${syntaxError}">
    <div id="syntaxError" class="syntax-error alert alert-error fade in" >
      <h4>${syntaxError.message}</h4>
<pre>
${syntaxError.excerpt}
</pre>
    </div>
  </g:if>
  <div class="row">
    <div class="span12">
      <cl:form action="upload" method="post" enctype="multipart/form-data">
        <fieldset>
          <legend>From JSON (Upload)</legend>
          <div class="clearfix">
            <div class="input">
              <input type="file" name="jsonFile" />
            </div>
          </div>
          <div class="actions">
            <g:actionSubmit class="btn btn-primary" action="load" value="Upload"/>
          </div>
        </fieldset>
      </cl:form>
    </div>
  </div>
  <div class="row">
    <div class="span12">
      <cl:form action="load">
        <fieldset>
          <legend>From JSON (URI)</legend>
          <div class="clearfix">
            <g:textField name="jsonUri" value="${params.jsonUri}" class="input-xxlarge"/>
          </div>
          <div class="actions">
            <g:actionSubmit class="btn btn-primary" action="load" value="Load"/>
          </div>
        </fieldset>
      </cl:form>
    </div>
  </div>
</div>
</body>
</html>
