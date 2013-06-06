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
 * @author yan@pongasoft.com
 */
public interface GluMetaModel extends Externable
{
  Map<String, FabricMetaModel> getFabrics();
  FabricMetaModel findFabric(String fabricName);

  Collection<AgentMetaModel> getAgents();
  AgentMetaModel findAgent(String fabricName, String agentName);

  Map<String, ConsoleMetaModel> getConsoles();
  ConsoleMetaModel findConsole(String consoleName);

  Map<String, ZooKeeperClusterMetaModel> getZooKeeperClusters();
  ZooKeeperClusterMetaModel findZooKeeperCluster(String zooKeeperClusterName);

  /**
   * This is the version of the meta model, NOT the version of glu
   */
  String getMetaModelVersion();
}
