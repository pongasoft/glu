/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.provisioner.services.fabric

import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.KeeperException.NoNodeException
import org.linkedin.util.lifecycle.Configurable
import org.linkedin.util.clock.Timespan
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.util.lifecycle.Destroyable
import org.linkedin.glu.agent.rest.client.ConfigurableFactory
import org.linkedin.util.annotations.Initializable
import org.linkedin.zookeeper.client.IZKClient

/**
 * This service will manage fabrics
 *
 * @author ypujante@linkedin.com */
class FabricServiceImpl implements FabricService, Destroyable
{
  public static final String MODULE = FabricServiceImpl.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  @Initializable
  ConfigurableFactory configurableFactory

  @Initializable(required = true)
  FabricStorage fabricStorage

  @Initializable
  String zookeeperRoot = "/org/glu"

  @Initializable
  String prefix = "glu"

  @Initializable
  Timespan zkClientWaitForStartTimeout = Timespan.parse('5s')

  /**
   * In memory cache of fabrics (small list, changes rarely...)
   */
  private volatile Map<String, FabricWithZkClient> _fabrics = [:]

  /**
   * @return a collection of all the fabrics known by the service (cached list)
   */
  Collection<Fabric> getFabrics()
  {
    return loadFabrics().values().collect { it.fabric }
  }

  /**
   * @return a map where the key is the agent name and the value is the fabric name the agent
   * belongs (as defined in ZooKeeper)
   */
  Map<String, String> getAgents()
  {
    def agents = [:]

    loadFabrics().values().zkClient.each {
      agents.putAll(getAgents(it))
    }

    return agents
  }

  /**
   * @param zkClient which ZooKeeper connection to use
   * @return a map where the key is the agent name and the value is the fabric the agent has been
   * assigned to or <code>null</code> if the agent has not been assigned to any fabric
   */
  private Map<String, String> getAgents(IZKClient zkClient)
  {
    def agents = [:]

    if(zkClient)
    {
      try
      {
        zkClient.getChildren("${zookeeperRoot}/agents/names").each { agent ->
          def path = "${zookeeperRoot}/agents/names/${agent}/fabric"

          if(zkClient.exists(path))
          {
            agents[agent] = zkClient.getStringData(path)
          }
          else
          {
            agents[agent] = null
          }
        }
      }
      catch(NoNodeException e)
      {
        if(log.isDebugEnabled())
        {
          log.debug("ignored exception", e)
        }
      }
    }

    return agents
  }

  /**
   * @return the fabric or <code>null</code>
   */
  Fabric findFabric(String fabricName)
  {
    return findFabricWithZkClient(fabricName)?.fabric
  }

  /**
   * @return the fabric or <code>null</code>
   */
  private FabricWithZkClient findFabricWithZkClient(String fabricName)
  {
    return loadFabrics()[fabricName]
  }

  /**
   * @return the list of fabric names
   */
  Collection<String> listFabricNames()
  {
    return loadFabrics().keySet().collect { it }
  }

  /**
   * Executes the closure with the ZooKeeper connection (instance of {@link IZKClient}) associated
   * to the fabric
   *
   * @return whatever the closure returns
   */
  def withZkClient(String fabricName, Closure closure)
  {
    closure(findFabricWithZkClient(fabricName)?.zkClient)
  }

  @Override
  boolean isConnected(String fabricName)
  {
    withZkClient(fabricName) { IZKClient zkClient ->
      zkClient?.isConnected()
    }
  }

  /**
   * Sets the fabric for the given agent: write it in ZooKeeper and configure the agent as well.
   */
  void setAgentFabric(String agentName, String fabricName)
  {
    withZkClient(fabricName) { zkClient ->
      zkClient.createOrSetWithParents("${zookeeperRoot}/agents/hosts/${agentName}/fabric",
                                      fabricName,
                                      Ids.OPEN_ACL_UNSAFE,
                                      CreateMode.PERSISTENT)
    }

    def zkConnectString = findFabric(fabricName).zkConnectString
    configurableFactory?.withRemoteConfigurable(agentName) { Configurable c ->
      c.configure(["${prefix}.agent.zkConnectString": zkConnectString])
    }

  }

  /**
   * resets the fabrics cache which will force the connection to ZooKeeper to be dropped.
   */
  synchronized void resetCache()
  {
    _fabrics.values().zkClient.each {
      it.destroy()
    }

    _fabrics = [:]

    log.info "Cache cleared"
  }

  private synchronized Map<String, FabricWithZkClient> loadFabrics()
  {
    if(!_fabrics)
    {
      def fabrics = [:]

      fabricStorage.loadFabrics().each { Fabric fabric ->
        try
        {
          FabricWithZkClient fi = new FabricWithZkClient(fabric: fabric)

          fi.zkClient = new ZKClient(fabric.zkConnectString,
                                     fabric.zkSessionTimeout,
                                     null)
          fi.zkClient.start()
          fabrics[fabric.name] = fi
          fi.zkClient.waitForStart(zkClientWaitForStartTimeout)
        }
        catch (Exception e)
        {
          log.warn("Could not connect to fabric ${fabric.name}: [${e.message}]... ignoring")
          if(log.isDebugEnabled())
            log.debug("Could not connect to fabric ${fabric.name}... ignoring", e)
        }
      }

      _fabrics = fabrics

      if(_fabrics)
        log.info "Loaded fabrics: ${_fabrics.values().fabric.name}"
    }

    return _fabrics
  }

  public synchronized void destroy()
  {
    _fabrics.values().zkClient.each {
      it.destroy()
    }
  }
}
