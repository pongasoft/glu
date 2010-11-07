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


package org.linkedin.glu.agent.impl.zookeeper

import org.linkedin.glu.agent.impl.storage.Storage
import org.linkedin.glu.agent.api.MountPoint

import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.ZooDefs.Ids
import org.linkedin.glu.agent.api.NoSuchMountPointException
import org.json.JSONObject
import org.apache.zookeeper.KeeperException
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.util.lang.LangUtils
import org.linkedin.groovy.util.json.JsonUtils

/**
 * Implementation of the storage using zookeeper.
 *
 * @author ypujante@linkedin.com
 */
class ZooKeeperStorage implements Storage
{
  public static final String MODULE = ZooKeeperStorage.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private static def ACLs = Ids.OPEN_ACL_UNSAFE

  private final IZKClient _zk

  ZooKeeperStorage(IZKClient zookeeper)
  {
    _zk = zookeeper
  }

  IZKClient getZooKeeperClient()
  {
    return _zk
  }

  /**
   * Make sure that a call to ZooKeeper happens only when zookeeper is connected and if an exception
   * is thrown, it gets logged but ignored. 
   */
  private def zkSafe(Closure closure)
  {
    try
    {
      if(_zk.isConnected())
        return closure()
    }
    catch(KeeperException.ConnectionLossException e)
    {
      log.warn("Call ignored ${closure.toString()} due to ConnectionLossException: zookeeper is not connected")
    }
    catch(IllegalStateException e)
    {
      log.warn("Call ignored ${closure.toString()} due to IllegalStateException: zookeeper is not connected")
    }
    catch(Throwable e)
    {
      log.warn("Call ignored ${closure.toString()} due to unexpected exception", e)
    }

    return null
  }

  public void clearState(MountPoint mountPoint)
  {
    zkSafe {
      try
      {
        _zk.delete(toPath(mountPoint))
      }
      catch (KeeperException.NoNodeException e)
      {
        if(log.isDebugEnabled())
        {
          log.debug("ignoring ok exception", e)
        }
      }
    }
  }

  private boolean hasState(mountPoint)
  {
    return _zk.exists(toPath(mountPoint)) != null
  }

  public getMountPoints()
  {
    def mountPoints = []

    if(_zk.exists('/'))
    {
      _zk.getChildren('/').each { child ->
        mountPoints << fromPath(child)
      }
    }

    return mountPoints
  }

  public void clearAllStates()
  {
    if(_zk.exists('/'))
    {
      _zk.getChildren('/').each { child ->
        _zk.delete("/${child}") 
      }
    }
  }

  public loadState(MountPoint mountPoint)
  {
    try
    {
      def data = _zk.getStringData(toPath(mountPoint))
      return JsonUtils.toMap(new JSONObject(data))
    }
    catch(KeeperException.NoNodeException e)
    {
      throw new NoSuchMountPointException(mountPoint)
    }
  }

  public void storeState(MountPoint mountPoint, state)
  {
    zkSafe {
      // modifying the state so making a copy
      state = LangUtils.deepClone(state)

      def error = state.scriptState.stateMachine.error
      if(error instanceof Throwable)
      {
        error = extractStackTrace(error, [])
        state.scriptState.stateMachine.error = error
      }
      state.scriptDefinition = state.scriptDefinition?.toExternalRepresentation()

      state = JsonUtils.toJSON(state)

      _zk.createOrSetWithParents(toPath(mountPoint),
                                 state.toString(),
                                 ACLs,
                                 CreateMode.PERSISTENT)
    }
  }

  private def extractStackTrace(exception, out)
  {
    if(exception)
    {
      out << [name: exception.getClass().name, message: exception.message]
      extractStackTrace(exception.cause, out)
    }

    return out
  }

  private String toPath(MountPoint mp)
  {
    return mp.path.replace('/', '_')
  }

  private MountPoint fromPath(String path)
  {
    return MountPoint.create(path.replace('_', '/'))
  }
}
