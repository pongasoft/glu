/*
 * Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.orchestration.engine.fabric

/**
 * This service will manage fabrics
 *
 * @author ypujante@linkedin.com */
interface FabricService
{
  /**
   * @return a collection of all the fabrics known by the service (cached list)
   */
  Collection<Fabric> getFabrics()

  /**
   * @return a map where the key is the agent name and the value is the fabric name the agent
   * belongs (as defined in ZooKeeper)
   */
  Map<String, String> getAgents()

  /**
   * @return the fabric or <code>null</code>
   */
  Fabric findFabric(String fabricName)

  /**
   * @return the list of fabric names
   */
  Collection<String> listFabricNames()

  /**
   * Sets the fabric for the given agent: write it in ZooKeeper and configure the agent as well.
   */
  void setAgentFabric(String agentName, String fabricName)

  /**
   * resets the fabrics cache which will force the connection to ZooKeeper to be dropped.
   */
  void resetCache()

  /**
   * @return <code>true</code> if the fabric is connected
   */
  boolean isConnected(String fabricName)

  /**
   * Executes the closure with the ZooKeeper connection (instance of
   * {@link org.linkedin.zookeeper.client.IZKClient}) associated to the fabric
   *
   * @return whatever the closure returns
   */
  def withZkClient(String fabricName, Closure closure)
}
