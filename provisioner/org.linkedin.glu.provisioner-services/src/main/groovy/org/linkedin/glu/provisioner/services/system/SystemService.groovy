/*
 * Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.provisioner.services.system

import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.services.fabric.Fabric

/**
 * @author yan@pongasoft.com */
public interface SystemService
{
  /**
   * Given a fabric, returns the list of agents that are declared in the system
   * but are not available through ZooKeeper
   */
  Collection<String> getMissingAgents(Fabric fabric)

  /**
   * Given a system, returns the list of agents that are declared
   * but are not available through ZooKeeper
   */
  Collection<String> getMissingAgents(Fabric fabric, SystemModel system)

  /**
   * Saves the new model as the current system. This method has a side effect in the sense that
   * the id of the provided system will be set (computed) if not set.
   *
   * @return <code>false</code> if the provided system is already the current system,
   * <code>true</code> otherwise
   */
  boolean saveCurrentSystem(SystemModel newSystemModel)
}