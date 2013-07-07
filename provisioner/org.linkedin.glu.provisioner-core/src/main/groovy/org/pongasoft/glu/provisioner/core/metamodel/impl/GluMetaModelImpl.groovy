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
import org.pongasoft.glu.provisioner.core.metamodel.AgentMetaModel
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
  StateMachineMetaModel stateMachine
  String gluVersion
  String metaModelVersion = META_MODEL_VERSION
  String zooKeeperRoot = DEFAULT_ZOOKEEPER_ROOT

  @Override
  FabricMetaModel findFabric(String fabricName)
  {
    return fabrics[fabricName]
  }

  @Override
  AgentMetaModel findAgent(String fabricName, String agentName)
  {
    findFabric(fabricName)?.findAgent(agentName)
  }

  @Override
  Collection<AgentMetaModel> getAgents()
  {
    def agents = []

    fabrics.values().each { FabricMetaModel model ->
      agents.addAll(model.agents.values())
    }

    return agents
  }

  @Override
  ConsoleMetaModel findConsole(String consoleName)
  {
    fabrics.values().find { it.console.name == consoleName }?.console
  }

  @Override
  ZooKeeperClusterMetaModel findZooKeeperCluster(String zooKeeperClusterName)
  {
    fabrics.values().find { it.zooKeeperCluster.name == zooKeeperClusterName }?.zooKeeperCluster
  }

  @Override
  Map<String, ZooKeeperClusterMetaModel> getZooKeeperClusters()
  {
    Map<String, ZooKeeperClusterMetaModel> res = [:]

    fabrics.values().each { FabricMetaModel model ->
      res[model.zooKeeperCluster.name] = model.zooKeeperCluster
    }

    return res
  }

  @Override
  Map<String, ConsoleMetaModel> getConsoles()
  {
    Map<String, ConsoleMetaModel> res = [:]

    fabrics.values().each { FabricMetaModel model ->
      res[model.console.name] = model.console
    }

    return res
  }

  @Override
  Object toExternalRepresentation()
  {
    def res =[
      metaModelVersion: metaModelVersion,
      gluVersion: gluVersion
    ]

    if(stateMachine)
      res.stateMachine = stateMachine.toExternalRepresentation()

    if(fabrics)
      res.fabrics = fabrics.collectEntries { k, v ->
        [k, GluGroovyCollectionUtils.xorMap(v.toExternalRepresentation(), ['name'])] }

    if(agents)
      res.agents = agents.collect { it.toExternalRepresentation() }

    if(consoles)
      res.consoles = consoles.values().collect { it.toExternalRepresentation() }

    if(zooKeeperClusters)
      res.zooKeeperClusters = zooKeeperClusters.values().collect { it.toExternalRepresentation() }

    if(zooKeeperRoot != DEFAULT_ZOOKEEPER_ROOT)
      res.zooKeeperRoot = zooKeeperRoot

    return res
  }
}