<!DOCTYPE html>%{--
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
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
  <title>GLU Console Login</title>
  <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'bootstrap.min.css')}"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'main-glu.css')}"/>
  <style type="text/css">
  body {
    padding-top: 0;
  }

  .logo {
    float: right;
  }

  .form {
    padding: 1.5em;
  }

  div#footer {
    clear: both;
    margin-top: 2em;
    padding-right: 0.5em;
    border-top: solid 1px #dddddd;
    text-align: right;
    font-style: italic;
    font-size: 0.8em;
    color: #aaaaaa;
  }
  </style>
  <g:javascript library="jquery" plugin="jquery"/>
  <r:layoutResources/>
  <g:javascript src="bootstrap.min.js" />
</head>
<body OnLoad="document.login.username.focus();">
<g:render template="/layouts/flash"/>
<div class="content">
  <div class="form">
  <div class="logo"><img src="${resource(dir: 'images', file: 'glu_480_white.png')}" alt="glu deployment automation platform"/></div>
    <h1>Console Login</h1>
  <g:form action="signIn" name="login">
    <input type="hidden" name="targetUri" value="${targetUri}" />
    <table class="table noFullWidth">
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
          <td><input class="btn btn-primary" type="submit" value="Sign in" /></td>
        </tr>
      </tbody>
    </table>
  </g:form>
  </div>
  </div>
<div id="footer">
  GLU Console - ${grailsApplication?.metadata?.getAt('app.version')}
</div>
<r:layoutResources/>
</body>
</html>
