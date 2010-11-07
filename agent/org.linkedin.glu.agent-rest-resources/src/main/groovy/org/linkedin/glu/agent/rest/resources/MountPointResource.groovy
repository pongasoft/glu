/*
 * Copyright 2010-2010 LinkedIn, Inc
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
import org.linkedin.groovy.util.rest.RestException
import org.linkedin.util.lang.LangUtils
import org.restlet.Context
import org.restlet.Request
import org.restlet.Response
import org.restlet.representation.Representation
import org.restlet.representation.Variant

/**
 * Represents a script resource
 *
 * @author ypujante@linkedin.com
 */
class MountPointResource extends BaseResource
{
  public static final String MODULE = MountPointResource.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  MountPointResource(Context context, Request request, Response response)
  {
    super(context, request, response);
  }


  public boolean allowPut()
  {
    return true
  }

  public boolean allowPost()
  {
    return true
  }

  public boolean allowDelete()
  {
    return true
  }

  public boolean allowGet()
  {
    return true
  }


  /**
   * GET: return the state + others (tbd)
   * GET with query string: wait for state / action
   */
  public Representation represent(Variant variant)
  {
    return noException {
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
  public void storeRepresentation(Representation representation)
  {
    noException {
      agent.installScript(toArgs(representation))
    }
  }

  /**
   * POST (execute action / execute action and wait / execute call / clear Error / interrupt action)
   */
  public void acceptRepresentation(Representation representation)
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
        response.setEntity(computeRepresentationResult(res))
      }
      else
      {
        throw new IllegalArgumentException("unkown action ${args}")
      }
    }
  }

  private Representation computeRepresentationResult(res)
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
  public void removeRepresentations()
  {
    noException {
      def force = query.getFirstValue('force')
      agent.uninstallScript(mountPoint: getMountPoint(), force: force) 
    }
  }

  private MountPoint getMountPoint()
  {
    return MountPoint.fromPath(path)
  }
}
