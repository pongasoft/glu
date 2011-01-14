/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.console.services

import org.linkedin.glu.console.domain.Fabric
import org.springframework.beans.factory.DisposableBean
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.CreateMode
import org.linkedin.glu.console.domain.AuditLog
import org.apache.zookeeper.KeeperException.NoNodeException
import org.linkedin.glu.agent.rest.client.ConfigurableFactory
import org.linkedin.util.lifecycle.Configurable
import org.linkedin.util.clock.Timespan
import org.linkedin.zookeeper.client.ZKClient

/**
 * This service will manage fabrics
 *
 * @author ypujante@linkedin.com */
class FabricService implements DisposableBean
{
  public static final String MODULE = FabricService.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  ConfigurableFactory configurableFactory

  /**
   * In memory cache of fabrics (small list, changes rarely...)
   */
  private volatile Map<String, Fabric> _fabrics = [:]

  def getFabrics()
  {
    return loadFabrics().values()
  }

  def getAgents()
  {
    def agents = [:]

    def envs = fabrics

    envs.each { Fabric fabric ->
      agents.putAll(getAgents(fabric.name))
    }

    return agents
  }

  def getAgents(String fabricName)
  {
    def agents = [:]

    Fabric fabric = findFabric(fabricName)
    if(fabric)
    {
      try
      {
        fabric.zkClient.getChildren('/org/glu/agents/names').each { agent ->
          def path = "/org/glu/agents/names/${agent}/fabric"

          if(fabric.zkClient.exists(path))
          {
            agents[agent] = fabric.zkClient.getStringData(path)
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

  Fabric findFabric(String fabricName)
  {
    return loadFabrics()[fabricName]
  }

  def listFabricNames()
  {
    return loadFabrics().keySet().collect { it }
  }

  def withZkClient(String fabricName, Closure closure)
  {
    closure(findFabric(fabricName).zkClient)
  }

  def setAgentFabric(String agentName, String fabricName)
  {
    AuditLog.audit('fabric.addAgent', "agent: ${agentName}, fabric: ${fabricName}")

    withZkClient(fabricName) { zkClient ->
      zkClient.createOrSetWithParents("/glu/agents/hosts/${agentName}/fabric",
                                      fabricName,
                                      Ids.OPEN_ACL_UNSAFE,
                                      CreateMode.PERSISTENT)
    }

    def zkConnectString = findFabric(fabricName).zkConnectString
    configurableFactory.withRemoteConfigurable(agentName) { Configurable c ->
      c.configure(['org.linkedin.glu.agent.zkConnectString': zkConnectString])
    }

  }

  synchronized def resetCache()
  {
    _fabrics.values().zkClient.each {
      it.destroy()
    }

    AuditLog.audit('fabric.resetCache')

    _fabrics = [:]

    log.info "Cache cleared"
  }

  private synchronized Map<String, Fabric> loadFabrics()
  {
    if(!_fabrics)
    {
      def fabrics = [:]

      Fabric.list().each { Fabric fabric ->
        try
        {
          fabric.zkClient = new ZKClient(fabric.zkConnectString,
                                         Timespan.parse(fabric.zkSessionTimeout),
                                         null)
          fabric.zkClient.start()
          fabrics[fabric.name] = fabric
          fabric.zkClient.waitForStart(Timespan.parse('5s'))
        }
        catch (Exception e)
        {
          log.warn("Could not connect to fabric ${fabric.name}: [${e.message}]... ignoring")
          if(log.isDebugEnabled())
            log.debug("Could not connect to fabric ${fabric.name}... ignoring", e)
        }
      }

      _fabrics = fabrics

      log.info "Loaded fabrics: ${_fabrics.values().name}"
    }

    return _fabrics
  }

  public synchronized void destroy()
  {
    _fabrics.values().each {
      it.destroy()
    }
  }
}
