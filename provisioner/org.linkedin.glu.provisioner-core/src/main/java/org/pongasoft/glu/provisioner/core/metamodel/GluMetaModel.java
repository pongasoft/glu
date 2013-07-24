/*
 * Copyright (c) 2013 Yan Pujante
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

package org.pongasoft.glu.provisioner.core.metamodel;

import org.linkedin.glu.utils.core.Externable;

import java.util.Collection;
import java.util.Map;

/**
 * The root of the model.
 *
 * @author yan@pongasoft.com
 */
public interface GluMetaModel extends Externable, MetaModel
{
  public static final String DEFAULT_ZOOKEEPER_ROOT = "/org/glu";

  /**
   * @return all fabrics in the glu model (name is unique)
   */
  Map<String, FabricMetaModel> getFabrics();
  FabricMetaModel findFabric(String fabricName);

  /**
   * Note that since agent names are only unique per fabric, there may be agents
   * with the same name. {@link #findAgent(String, String)}.
   *
   * @return all agents managed by glu
   */
  Collection<AgentMetaModel> getAgents();

  /**
   * @return the agent given the fabric name and agent name or <code>null</code> if no such agent
   */
  AgentMetaModel findAgent(String fabricName, String agentName);

  /**
   * @return the agent cli (to talk to agents directly using their REST api)
   */
  AgentCliMetaModel getAgentCli();

  /**
   * @return all the consoles defined in this model
   */
  Map<String, ConsoleMetaModel> getConsoles();

  /**
   * @return the given console given its name or <code>null</code>
   */
  ConsoleMetaModel findConsole(String consoleName);

  /**
   * @return the console cli (to talk to console using their REST api)
   */
  ConsoleCliMetaModel getConsoleCli();

  /**
   * @return all ZooKeeper clusters defined in this model
   */
  Map<String, ZooKeeperClusterMetaModel> getZooKeeperClusters();

  /**
   * @return the ZooKeeper cluster given its name or <code>null</code>
   */
  ZooKeeperClusterMetaModel findZooKeeperCluster(String zooKeeperClusterName);

  /**
   * @return the ZooKeeper root to use which by default is set to {@link #DEFAULT_ZOOKEEPER_ROOT}.
   */
  String getZooKeeperRoot();

  /**
   * If you need to redefine the standard state machine (not a typical use case!)
   */
  StateMachineMetaModel getStateMachine();

  /**
   * This is the version of glu
   */
  String getGluVersion();

  /**
   * This is the version of the meta model, NOT the version of glu
   */
  String getMetaModelVersion();
}
