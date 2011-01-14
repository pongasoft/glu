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
  --}%<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <title>GLU Console Login</title>
  <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'main.css')}"/>
  <style type="text/css">
  .logo {
    float: right;
  }

  .form {
    padding: 1.5em;
  }

  div#footer {
    clear: both;
    margin-top: 2em;
    border-top: solid 1px #dddddd;
    text-align: right;
    font-style: italic;
    font-size: 0.8em;
    color: #aaaaaa;
  }
  </style>
</head>
<body>
<g:render template="/layouts/flash"/>
<div class="content">
  <div class="form">
    <div class="logo"><img src="${resource(dir: 'images', file: 'glu_480_white.png')}" alt="glu deployment automation platform"/></div>
    <h1>Console Login</h1>
  <g:form action="signIn">
    <input type="hidden" name="targetUri" value="${targetUri}" />
    <table>
      <tbody>
        <tr>
          <th>Username:</th>
          <td><input type="text" name="username" value="${username}" /></td>
        </tr>
        <tr>
          <th>Password:</th>
          <td><input type="password" name="password" value="" /></td>
        </tr>
        <tr>
          <td></td>
          <td><input type="submit" value="Sign in" /></td>
        </tr>
      </tbody>
    </table>
  </g:form>
  </div>
  </div>
<div id="footer">
  GLU Console - ${grailsApplication?.metadata?.getAt('app.version')}
</div>
</body>
</html>
