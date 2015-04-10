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
  <title>Encrypt Plain Text</title>
  <link rel="stylesheet" href="${resource(dir:'css',file:'main-glu.css')}"/>
</head>
<body>
<ul class="nav nav-tabs">
  <li><cl:link action="list">List</cl:link></li>
  <li><cl:link action="create">New</cl:link></li>
  <li class="active"><a href="#">Encrypt/Decrypt</a></li>
</ul>
<div class="body">
  <h3>Encrypt Plain Text</h3>
  <cl:form name="encryptForm" action="ajaxEncrypt" method="post">
    <div class="dialog">
      <table class="table table-bordered table-condensed noFullWidth">
        <tbody>
        <tr class="prop">
          <th>Select Encryption Key:</th>
          <td valign="top">
            <g:select name="keyName" from="${keyNames}" value="${params.keyName ?: keyNames[0]}" />
          </td>
        </tr>
        <tr class="prop">
          <th>Enter Plain Text String:</th>
          <td valign="top">
            <input type="text" id="plainText" name="plainText" value=""${params.plainText ?: ''}"/>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
    <g:submitToRemote class="btn btn-primary" action="ajaxEncrypt" update="status" value='Encrypt'/>
  </cl:form>

  <h3>Decrypt Text</h3>
  <cl:form name="decryptForm" action="ajaxDecrypt" method="post">
    <div class="dialog">
      <table class="table table-bordered table-condensed noFullWidth">
        <tbody>
        <tr class="prop">
          <th>Enter Encrypted String:</th>
          <td valign="top">
            <input type="text" id="encrypted" name="encrypted" value=""${params.encrypted ?: ''}"/>
          </td>
        </tr>
        </tbody>
      </table>
    </div>
    <g:submitToRemote class="btn btn-primary" action="ajaxDecrypt" update="status" value='Decrypt'/>
  </cl:form>

  <div id="status" class="info">Output Area</div>

</div>
</body>
</html>