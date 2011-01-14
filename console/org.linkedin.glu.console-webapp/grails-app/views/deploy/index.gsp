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

<html>
<head>
  <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
  <meta name="layout" content="main"/>
  <title>Deployment</title>
  <style type="text/css">
  table {
    width: 100%;
  }
  tr.header {
    border: solid 2px #000000;
  }
  </style>
</head>
<body>
<div class="body">
  <g:form method="post">
    Manifest base directory: <input type="text" name="manif" value="${manif}"/>
    <span class="button"><g:actionSubmit class="updateManif" value="Update"/></span>
  </g:form>

  <h1>Applications</h1>

  <g:renderErrors bean="system.bom"/>
  <g:renderErrors bean="system.topology"/>
  <g:renderErrors bean="system"/>
  ${topology}
  <g:each in="${bom}" status="i" var="entry">
    <li>${entry}</li>
  </g:each>
</div>
</body>
</html>
