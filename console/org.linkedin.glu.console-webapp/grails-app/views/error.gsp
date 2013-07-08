<!DOCTYPE html>
%{--
  - Copyright (c) 2010-2010 LinkedIn, Inc
  - Portions Copyright (c) 2013 Yan Pujante
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
  <title>Unexpected Error</title>
  <meta name="layout" content="main">
  <link rel="stylesheet" href="${resource(dir: 'css', file: 'errors.css')}" type="text/css">
</head>
<body>
<g:if env="development">
  <g:renderException exception="${exception}" />
</g:if>
<g:else>
  <h2>An unexpected error has occurred <a href="#" onclick="toggleShowHide('#exceptionDetails');return false;"><i class="icon-info-sign"> </i></a></h2>
  <div id="exceptionDetails" class="hidden">
    <g:renderException exception="${exception}" />
  </div>
</g:else>
</body>
</html>
