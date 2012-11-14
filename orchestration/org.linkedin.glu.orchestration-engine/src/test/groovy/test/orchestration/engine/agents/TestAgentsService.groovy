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

package test.orchestration.engine.agents

import org.linkedin.glu.groovy.utils.plugins.PluginServiceImpl
import org.linkedin.glu.orchestration.engine.agents.AgentsServiceImpl
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.agent.tracker.AgentInfo
import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.orchestration.engine.tracker.TrackerService
import org.linkedin.glu.agent.rest.client.AgentFactory
import org.linkedin.zookeeper.tracker.TrackedNode
import java.security.AccessControlException

/**
 * @author yan@pongasoft.com */
public class TestAgentsService extends GroovyTestCase
{
  PluginServiceImpl pluginService = new PluginServiceImpl()
  AgentsServiceImpl agentsService = new AgentsServiceImpl(pluginService: pluginService)

  /**
   * Test for plugin on streamFileContent
   */
  public void testStreamFileContentPlugin()
  {
    def agent = [getFileContent: { args -> return args.fileContent} ]
    def trackerService = [getAgentInfo: {Fabric fabric, String agentName ->
      return new AgentInfo(agentName: agentName, trackedNode: new TrackedNode(data: "{}"))}]
    def agentFactory = [withRemoteAgent: {URI agentURI, Closure closure -> closure(agent as Agent)}]

    agentsService.trackerService = trackerService as TrackerService
    agentsService.agentFactory = agentFactory as AgentFactory


    def fileContent = new Object()

    agentsService.streamFileContent([fabric: new Fabric(name: 'f1'),
                                    id: 'a1',
                                    location: '/test',
                                    fileContent: fileContent]) { fc ->
      assertEquals(fileContent, fc)
    }

    // pre plugin
    pluginService.initializePlugin([AgentsService_pre_streamFileContent: { args ->
      if(args.location == '/forbidden')
        throw new AccessControlException("forbidden")
      else
        return null
    }], [:])

    // should still work
    agentsService.streamFileContent([fabric: new Fabric(name: 'f1'),
                                    id: 'a1',
                                    location: '/test',
                                    fileContent: fileContent]) { fc ->
      assertEquals(fileContent, fc)
    }

    // pre plugin will prevent the execution
    def exception = shouldFail(AccessControlException) {
      agentsService.streamFileContent([fabric: new Fabric(name: 'f1'),
                                      id: 'a1',
                                      location: '/forbidden',
                                      fileContent: fileContent]) { fc ->
        assertEquals(fileContent, fc)
      }
    }

    assertEquals("forbidden", exception)

    // post plugin
    pluginService.initializePlugin([AgentsService_post_streamFileContent: { args ->
      if(args.newFileContent)
        return args.newFileContent
      else
        return null
    }], [:])

    // should still work (removed pre plugin)
    agentsService.streamFileContent([fabric: new Fabric(name: 'f1'),
                                    id: 'a1',
                                    location: '/forbidden',
                                    fileContent: fileContent]) { fc ->
      assertEquals(fileContent, fc)
    }

    def newFileContent = new Object()

    // post plugin will replace normal output
    agentsService.streamFileContent([fabric: new Fabric(name: 'f1'),
                                    id: 'a1',
                                    location: '/forbidden',
                                    fileContent: fileContent,
                                    newFileContent: newFileContent]) { fc ->
      assertEquals(newFileContent, fc)
    }
  }
}