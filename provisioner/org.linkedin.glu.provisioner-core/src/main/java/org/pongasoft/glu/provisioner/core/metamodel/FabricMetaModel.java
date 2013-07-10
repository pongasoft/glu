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

import java.util.Map;

/**
 * Represents a fabric in glu.
 *
 * @author yan@pongasoft.com
 */
public interface FabricMetaModel extends Externable
{
  /**
   * @return the name of the fabric
   */
  String getName();

  /**
   * In a given fabric, agent names must be unique.
   *
   * @return all the agents that belong to a fabric. key is agent name
   */
  Map<String, AgentMetaModel> getAgents();

  AgentMetaModel findAgent(String agentName);

  /**
   * @return the ZooKeeper cluster where this fabric is hosted
   */
  ZooKeeperClusterMetaModel getZooKeeperCluster();

  /**
   * @return the console that manages this fabric
   */
  ConsoleMetaModel getConsole();

  /**
   * @return the keys associated to this fabric (communication to agents)
   * @see ConsoleMetaModel for details and restrictions
   */
  KeysMetaModel getKeys();

  /**
   * @return reference to the glu meta model this fabric belongs to
   */
  GluMetaModel getGluMetaModel();
}