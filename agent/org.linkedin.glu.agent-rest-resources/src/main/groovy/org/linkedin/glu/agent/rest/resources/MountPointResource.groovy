/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2012-2013 Yan Pujante
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

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.rest.common.InputStreamOutputRepresentation
import org.linkedin.groovy.util.rest.RestException
import org.linkedin.util.lang.LangUtils
import org.restlet.representation.Representation
import org.restlet.resource.Delete
import org.restlet.resource.Get
import org.restlet.resource.Post
import org.restlet.resource.Put

/**
 * Represents a script resource
 *
 * @author ypujante@linkedin.com
 */
class MountPointResource extends BaseResource
{
  public static final String MODULE = MountPointResource.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  /**
   * GET: return the state + others (tbd)
   * GET with query string: wait for state / action
   */
  @Get
  public Representation getMountPointsOrWaitForState()
  {
    noException {
      def form = request.originalRef.queryAsForm
      if(form)
      {
        def args = toArgs(form)
        args.mountPoint = getMountPoint()
        def res

        if(args.state)
        {
          // wait for state
          res = agent.waitForState(args)
        }
        else
        {
          if(args.actionId)
          {
            res = agent.waitForAction(args)
          }
          else
          {
            throw new IllegalArgumentException("only 'state' or 'actionId' supported")
          }
        }

        return toRepresentation([res: res])
      }
      else
      {
        def mp = getMountPoint()
        // returns information about the mount point
        def state = agent.getFullState(mountPoint: mp)
        def error = state.scriptState.stateMachine.error
        if(error instanceof Throwable)
        {
          // modifying the state so making a copy
          state = LangUtils.deepClone(state)
          error = RestException.toJSON(error)
          state.scriptState.stateMachine.error = error
        }
        return toRepresentation([fullState: state])
      }
    }
  }

  /**
   * PUT (install script)
   */
  @Put
  public Representation installScript(Representation representation)
  {
    noException {
      def args = toArgs(representation)
      args.mountPoint = getMountPoint() // making sure that the mountPoint is coming from the path (glu-178)
      agent.installScript(args)
      return null
    }
  }

  /**
   * POST (execute action / execute action and wait / execute call / clear Error / interrupt action)
   */
  @Post
  public Representation executeOnMountPoint(Representation representation)
  {
    noException {
      def args = toArgs(representation)

      def action = [
          'executeAction',
          'executeActionAndWait',
          'executeActionAndWaitForState',
          'executeCall',
          'clearError',
          'interruptAction'
      ].find { args.containsKey(it)}

      if(action)
      {
        args[action].mountPoint = getMountPoint()
        def res = agent."${action}"(args[action])
        computeRepresentationResult(res)
      }
      else
      {
        throw new IllegalArgumentException("unkown action ${args}")
      }
    }
  }

  private static Representation computeRepresentationResult(res)
  {
    if(res instanceof InputStream)
    {
      new InputStreamOutputRepresentation(res)
    }
    else
    {
      toRepresentation([res: res])
    }
  }

  /**
   * DELETE (uninstall script)
   */
  @Delete
  public Representation uninstallScript()
  {
    noException {
      def force = query.getFirstValue('force')
      agent.uninstallScript(mountPoint: getMountPoint(), force: force)
      return null
    }
  }

  private MountPoint getMountPoint()
  {
    return MountPoint.fromPath(path)
  }
}
