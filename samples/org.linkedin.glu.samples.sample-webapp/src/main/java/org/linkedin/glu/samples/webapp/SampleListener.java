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

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * @author yan@pongasoft.com
 */
public class SampleListener implements ServletContextListener
{

  // Public constructor is required by servlet spec
  public SampleListener()
  {
  }

  public void contextInitialized(ServletContextEvent sce)
  {
    sce.getServletContext().log("Initializing sample webapp...");

    try
    {
      // artificial wait time to simulate initialization
      Thread.sleep(1500);

      if(sce.getServletContext().getContextPath().equals("/fail"))
      {
        sce.getServletContext().log("Failing");
        throw new RuntimeException("does not boot");
      }
      else
      {
        sce.getServletContext().log("Not failing");
      }

      // artificial wait time to simulate initialization
      Thread.sleep(1500);
    }
    catch(InterruptedException e)
    {
      throw new RuntimeException(e);
    }

    sce.getServletContext().log("Initialized.");
  }

  public void contextDestroyed(ServletContextEvent sce)
  {
    sce.getServletContext().log("Sample webapp destroyed.");
  }
}
