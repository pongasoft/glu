/*
 * Copyright (c) 2011-2013 Yan Pujante
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

package org.linkedin.glu.orchestration.engine.system

import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.fabric.Fabric

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
   * The source can be a <code>URI</code> or a <code>String</code> or an <code>InputStream</code>
   * @return the model
   */
  SystemModel parseSystemModel(def source)

  /**
   * The source can be a <code>URI</code> or a <code>String</code> or an <code>InputStream</code>
   * @return the model
   */
  SystemModel parseSystemModel(def source, String filename)

  /**
   * Saves the new model as the current system. This method has a side effect in the sense that
   * the id of the provided system will be set (computed) if not set.
   *
   * @return <code>false</code> if the provided system is already the current system,
   * <code>true</code> otherwise
   */
  boolean saveCurrentSystem(SystemModel newSystemModel)

  /**
   * Deletes the current system associated to a fabric
   */
  boolean deleteCurrentSystem(String fabric)

  /**
   * Sets the system provided its id as the current system
   *
   * @return <code>false</code> if the provided system is already the current system,
   * <code>true</code> otherwise
   */
  boolean setAsCurrentSystem(String fabric, String systemId)

  /**
   * @return the number of systems in the fabric
   */
  int getSystemsCount(String fabric)

  /**
   * params can be what grails accept for paginating queries: <code>max</code>,
   * <code>offset</code>, <code>sort</code>, <code>order</code>
   *
   * @return a map with systems: the list of systems ({@link SystemModelDetails}) and
   *         count: the total number of systems
   */
  Map findSystems(String fabric,
                  boolean includeDetails,
                  params)

  /**
   * Find the current system associated to the fabric.
   *
   * @param fabric
   * @return
   */
  SystemModel findCurrentSystem(String fabric)

  /**
   * @return <code>null</code> if not found
   */
  SystemModelDetails findDetailsBySystemId(String systemId)
}