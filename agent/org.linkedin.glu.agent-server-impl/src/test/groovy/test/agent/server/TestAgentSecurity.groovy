/*
 * Copyright (c) 2012 Yan Pujante
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

import org.linkedin.glu.agent.api.ShellExecException

/**
 * @author yan@pongasoft.com */
public class TestAgentSecurity extends WithAgentForTest
{
  /**
   * Make sure that when the agent is started with sslEnabled option, then keys are required!
   * glu-175
   */
  public void testClientAuth()
  {
    // when no key is passed => it should fail
    def msg = shouldFail(ShellExecException) {
      agent.rootShell.exec("curl -k https://localhost:${agent.agentPort}/agent")
    }

    assertTrue(msg.contains("alert bad certificate"))

    // when a (correct) key is passed => it should work
    assertEquals('{"mountPoints":["/"]}',
                 agent.rootShell.exec("curl -k https://localhost:${agent.agentPort}/agent -E ${agent.devKeysDir.canonicalPath}/console.pem"))
  }

}