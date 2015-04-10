/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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
import org.linkedin.glu.provisioner.core.model.builder.SystemModelBuilder
import org.linkedin.groovy.util.net.GroovyNetUtils
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.groovy.utils.plugins.PluginService
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.glu.provisioner.core.model.JSONSystemModelSerializer
import org.linkedin.glu.provisioner.core.model.SystemModelRenderer

/**
 * @author yan@pongasoft.com */
public class SystemServiceImpl implements SystemService
{
  @Initializable(required = true)
  AgentsService agentsService

  @Initializable(required = true)
  SystemStorage systemStorage

  @Initializable(required = true)
  PluginService pluginService

  @Initializable(required = true)
  SystemModelRenderer systemModelRenderer

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

  @Override
  SystemModel parseSystemModel(source)
  {
    parseSystemModel(source, null)
  }

  @Override
  SystemModel parseSystemModel(source, String filename)
  {
    if(!source)
      return null

    pluginService.executePrePostMethods(SystemService,
                                        "parseSystemModel",
                                        [source: source, filename: filename]) { args ->
      SystemModel model = args.pluginResult

      if(model == null)
      {
        def builder = new SystemModelBuilder()

        switch(args.source)
        {
          case String:
            builder.deserializeFromJsonString(args.source, args.filename)
            break

          case InputStream:
            builder.deserializeFromJsonInputStream(args.source, args.filename)
            break

          case URI:
            builder.deserializeFromJsonString(GroovyIOUtils.cat(args.source),
                                              args.filename ?: GroovyNetUtils.guessFilename(args.source))
            break

          default:
            builder.deserializeFromJsonString(GroovyIOUtils.cat(args.source),
                                              args.filename)
            break
        }

        model = builder.toModel()
      }

      return model
    } as SystemModel
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

    String newSystemModelSha1 = systemModelRenderer.computeSystemId(newSystemModel)

    if(!newSystemModel.id)
      newSystemModel.id = newSystemModelSha1

    SystemModel currentSystemModel = systemStorage.findCurrentByFabric(newSystemModel.fabric)

    // when the new system and the current system match, there is no reason to
    // change it
    if(newSystemModelSha1 == systemModelRenderer.computeSystemId(currentSystemModel))
    {
      return false
    }

    SystemModel previousSystemModel = systemStorage.findBySystemId(newSystemModel.id)

    if(previousSystemModel)
    {
      if(newSystemModelSha1 != systemModelRenderer.computeSystemId(previousSystemModel))
      {
        throw new IllegalArgumentException("same id ${newSystemModel.id} but different content!")
      }
    }

    systemStorage.saveCurrentSystem(newSystemModel)

    return true;
  }

  @Override
  synchronized boolean deleteCurrentSystem(String fabric)
  {
    systemStorage.deleteCurrentSystem(fabric)
  }

  @Override
  synchronized boolean setAsCurrentSystem(String fabric, String systemId)
  {
    SystemModel previousSystemModel = systemStorage.findBySystemId(systemId)

    if(previousSystemModel == null)
      throw new IllegalArgumentException("no such system: ${systemId}")

    if(previousSystemModel.fabric != fabric)
      throw new IllegalArgumentException("system [${systemId}] does not belong to fabric ${fabric}")

    systemStorage.setAsCurrentSystem(fabric, systemId)
  }

  @Override
  SystemModel findCurrentSystem(String fabric)
  {
    systemStorage.findCurrentByFabric(fabric)
  }

  @Override
  SystemModelDetails findDetailsBySystemId(String systemId)
  {
    systemStorage.findDetailsBySystemId(systemId)
  }

  @Override
  int getSystemsCount(String fabric)
  {
    systemStorage.getSystemsCount(fabric)
  }


  @Override
  Map findSystems(String fabric, boolean includeDetails, params)
  {
    systemStorage.findSystems(fabric, includeDetails, params)
  }
}