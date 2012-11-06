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
import org.restlet.representation.InputRepresentation
import org.restlet.representation.Representation

/**
 * @author yan@pongasoft.com */
public class CommandsResource extends BaseResource
{
  CommandsResource(Context context, Request request, Response response)
  {
    super(context, request, response);
  }

  @Override
  boolean allowPost()
  {
    return true
  }

  /**
   * Handle POST
   */
  @Override
  public void acceptRepresentation(Representation representation)
  {
    noException {
      def args = toArgs(request.originalRef.queryAsForm)

      // stdin will be copied right away in executeShellCommand
      if(representation instanceof InputRepresentation)
        args.stdin = representation.stream

      def res

      if(args.type == "shell")
      {
        res = agent.executeShellCommand(args)
        response.setEntity(toRepresentation(res: res))
      }
      else
        throw new UnsupportedOperationException("unknown command type [${args.toString()}]")
    }
  }
}