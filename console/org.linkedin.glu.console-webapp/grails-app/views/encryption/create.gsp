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

<%@ page import="org.linkedin.glu.console.domain.Fabric" %>
<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Create New Encryption Key</title>
  <link rel="stylesheet" href="${resource(dir:'css',file:'main.css')}"/>
  <script type="text/javascript" src="${resource(dir:'js',file:'console_yui.js')}"></script>
</head>
<body>
<ul class="submenu">
  <li><g:link action="list">List</g:link></li>
  <li class="selected">New</li>
  <li><g:link action="encrypt">Encrypt/Decrypt</g:link></li>
</ul>
<div class="body">
  <h3>Create New Encryption Key</h3>
  <g:form action="ajaxSave" method="post">
    <div class="dialog">
      <table>
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
    <g:submitToRemote action="ajaxSave" update="status" value='Create'/>
  </g:form>

  <div id="status" class="info">Output Area</div>
</div>
</body>
</html>