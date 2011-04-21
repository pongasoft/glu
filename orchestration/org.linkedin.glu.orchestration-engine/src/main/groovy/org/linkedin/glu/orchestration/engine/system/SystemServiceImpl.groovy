/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.util.annotations.Initializable

/**
 * @author yan@pongasoft.com */
public class SystemServiceImpl implements SystemService
{
  @Initializable(required = true)
  AgentsService agentsService

  @Initializable(required = true)
  SystemStorage systemStorage

  /**
   * Given a fabric, returns the list of agents that are declared in the system
   * but are not available through ZooKeeper
   */
  Collection<String> getMissingAgents(Fabric fabric)
  {
    getMissingAgents(fabric, systemStorage.findCurrentByFabric(fabric))
  }

  /**
   * Given a system, returns the list of agents that are declared
   * but are not available through ZooKeeper
   */
  Collection<String> getMissingAgents(Fabric fabric, SystemModel system)
  {
    if(system && system.fabric != fabric.name)
      throw new IllegalArgumentException("fabric mismatch: ${system.fabric} != ${fabric.name}")

    def availableAgents = agentsService.getAgentInfos(fabric)

    def missingAgents = [] as Set

    system?.each { SystemEntry se ->
      if(!availableAgents.containsKey(se.agent))
        missingAgents << se.agent
    }

    return missingAgents
  }

  /**
   * Saves the new model as the current system. This method has a side effect in the sense that
   * the id of the provided system will be set (computed) if not set.
   *
   * @return <code>false</code> if the provided system is already the current system,
   * <code>true</code> otherwise
   */
  synchronized boolean saveCurrentSystem(SystemModel newSystemModel)
  {
    if(newSystemModel.filters)
      throw new IllegalStateException("cannot save a filtered model!")

    String newSystemModelSha1 = newSystemModel.computeContentSha1()

    if(!newSystemModel.id)
      newSystemModel.id = newSystemModelSha1

    SystemModel currentSystemModel = systemStorage.findCurrentByFabric(newSystemModel.fabric)

    // when the new system and the current system match, there is no reason to
    // change it
    if(newSystemModelSha1 == currentSystemModel?.computeContentSha1())
    {
      return false
    }

    SystemModel previousSystemModel = systemStorage.findBySystemId(newSystemModel.id)

    if(previousSystemModel)
    {
      if(newSystemModelSha1 != previousSystemModel?.computeContentSha1())
      {
        throw new IllegalArgumentException("same id ${newSystemModel.id} but different content!")
      }
    }

    systemStorage.saveCurrentSystem(newSystemModel)

    return true;
  }
}