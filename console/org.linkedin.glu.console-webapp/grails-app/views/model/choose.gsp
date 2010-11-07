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
  <h1>Load Model</h1>

  <h2>From JSON (Upload)</h2>

  <g:form action="upload" method="post" enctype="multipart/form-data">
    <input type="file" name="jsonFile" />
    <g:actionSubmit action="load" value="Upload"/>
  </g:form>

  <h2 class="separator">From JSON (URI)</h2>
  <g:form action="load">
    <table>
      <tr>
        <th>Json Uri:</th>
        <td> <g:textField name="jsonUri" value="${params.jsonUri}" class="input"/> </td>
      </tr>
    </table>
    <p>
      <g:actionSubmit action="load" value="Load"/>
    </p>
  </g:form>

  </div>
</body>
</html>
