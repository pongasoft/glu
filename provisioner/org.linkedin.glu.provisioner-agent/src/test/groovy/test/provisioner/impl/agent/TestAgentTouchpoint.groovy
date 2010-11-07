/*
 * Copyright 2010-2010 LinkedIn, Inc
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

package test.provisioner.impl.agent

import org.linkedin.glu.provisioner.core.environment.Installation
import org.linkedin.glu.provisioner.core.touchpoint.Touchpoint
import test.provisioner.impl.mocks.AgentMock
import test.provisioner.impl.mocks.AgentFactoryMock
import org.linkedin.glu.provisioner.core.action.IActionDescriptorFactory
import test.provisioner.impl.mocks.MockKeyProvider
import org.linkedin.glu.provisioner.impl.agent.AgentActionDescriptorFactory
import org.linkedin.glu.provisioner.impl.agent.AgentTouchpoint

/**
 * Test the actions created by the agent touchpoint
 *
 * author:  Riccardo Ferretti
 * created: Aug 5, 2009
 */
public class TestAgentTouchpoint extends GroovyTestCase
{

  /**
   * Test the actions created when installing an installation
   */
  void testActionFactory_install()
  {
    IActionDescriptorFactory factory = new AgentActionDescriptorFactory()
    AgentMock mock = new AgentMock()
    Touchpoint tp = new AgentTouchpoint(new AgentFactoryMock(agent: mock), new MockKeyProvider())
    Installation inst = new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/org.linkedin.media/media-backend/3.4',
                                          props: ['myprop':'myvalue'], parent: null)
    tp.getAction(factory.getActionDescriptor('install', inst)).execute()

    def expectedActions = [
            'clearError [mountPoint:/media]',
            'executeAction [mountPoint:/media, action:install, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2], myprop:myvalue]]',
            'waitForState [mountPoint:/media, state:installed, timeout:10s]']

    checkActions(expectedActions, mock.log)
  }

  /**
   * Test the actions created when starting an installation
   */
  void testActionFactory_start()
  {
    IActionDescriptorFactory factory = new AgentActionDescriptorFactory()
    AgentMock mock = new AgentMock()
    Touchpoint tp = new AgentTouchpoint(new AgentFactoryMock(agent: mock), new MockKeyProvider())
    Installation inst = new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/org.linkedin.media/media-backend/3.4',
                                          props: ['myprop':'myvalue'], parent: null)
    tp.getAction(factory.getActionDescriptor('start', inst)).execute()

    def expectedActions = [
            'clearError [mountPoint:/media]',
            'executeAction [mountPoint:/media, action:start, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2], myprop:myvalue]]',
            'waitForState [mountPoint:/media, state:running, timeout:10s]']

    checkActions(expectedActions, mock.log)
  }


  /**
   * Test the actions created when configuring an installation
   */
  void testActionFactory_configure()
  {
    IActionDescriptorFactory factory = new AgentActionDescriptorFactory()
    AgentMock mock = new AgentMock()
    Touchpoint tp = new AgentTouchpoint(new AgentFactoryMock(agent: mock), new MockKeyProvider())
    Installation inst = new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/org.linkedin.media/media-backend/3.4',
                                          props: ['myprop':'myvalue'], parent: null)
    tp.getAction(factory.getActionDescriptor('configure', inst)).execute()

    def expectedActions = [
            'clearError [mountPoint:/media]',
            'executeAction [mountPoint:/media, action:configure, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2], myprop:myvalue]]',
            'waitForState [mountPoint:/media, state:stopped, timeout:10s]']

    checkActions(expectedActions, mock.log)
  }

  /**
   * Test the actions created when stopping an installation
   */
  void testActionFactory_stop()
  {
    IActionDescriptorFactory factory = new AgentActionDescriptorFactory()
    AgentMock mock = new AgentMock()
    Touchpoint tp = new AgentTouchpoint(new AgentFactoryMock(agent: mock), new MockKeyProvider())
    Installation inst = new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/org.linkedin.media/media-backend/3.4',
                                          props: ['myprop':'myvalue'], parent: null)
    tp.getAction(factory.getActionDescriptor('stop', inst)).execute()

    def expectedActions = [
            'clearError [mountPoint:/media]',
            'executeAction [mountPoint:/media, action:stop, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2], myprop:myvalue]]',
            'waitForState [mountPoint:/media, state:stopped, timeout:10s]']

    checkActions(expectedActions, mock.log)
  }

  /**
   * Test the actions created when uninstalling an installation
   */
  void testActionFactory_uninstall()
  {
    IActionDescriptorFactory factory = new AgentActionDescriptorFactory()
    AgentMock mock = new AgentMock()
    Touchpoint tp = new AgentTouchpoint(new AgentFactoryMock(agent: mock), new MockKeyProvider())
    Installation inst = new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/org.linkedin.media/media-backend/3.4',
                                          props: ['myprop':'myvalue'], parent: null)
    tp.getAction(factory.getActionDescriptor('uninstall', inst)).execute()

    def expectedActions = [
            'clearError [mountPoint:/media]',
            'executeAction [mountPoint:/media, action:uninstall, actionArgs:[encryptionKeys:[s1:secretKey1, s2:secretKey2], myprop:myvalue]]',
            'waitForState [mountPoint:/media, state:NONE, timeout:10s]']

    checkActions(expectedActions, mock.log)
  }

  
  private void checkActions(List expectedActions, List actualActions)
  {
    assertEquals("Different number of actions!", expectedActions.size(), actualActions.size())
    expectedActions.eachWithIndex {el, idx ->
      assertEquals(el, actualActions[idx])
    }
  }
}