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

import org.linkedin.util.clock.Timespan
import org.pongasoft.glu.provisioner.core.metamodel.FabricMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperClusterMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperMetaModel

/**
 * @author yan@pongasoft.com  */
public class ZooKeeperClusterMetaModelImpl implements ZooKeeperClusterMetaModel
{
  String name
  Map<String, FabricMetaModel> fabrics
  List<ZooKeeperMetaModel> zooKeepers
  Map<String, Object> configTokens
  GluMetaModel gluMetaModel
  Timespan zooKeeperSessionTimeout

  @Override
  FabricMetaModel findFabric(String fabricName)
  {
    fabrics[fabricName]
  }

  @Override
  String getZooKeeperConnectionString()
  {
    zooKeepers.collect { ZooKeeperMetaModel zk -> "${zk.host.resolveHostAddress()}:${zk.clientPort}" }.join(',')
  }

  @Override
  Object toExternalRepresentation()
  {
    [
      name: getName(),
      zooKeepers: getZooKeepers()?.collect { it.toExternalRepresentation() } ?: [],
      zooKeeperSessionTimeout: getZooKeeperSessionTimeout()?.toString(),
      configTokens: getConfigTokens() ?: [:]
    ]
  }
}