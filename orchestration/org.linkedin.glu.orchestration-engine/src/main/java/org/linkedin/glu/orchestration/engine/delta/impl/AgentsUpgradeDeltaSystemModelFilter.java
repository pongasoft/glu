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

package org.linkedin.glu.orchestration.engine.delta.impl;

import org.linkedin.glu.orchestration.engine.delta.DeltaSystemModelFilter;
import org.linkedin.glu.provisioner.core.model.SystemEntry;
import org.linkedin.glu.provisioner.core.model.SystemFilter;

import java.util.Set;

/**
 * For bounce we take all the entries where the current state is running or stopped
 * and the expected state is 'running'.
 * 
 * @author yan@pongasoft.com
 */
public class AgentsUpgradeDeltaSystemModelFilter implements DeltaSystemModelFilter
{
  private final Set<String> _agents;

  /**
   * Constructor
   */
  public AgentsUpgradeDeltaSystemModelFilter(Set<String> agents)
  {
    _agents = agents;
  }
  /*
    SystemModel currentModel = agentsService.getCurrentSystemModel(params.fabric)
    def agents = (params.agents ?: []) as Set
    def filteredCurrentModel = currentModel.filterBy { SystemEntry entry ->
      agents.contains(entry.agent)
    }

    // we keep only the agents that are part of the current model!
    agents = new HashSet()
    filteredCurrentModel.each { SystemEntry entry ->
      agents << entry.agent
    }

    SystemModel expectedModel = new SystemModel(fabric: currentModel.fabric)
    agents.each { String agent ->
      SystemEntry entry = new SystemEntry(agent: agent,
                                          mountPoint: PlannerService.AGENT_SELF_UPGRADE_MOUNT_POINT,
                                          entryState: 'upgraded')
      entry.script = [scriptClassName: autoUpgradeScriptClassname]
      entry.initParameters = [
        newVersion: params.version,
        agentTar: params.coordinates,
      ]
      expectedModel.addEntry(entry)
    }

    expectedModel = expectedModel.filterBy { SystemEntry entry ->
      entry.mountPoint == PlannerService.AGENT_SELF_UPGRADE_MOUNT_POINT
    }
   */

  @Override
  public boolean filter(SystemEntry expectedEntry, SystemEntry currentEntry)
  {
    if(currentEntry == null)
      return false;

    return true;
  }
}
