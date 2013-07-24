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

package org.pongasoft.glu.provisioner.core.metamodel.impl

import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.pongasoft.glu.provisioner.core.metamodel.AgentCliMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.AgentMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ConsoleCliMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ConsoleMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.FabricMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.StateMachineMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperClusterMetaModel

/**
 * @author yan@pongasoft.com  */
public class GluMetaModelImpl implements GluMetaModel
{
  public static final String META_MODEL_VERSION = '1.0.0'

  Map<String, FabricMetaModel> fabrics
  Collection<AgentMetaModel> agents
  Map<String, ConsoleMetaModel> consoles
  Map<String, ZooKeeperClusterMetaModel> zooKeeperClusters
  StateMachineMetaModel stateMachine
  String gluVersion
  String metaModelVersion = META_MODEL_VERSION
  String zooKeeperRoot = DEFAULT_ZOOKEEPER_ROOT
  ConsoleCliMetaModel consoleCli
  AgentCliMetaModel agentCli

  private ConsoleCliMetaModel _defaultConsoleCli = new ConsoleCliMetaModelImpl(gluMetaModel: this)
  private AgentCliMetaModel _defaultAgentCli = new AgentCliMetaModelImpl(gluMetaModel: this)

  @Override
  FabricMetaModel findFabric(String fabricName)
  {
    return fabrics?.get(fabricName)
  }

  @Override
  AgentMetaModel findAgent(String fabricName, String agentName)
  {
    findFabric(fabricName)?.findAgent(agentName)
  }

  @Override
  Collection<AgentMetaModel> getAgents()
  {
    return agents
  }

  @Override
  ConsoleMetaModel findConsole(String consoleName)
  {
    getConsoles()?.values()?.find { it.name == consoleName }
  }

  @Override
  ZooKeeperClusterMetaModel findZooKeeperCluster(String zooKeeperClusterName)
  {
    getZooKeeperClusters()?.values()?.find { it.name == zooKeeperClusterName }
  }

  @Override
  Map<String, ZooKeeperClusterMetaModel> getZooKeeperClusters()
  {
    return zooKeeperClusters
  }

  @Override
  Map<String, ConsoleMetaModel> getConsoles()
  {
    return consoles
  }

  @Override
  ConsoleCliMetaModel getConsoleCli()
  {
    consoleCli ?: _defaultConsoleCli
  }

  @Override
  AgentCliMetaModel getAgentCli()
  {
    agentCli ?: _defaultAgentCli
  }

  @Override
  Object toExternalRepresentation()
  {
    [
      metaModelVersion: getMetaModelVersion(),
      gluVersion: getGluVersion(),
      stateMachine : getStateMachine()?.toExternalRepresentation(),
      fabrics: getFabrics()?.collectEntries { k, v ->
        [k, GluGroovyCollectionUtils.xorMap(v.toExternalRepresentation(), ['name'])] } ?: [:],
      agents: getAgents()?.collect { it.toExternalRepresentation() } ?: [],
      agentCli: getAgentCli()?.toExternalRepresentation(),
      consoles: getConsoles()?.values()?.collect { it.toExternalRepresentation() } ?: [],
      consoleCli: getConsoleCli()?.toExternalRepresentation(),
      zooKeeperClusters: getZooKeeperClusters()?.values()?.collect { it.toExternalRepresentation() } ?: [],
      zooKeeperRoot: getZooKeeperRoot()
    ]
  }
}