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

<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Load Model</title>
  <script type="text/javascript" src="${resource(dir:'js',file:'console_yui.js')}"></script>
  <style type="text/css">
  table {
    width: 100%;
  }
  td {
   vertical-align: top;
  }
  .input {
    width: 100%;
  }

    th {
      width: 20%;
    }
    td.headline {
      padding-top: 1em;
      padding-bottom: 1em;
      
    }

    .separator {
      border-top: 1px solid black;
      padding-top: 1.5em;
    }
  </style>
</head>
<body>
<div class="body">
  <ul class="tabs">
    <li><g:link action="list">List</g:link></li>
    <li class="active"><a href="#">Load</a></li>
  </ul>

  <div class="row">
    <div class="span12">
      <g:form class="form-stacked" action="upload" method="post" enctype="multipart/form-data">
        <fieldset>
          <legend>From JSON (Upload)</legend>
          <div class="clearfix">
            <div class="input">
              <input type="file" name="jsonFile" />
            </div>
          </div>
        </fieldset>
        <div class="actions">
          <g:actionSubmit class="btn primary" action="load" value="Upload"/>
        </div>
      </g:form>
    </div>
  </div>
  <div class="row">
    <div class="span12">
      <g:form class="form-stacked" action="load">
        <fieldset>
          <legend>From JSON (URI)</legend>
          <div class="clearfix">
            <g:textField name="jsonUri" value="${params.jsonUri}" class="input"/>
          </div>
        </fieldset>
        <div class="actions">
          <g:actionSubmit class="btn primary" action="load" value="Load"/>
        </div>
      </g:form>
    </div>
  </div>
</div>
</body>
</html>
