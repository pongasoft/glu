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

package org.linkedin.glu.orchestration.engine.system

import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.fabric.Fabric

/**
 * @author yan@pongasoft.com */
public interface SystemStorage
{
  SystemModel findCurrentByFabric(String fabric)

  SystemModelDetails findCurrentDetailsByFabric(String fabric)

  SystemModel findCurrentByFabric(Fabric fabric)

  SystemModel findBySystemId(String systemId)

  SystemModelDetails findDetailsBySystemId(String systemId)

  void saveCurrentSystem(SystemModel systemModel)

  boolean setAsCurrentSystem(String fabric, String systemId)

  /**
   * params can be what grails accept for paginating queries: <code>max</code>,
   * <code>offset</code>, <code>sort</code>, <code>order</code>
   * @return a map with systems: the list of systems and
   *         count: the total number of systems
   */
  Map findSystems(String fabric,
                  boolean includeDetails,
                  params)

  /**
   * @return the number of systems in the fabric
   */
  int getSystemsCount(String fabric)
}