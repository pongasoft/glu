/*
 * Copyright (c) 2012-2013 Yan Pujante
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

package org.linkedin.glu.agent.rest.common

import org.linkedin.glu.agent.api.AgentException
import org.linkedin.glu.groovy.utils.rest.GluGroovyRestUtils
import org.linkedin.groovy.util.rest.RestException
import org.restlet.data.Status
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author yan@pongasoft.com */
public class AgentRestUtils
{
  public static final String MODULE = AgentRestUtils.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  /**
   * This method will try to rebuild the full stack trace based on the rest exception recursively.
   * Handles the case when the client does not know about an exception
   * (or it simply cannot be created).
   */
  public static AgentException throwAgentException(Status status, RestException restException)
  {
    Throwable exception = rebuildAgentException(restException)

    if(exception instanceof AgentException)
    {
      throw exception
    }
    else
    {
      throw new AgentException(status.toString(), restException)
    }
  }

  /**
   * This method will try to rebuild the full stack trace based on the rest exception recursively.
   * Handles the case when the client does not know about an exception
   * (or it simply cannot be created).
   */
  public static Throwable rebuildAgentException(RestException restException)
  {
    GluGroovyRestUtils.rebuildException(restException)
  }
}