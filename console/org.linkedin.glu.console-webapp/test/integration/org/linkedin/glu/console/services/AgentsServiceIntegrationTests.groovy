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

package org.linkedin.glu.console.services

import org.linkedin.glu.orchestration.engine.agents.AgentsServiceImpl
import org.linkedin.glu.agent.api.Agent
import org.linkedin.zookeeper.tracker.TrackedNode
import org.linkedin.glu.agent.tracker.AgentInfo
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.orchestration.engine.tracker.TrackerService
import org.linkedin.glu.agent.rest.client.AgentFactory
import org.linkedin.glu.orchestration.engine.plugins.PluginServiceImpl
import org.linkedin.glu.orchestration.engine.plugins.builtin.StreamFileContentPlugin
import java.security.Permission
import org.linkedin.glu.orchestration.engine.authorization.AuthorizationService
import java.security.AccessControlException
import org.linkedin.groovy.util.io.DataMaskingInputStream

/**
 * @author yan@pongasoft.com */
public class AgentsServiceIntegrationTests extends GroovyTestCase
{
  AgentsServiceImpl _agentsServiceImpl
  PluginServiceImpl pluginService
  def authorizationService

  public void testStreamFileContent()
  {
    def agent = [getFileContent: { args -> return args.fileContent} ]
    def trackerService = [getAgentInfo: {Fabric fabric, String agentName ->
      return new AgentInfo(agentName: agentName, trackedNode: new TrackedNode(data: "{}"))}]
    def agentFactory = [withRemoteAgent: {URI agentURI, Closure closure -> closure(agent as Agent)}]

    _agentsServiceImpl.trackerService = trackerService as TrackerService
    _agentsServiceImpl.agentFactory = agentFactory as AgentFactory

    def plugin = pluginService.findPlugin(StreamFileContentPlugin.class.name)

    assertEquals("/export/content/glu", plugin.unrestrictedLocation)
    assertEquals("ADMIN", plugin.unrestrictedRole)
    assertEquals(authorizationService, plugin.authorizationService)

    String executingPrincipalRole = "ADMIN"

    // now that I know that the proper authorization service is properly wired, I am changing it
    plugin.authorizationService = [
      checkRole : { String role, String message, Permission permission ->
        if(executingPrincipalRole != role)
          throw new AccessControlException(message, permission)
      }
    ] as AuthorizationService

    // outside /export/content/glu, ADMIN => OK
    String content = "c1"
    _agentsServiceImpl.streamFileContent([fabric: new Fabric(name: 'f1'),
                                         id: 'a1',
                                         location: '/test',
                                         fileContent: content]) { fc ->
      assertEquals(content, fc)
    }

    // subdir of /export/content/glu, ADMIN => OK
    content = "c2"
    _agentsServiceImpl.streamFileContent([fabric: new Fabric(name: 'f1'),
                                         id: 'a1',
                                         location: '/export/content/glu/foo',
                                         fileContent: content]) { fc ->
      assertEquals(content, fc)
    }

    // outside /export/content/glu, RELEASE => NOT OK
    executingPrincipalRole = "RELEASE"
    content = "c3"
    def message = shouldFail(AccessControlException) {

      _agentsServiceImpl.streamFileContent([fabric: new Fabric(name: 'f1'),
                                           id: 'a1',
                                           location: '/test',
                                           fileContent: content]) { fc ->
        assertEquals(content, fc)
      }
    }
    assertEquals("/test", message)


    // subdir of /export/content/glu, RELEASE => OK
    content = "c4"
    _agentsServiceImpl.streamFileContent([fabric: new Fabric(name: 'f1'),
                                         id: 'a1',
                                         location: '/export/content/glu/foo',
                                         fileContent: content]) { fc ->
      assertEquals(content, fc)
    }

    content = "c5"

    // when stream it gets decorated by a DataMaskingInputStream
    new ByteArrayInputStream(content.bytes).withStream { stream ->
      _agentsServiceImpl.streamFileContent([fabric: new Fabric(name: 'f1'),
                                           id: 'a1',
                                           location: '/export/content/glu/foo',
                                           fileContent: stream]) { fc ->
        assertTrue(fc instanceof DataMaskingInputStream)
        assertEquals(content, fc.text)
      }
    }
  }
}