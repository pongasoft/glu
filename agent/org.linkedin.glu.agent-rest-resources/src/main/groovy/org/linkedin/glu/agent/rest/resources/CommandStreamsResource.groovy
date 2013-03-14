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

package org.linkedin.glu.agent.rest.resources

import org.linkedin.glu.agent.rest.common.InputStreamOutputRepresentation
import org.restlet.data.Status
import org.restlet.representation.Representation
import org.restlet.resource.Get

/**
 * @author yan@pongasoft.com */
public class CommandStreamsResource extends CommandBaseResource
{
  /**
   * return the stream(s) and information about the command
   */
  @Get
  public Representation streamCommandResults()
  {
    noException {

      def res = agent.streamCommandResults(requestArgs)

      def stream = res.remove('stream')

      res.each { k,v ->
        addResponseHeader("X-glu-command-${k}", v)
      }

      if(stream)
        return new InputStreamOutputRepresentation(stream)
      else
      {
        response.setStatus(Status.SUCCESS_NO_CONTENT)
        return null
      }
    }
  }

}