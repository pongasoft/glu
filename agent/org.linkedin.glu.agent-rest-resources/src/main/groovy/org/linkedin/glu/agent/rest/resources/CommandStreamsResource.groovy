/*
 * Copyright (c) 2012 Yan Pujante
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

import org.restlet.Context
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.Representation
import org.restlet.representation.Variant
import org.linkedin.glu.agent.rest.common.InputStreamOutputRepresentation
import org.restlet.representation.EmptyRepresentation
import org.restlet.data.Status

/**
 * @author yan@pongasoft.com */
public class CommandStreamsResource extends CommandBaseResource
{
  CommandStreamsResource(Context context, Request request, Response response)
  {
    super(context, request, response);
  }

  @Override
  boolean allowGet()
  {
    return true
  }

  /**
   * GET: return the stream(s) and information about the command
   */
  public Representation represent(Variant variant)
  {
    return noException {

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
        return EmptyRepresentation.createEmpty()
      }
    }
  }

}