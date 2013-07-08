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

import org.linkedin.util.io.PathUtils
import org.restlet.representation.Representation
import org.restlet.resource.Get
import org.restlet.resource.Put

/**
 * Handles resources to get info about the processes
 *
 * @author ypujante@linkedin.com
 */
class ProcessResource extends BaseResource
{
  /**
   * GET: return information about the process(es)
   */
  @Get
  public Representation ps()
  {
    noException {
      return toRepresentation(agent.ps())
    }
  }

  /**
   * PUT: send a signal to a process
   */
  @Put
  public Representation kill(Representation representation)
  {
    noException {
      def args = toArgs(representation)

      def pid = PathUtils.removeLeadingSlash(path).toInteger()
      
      agent.kill(pid, args.signal)

      return null
    }
  }
}
