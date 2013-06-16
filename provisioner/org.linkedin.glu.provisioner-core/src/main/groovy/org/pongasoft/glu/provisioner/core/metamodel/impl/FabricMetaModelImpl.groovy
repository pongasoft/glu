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

import org.pongasoft.glu.provisioner.core.metamodel.AgentMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ConsoleMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.FabricMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.KeysMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperClusterMetaModel

/**
 * @author yan@pongasoft.com  */
public class FabricMetaModelImpl implements FabricMetaModel
{
  String name
  KeysMetaModel keys
  Map<String, AgentMetaModel> agents
  ZooKeeperClusterMetaModel zooKeeperCluster
  ConsoleMetaModel console

  @Override
  AgentMetaModel findAgent(String agentName)
  {
    return agents[agentName]
  }

  @Override
  Object toExternalRepresentation()
  {
    def res = [name: name]

    if(keys)
      res.keys = keys.toExternalRepresentation()

    res.console = console.name
    res.zooKeeperCluster = zooKeeperCluster.name

    return res
  }
}