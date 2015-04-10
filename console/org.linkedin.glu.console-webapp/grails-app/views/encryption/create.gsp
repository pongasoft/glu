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
  <title>Create New Encryption Key</title>
  <link rel="stylesheet" href="${resource(dir:'css',file:'main-glu.css')}"/>
</head>
<body>
<ul class="nav nav-tabs">
  <li><cl:link action="list">List</cl:link></li>
  <li class="active"><a href="#">New</a></li>
  <li><cl:link action="encrypt">Encrypt/Decrypt</cl:link></li>
</ul>
<div class="body">
  <h3>Create New Encryption Key</h3>
  <cl:form action="ajaxSave" method="post">
    <div class="dialog">
      <table class="table table-bordered table-condensed noFullWidth">
        <tbody>

        <tr class="prop">
          <th>Name:</th>
          <td valign="top">
            <input type="text" id="keyName" name="keyName" value=""/>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
    <g:submitToRemote class="btn btn-primary" action="ajaxSave" update="status" value='Create'/>
  </cl:form>

  <div id="status" class="info">Output Area</div>
</div>
</body>
</html>