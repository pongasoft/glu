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

/**
 * Represents a single instance of ZooKeeper
 *
 * @author yan@pongasoft.com
 */
public interface ZooKeeperMetaModel extends ServerMetaModel, Externable
{
  public static final int DEFAULT_CLIENT_PORT = 2181;
  public static final int DEFAULT_QUORUM_PORT = 2888;
  public static final int DEFAULT_LEADER_ELECTION_PORT = 3888;

  /**
   * @return the client port ({@link #getMainPort()})
   */
  int getClientPort();

  /**
   * @return the (quorum) port used when cluster has more than 1 server to talk to each other
   */
  int getQuorumPort();

  /**
   * @return the (leader election) port used when cluster has more than 1 server to talk to
   *         each other
   */
  int getLeaderElectionPort();

  /**
   * @return the index of this instance in the cluster (used for configuration purposes)
   */
  int getServerIdx();

  /**
   * @return a reference to the cluster this instance belongs to
   */
  ZooKeeperClusterMetaModel getZooKeeperCluster();
}