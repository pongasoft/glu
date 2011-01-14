<%@ page import="org.linkedin.glu.samples.webapp.MonitorServlet" %>
<%--
~ Copyright (c) 2011 Yan Pujante
~
~ Licensed under the Apache License, Version 2.0 (the "License"); you may not
~ use this file except in compliance with the License. You may obtain a copy of
~ the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing, software
~ distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
~ WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
~ License for the specific language governing permissions and limitations under
~ the License.
--%>
<html>
<head>
  <title>Monitor - Sample Webapp</title>
  <style type="text/css">
    table {
      padding-top: 2em;
    }
    td {
      border: solid 1px black;
    }
    .GOOD {
      color: green;
    }
    .BUSY {
      color: orange;
    }
    .DEAD {
      color: red;
    }
  </style>
</head>
<body>
<%! MonitorServlet.MonitorState currentState; %>
<% currentState = (MonitorServlet.MonitorState)
      getServletConfig().getServletContext().getAttribute(MonitorServlet.WEBAPP_STATE_ATTRIBUTE); %>
<h1>Monitor for <%= getServletConfig().getServletContext().getContextPath() %></h1>
Current state reported:
<span class="<%= currentState %>" style="font-weight: bold; font-size: 2em;">
  <%= currentState %>
</span>

<form method="POST" action="monitor">
  <table>
    <tr>
      <th>Select One</th>
      <th>Value</th>
      <th>Description</th>
    </tr>

    <% for(MonitorServlet.MonitorState state : MonitorServlet.MonitorState.values()) { %>
    <tr>
      <td>
        <input type="radio"
               name="<%= MonitorServlet.WEBAPP_STATE_ATTRIBUTE %>"
               value="<%= state %>"
               <%= currentState == state ? "checked" : ""%>>
      </td>
      <td><%= state %></td>
      <td><%= state.getDescription() %></td>
    </tr>
    <% } %>

    <tr>
      <td colspan="3">
        <input type="submit" name="changeState" value="Change monitor state"/>
      </td>
    </tr>
  </table>
</form>
</body>
</html>