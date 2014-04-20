/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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

import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.ZooDefs.Ids
import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.impl.storage.WriteOnlyStorage
import org.linkedin.glu.groovy.utils.jvm.JVMInfo
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.lang.LangUtils
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.glu.agent.impl.storage.AgentProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Implementation of the storage using zookeeper.
 *
 * @author ypujante@linkedin.com
 */
class ZooKeeperStorage implements WriteOnlyStorage
{
  public static final String MODULE = ZooKeeperStorage.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private static def ACLs = Ids.OPEN_ACL_UNSAFE

  private final IZKClient _zkState
  private final IZKClient _zkAgentProperties

  String prefix = 'glu'

  ZooKeeperStorage(IZKClient zkState, IZKClient zkAgentProperties)
  {
    _zkState = zkState
    _zkAgentProperties = zkAgentProperties
  }

  IZKClient getZkState()
  {
    return _zkState
  }

  IZKClient getZkAgentProperties()
  {
    return _zkAgentProperties
  }

  /**
   * Make sure that a call to ZooKeeper happens only when zookeeper is connected and if an exception
   * is thrown, it gets logged but ignored. 
   */
  private def zkSafe(IZKClient zk, Closure closure)
  {
    try
    {
      if(zk.isConnected())
        return closure(zk)
    }
    catch(KeeperException.ConnectionLossException ignored)
    {
      log.warn("Call ignored ${closure.toString()} due to ConnectionLossException: zookeeper is not connected")
    }
    catch(IllegalStateException ignored)
    {
      log.warn("Call ignored ${closure.toString()} due to IllegalStateException: zookeeper is not connected")
    }
    catch(Throwable e)
    {
      log.warn("Call ignored ${closure.toString()} due to unexpected exception", e)
    }

    log.warn("Call ignored ${closure.toString()}: zookeeper is not connected")

    if(log.isDebugEnabled())
      log.debug("zkSafe => Call ignored due to zk not connected!", new Exception("where am I?"))

    return null
  }

  public void clearState(MountPoint mountPoint)
  {
    zkSafe(_zkState) { IZKClient zk ->
      try
      {
        zk.delete(mountPoint.toPathWithNoSlash())
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

  public getMountPoints()
  {
    zkSafe(_zkState) {  IZKClient zk ->
      def mountPoints = []

      if(zk.exists('/'))
      {
        zk.getChildren('/').each { child ->
          mountPoints << MountPoint.fromPathWithNoSlash(child)
        }
      }

      return mountPoints
    }
  }

  public void clearAllStates()
  {
    zkSafe(_zkState) {  IZKClient zk ->
      if(zk.exists('/'))
      {
        zk.getChildren('/').each { child ->
          zk.delete("/${child}")
        }
      }
    }
  }

  @Override
  def invalidateState(MountPoint mountPoint)
  {
    clearState(mountPoint)
    return null
  }

  public void storeState(MountPoint mountPoint, state)
  {
    zkSafe(_zkState) {  IZKClient zk ->
      // modifying the state so making a copy
      state = LangUtils.deepClone(state)

      def error = state.scriptState.stateMachine.error
      if(error instanceof Throwable)
      {
        error = extractStackTrace(error, [])
        state.scriptState.stateMachine.error = error
      }

      state = JsonUtils.compactPrint(state)

      zk.createOrSetWithParents(mountPoint.toPathWithNoSlash(),
                                state,
                                ACLs,
                                CreateMode.PERSISTENT)
    }
  }

  @Override
  AgentProperties saveAgentProperties(AgentProperties agentProperties)
  {
    // we filter out the properties
    Map<String, String> props =
      agentProperties.exposedProperties.findAll { k, v ->
        k.startsWith(prefix)
      }
    props.putAll(JVMInfo.getJVMInfo(agentProperties.exposedProperties))

    if(log.isDebugEnabled())
      log.debug "Creating/Updating agent ephemeral node: ${new TreeMap(props)}"

    zkSafe(_zkAgentProperties) { IZKClient zk ->
      zk.createOrSetWithParents('/',
                                JsonUtils.compactPrint(props),
                                Ids.OPEN_ACL_UNSAFE,
                                CreateMode.EPHEMERAL)
    }

    return agentProperties
  }

  void clearAgentProperties()
  {
    if(log.isDebugEnabled())
      log.debug "Deleting agent ephemeral node"

    zkSafe(_zkAgentProperties) { IZKClient zk ->
      zk.delete('/')
    }
  }

  @Override
  AgentProperties updateAgentProperty(String name, String value)
  {
    throw new UnsupportedOperationException("not supported in this class")
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
}
