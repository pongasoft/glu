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
 * Represents a glu agent (that gets installed on each node where you need to deploy/undeploy
 * applications)
 *
 * @author yan@pongasoft.com
 */
public interface AgentMetaModel extends ServerMetaModel, Externable
{
  public static final int DEFAULT_PORT = 12906;
  public static final int DEFAULT_CONFIG_PORT = DEFAULT_PORT + 1;

  /**
   * @return the name as specified in the config (may be <code>null</code>)
   */
  String getName();

  /**
   * @return the name of the agent: either {@link #getName()} or resolved host
   * (never <code>null</code>)
   */
  String getResolvedName();

  /**
   * @return which port the agent is listening on. Alias to {@link #getMainPort()}.
   */
  int getAgentPort();

  /**
   * @return which port the agent is listening on for configuration. Not a typical use case.
   */
  int getConfigPort();

  /**
   * An agent belongs to one and only one fabric.
   *
   * @return the unique fabric this agent belongs to
   */
  FabricMetaModel getFabric();

  /**
   * @return the agent upgrade
   */
  AgentUpgradeMetaModel getAgentUpgrade();
}