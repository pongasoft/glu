/*
 * Copyright (c) 2013 Yan Pujante
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

package test.agent.server

import org.linkedin.glu.agent.api.MountPoint

/**
 * @author yan@pongasoft.com  */
public class TestScriptCapabilities extends WithAgentForTest
{
  public void testAgentZooKeeper()
  {
    def scriptMountPoint = MountPoint.fromPath('/s')

    agent.installScript(mountPoint: scriptMountPoint,
                        initParameters: [p1: 'v1'],
                        scriptClassName: MyScriptTestAgentImpl.class.name)

    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'install')

    assertTrue agent.waitForState(mountPoint: scriptMountPoint, state: 'installed')
  }
}

public class MyScriptTestAgentImpl
{
  def install = { args ->
    GroovyTestCase.assertEquals("127.0.0.1:2121", agentZooKeeper.connectString)
    GroovyTestCase.assertEquals("test-fabric", agentZooKeeper.getStringData('/org/glu/agents/names/agent-1/fabric'))
  }
}
