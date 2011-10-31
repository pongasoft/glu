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

<%@ page import="org.linkedin.glu.grails.utils.ConsoleConfig" %>
<html>
<head>
  <title><cl:withFabric>[${fabric}] </cl:withFabric><g:layoutTitle/> - GLU Console</title>
  <link rel="stylesheet" href="${resource(dir:'css/twitter-bootstrap',file:'bootstrap.min.css')}"/>
  <link rel="stylesheet" href="${resource(dir:'css',file:'main.css')}"/>
  <link rel="shortcut icon" href="${resource(dir:'images',file:'favicon.ico')}" type="image/x-icon"/>
  <style type="text/css">
  <cl:withFabric>
    .topbar .fill {
      background: ${fabric.color};
      background: -webkit-gradient(linear, left top, right top, color-stop(.2, ${fabric.color}),color-stop(1, #ffffff)) fixed no-repeat 0 100%;
      background: -moz-linear-gradient(left,${fabric.color} 20%,#ffffff) fixed no-repeat 0 100%;
    }
  .topbar a {
    text-shadow: 1px 1px 1px ${fabric.color};
  }
  </cl:withFabric>
  div#footer {
    margin-top: 2em;
    border-top: solid 1px #dddddd;
    text-align: right;
    font-style: italic;
    font-size: 0.8em;
    color: #aaaaaa;
  }
  #footer-text {
    position: relative;
    top: -8px;
    left: -30px;
  }
  #footer-image {
    margin-top: 2px;
  }
    <cl:renderTagsCss/>
  </style>
  <g:layoutHead/>
  <cl:renderCustomCss/>
  <g:javascript library="jquery" plugin="jquery"/>
  <g:javascript src="twitter-bootstrap/bootstrap-modal.js" />
  <g:javascript src="twitter-bootstrap/bootstrap-dropdown.js" />
  <g:javascript library="yui" />
  <g:javascript src="prototype/effects.js" />
  <g:javascript library="application" />
</head>
<body onload="${pageProperty(name:'body.onload')}">
<g:set var="zkStatus" value="zk-unknown" scope="request"/>
<shiro:hasRole name="ADMIN"><g:set var="isAdminUser" value="true"/></shiro:hasRole>
<cl:withOrWithoutSystem>
<div class="topbar" data-dropdown="dropdown">
  <div class="fill">
    <div class="container">
      <ul class="nav">
        <li ${cl.navbarEntryClass(entry: 'Dashboard')} id="tab-dashboard"><g:link controller="dashboard">Dashboard</g:link></li>
        <g:if test="${fabric}">
          <li ${cl.navbarEntryClass(entry: 'Plans')} id="tab-plans"><g:link controller="plan" action="deployments">Deployments</g:link></li>
          <li ${cl.navbarEntryClass(entry: 'Model')} id="tab-model"><g:link controller="model" action="list">Model</g:link></li>
        </g:if>
        <g:if test="${isAdminUser}"><li ${cl.navbarEntryClass(entry: 'Admin')} id="tab-admin"><g:link controller="admin">Admin</g:link></li></g:if>
        <li ${cl.navbarEntryClass(entry: 'User')} id="tab-home"><g:link controller="home"><span id="tab-home-username">${user.username.encodeAsHTML()}</span></g:link></li>
        <li ${cl.navbarEntryClass(entry: 'Help')} id="tab-help"><g:link controller="help" action="index">Help</g:link></li>
      </ul>
      <ul class="nav secondary-nav">
        <cl:whenDisconnectedFabric><li><span class="label important">not connected!</span></li></cl:whenDisconnectedFabric>
        <cl:renderFabricSelectDropdown/>
        <g:each in="${ConsoleConfig.getInstance().defaults.model}" var="h">
          <li><g:link controller="system" action="filter_values" id="${h.name}"><span id="header-info-${h.name}">${request[h.name]?.name ?: 'All [' + h.name + ']'}</span><g:each in="${h.header}" var="m"><g:if test="${request[h.name]?.getAt(m)}">:${request[h.name][m]}</g:if></g:each></g:link></li>
        </g:each>
        <g:each in="${ConsoleConfig.getInstance().defaults.header}" var="hm">
          <g:each in="${hm}" var="k">
            <g:each in="${k.value}" var="rv">
              <li><span id="header-info-${k.key}-${rv}">${request['system']?.(k.key)?.get(rv)}</span></li>
            </g:each>
          </g:each>
        </g:each>
      </ul>
    </div>
  </div>
</div>
</cl:withOrWithoutSystem>
<div class="container">
  <g:render template="/layouts/flash"/>
  <g:layoutBody/>
</div>
<div id="footer">
  <div id="footer-image"><img src="${resource(dir: 'images', file: 'glu_100_white.png')}" alt="glu deployment automation platform"/></div>
  <div id="footer-text">GLU Console - ${grailsApplication?.metadata?.getAt('app.version')}</div>
</div>
</body>
</html>