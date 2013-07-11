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

import java.util.List;
import java.util.Map;

/**
 * Represents a cluster of ZooKeeper instances.
 * @author yan@pongasoft.com
 */
public interface ZooKeeperClusterMetaModel extends Externable, Configurable, MetaModel
{
  /**
   * @return the name of the cluster
   */
  String getName();

  /**
   * @return all the fabrics that are installed in this cluster
   */
  Map<String, FabricMetaModel> getFabrics();

  /**
   * @return the fabric given its name or <code>null</code>
   */
  FabricMetaModel findFabric(String fabricName);

  /**
   * @return all the ZooKeeper server instances making up this cluster
   */
  List<ZooKeeperMetaModel> getZooKeepers();

  /**
   * @return the connection string in order to connect to this cluster
   */
  String getZooKeeperConnectionString();

  /**
   * @return reference to the glu meta model this cluster belongs to
   */
  GluMetaModel getGluMetaModel();
}