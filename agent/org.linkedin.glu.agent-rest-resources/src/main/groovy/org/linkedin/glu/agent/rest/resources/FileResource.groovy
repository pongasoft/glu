/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2013 Yan Pujante
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
import org.linkedin.util.io.PathUtils
import org.restlet.data.Status
import org.restlet.representation.Representation
import org.restlet.resource.Get

/**
 * Handles resources of type files (tail / ls)
 *
 * @author ypujante@linkedin.com */
class FileResource extends BaseResource
{
  /**
   * GET: get the content of the file/directory
   */
  @Get
  public Representation getFileContent()
  {
    noException {
      def args = toArgs(request.originalRef.queryAsForm)
      args.location = PathUtils.removeLeadingSlash(path)

      def res = agent.getFileContent(args)
      if(res instanceof InputStream)
      {
        return new InputStreamOutputRepresentation(res)
      }
      else
      {
        if(res == null)
        {
          response.setStatus(Status.CLIENT_ERROR_NOT_FOUND)
          return null
        }

        if(res.tailStream)
        {
          def stream = res.remove('tailStream')
          res.each { k,v ->
            addResponseHeader("X-glu-file-${k}", v)
          }
          return new InputStreamOutputRepresentation(stream)
        }
        else
          return toRepresentation([res: res])
      }
    }
  }
}
