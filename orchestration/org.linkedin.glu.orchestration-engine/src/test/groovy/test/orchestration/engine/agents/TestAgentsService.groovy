/*
 * Copyright (c) 2011-2014 Yan Pujante
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

import org.apache.zookeeper.data.Stat
import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.tracker.AgentsTracker
import org.linkedin.glu.agent.tracker.MountPointInfo
import org.linkedin.glu.groovy.utils.plugins.PluginServiceImpl
import org.linkedin.glu.orchestration.engine.agents.AgentsServiceImpl
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.agent.tracker.AgentInfo
import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.orchestration.engine.tracker.TrackerService
import org.linkedin.glu.agent.rest.client.AgentFactory
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SettableClock
import org.linkedin.zookeeper.tracker.TrackedNode
import java.security.AccessControlException

/**
 * @author yan@pongasoft.com */
public class TestAgentsService extends GroovyTestCase
{
  PluginServiceImpl pluginService = new PluginServiceImpl()
  AgentsServiceImpl agentsService = new AgentsServiceImpl(pluginService: pluginService)

  Clock clock = new SettableClock()

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

  /**
   * Make sure that if some invalid data makes it into ZooKeeper, it still behaves properly
   */
  public void testInvalidData()
  {

    def allInfos = [:]

    def trackerService = [
      getAllInfosWithAccuracy: { Fabric f -> allInfos[f] }
    ] as TrackerService

    agentsService.trackerService = trackerService

    def fabric = new Fabric(name: 'f1')
    def mp1 = MountPoint.create('/m1')
    def mp2 = MountPoint.create('/m2')
    def agentName = 'agent-1'
    allInfos[fabric] = [
      accuracy: AgentsTracker.AccuracyLevel.ACCURATE,
      allInfos: [
        (agentName): [
          info: new AgentInfo(agentName: agentName,
                              trackedNode: new TrackedNode("/agent", "{}", createStat(), 0)),
          mountPoints: [(mp1):
                          new MountPointInfo(agentName: agentName,
                                             mountPoint: mp1,
                                             trackedNode: new TrackedNode("/m1", "{}", createStat(), 0)),
                        (mp2):
                          new MountPointInfo(agentName: agentName,
                                             mountPoint: mp2,
                                             trackedNode: new TrackedNode("/m2", "garbage", createStat(), 0))
          ]
        ]
      ]
    ]

    def model = agentsService.getCurrentSystemModel(fabric)


    def entry = model.findEntry(agentName, mp2.path)
    assertEquals("agent-1", entry.agent)
    assertEquals("/m2", entry.mountPoint)
    assertEquals("NONE", entry.entryState)
    assertEquals("NONE", entry.metadata.currentState)
    assertEquals("com.fasterxml.jackson.core.JsonParseException", entry.metadata.error[0].name)
    assertEquals(clock.currentTimeMillis(), entry.metadata.modifiedTime)
    assertEquals("NONE", entry.metadata.scriptState.stateMachine.currentState)
    assertEquals("com.fasterxml.jackson.core.JsonParseException", entry.metadata.scriptState.stateMachine.error[0].name)
  }

  protected Stat createStat()
  {
    new Stat(0,
             0,
             clock.currentTimeMillis(),
             clock.currentTimeMillis(),
             0,
             0,
             0,
             0,
             0,
             0,
             0)
  }
}