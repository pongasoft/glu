/*
 * Copyright (c) 2010-2011 LinkedIn, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.linkedin.glu.samples.webapp;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author yan@pongasoft.com
 */
public class MonitorServlet extends HttpServlet
{
  public static enum MonitorState
  {
    GOOD(HttpServletResponse.SC_OK, "Everything is fine"),
    BUSY(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Application is overloaded (recoverable)"),
    DEAD(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Application is in a dead state (non recoverable)");

    private final int _responseCode;
    private final String _description;

    private MonitorState(int responseCode, String description)
    {
      _responseCode = responseCode;
      _description = description;
    }

    public int getResponseCode()
    {
      return _responseCode;
    }

    public String getDescription()
    {
      return _description;
    }
  }

  public static final String WEBAPP_STATE_ATTRIBUTE = "monitor.webapp.state";

  public void init() throws ServletException
  {
    getServletContext().setAttribute(WEBAPP_STATE_ATTRIBUTE, MonitorState.GOOD);
  }

  @Override
  protected void doHead(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    MonitorState state = (MonitorState) getServletContext().getAttribute(WEBAPP_STATE_ATTRIBUTE);

    response.setHeader("X-glu-monitoring", state.name() + " [" + state.getDescription() + "]");

    response.sendError(state.getResponseCode(), state.name());
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    request.getRequestDispatcher("/monitor.jsp").forward(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException
  {
    String newState = request.getParameter(WEBAPP_STATE_ATTRIBUTE);

    if(newState != null)
    {
      getServletContext().setAttribute(WEBAPP_STATE_ATTRIBUTE, MonitorState.valueOf(newState));
    }

    response.sendRedirect("monitor");
  }
}
