/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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


package test.agent.impl

import org.linkedin.glu.agent.impl.state.StateMachineFactoryImpl
import org.linkedin.glu.agent.api.Agent

/**
 *
 *
 * @author ypujante@linkedin.com
 */
def class TestStateMachineFactory extends GroovyTestCase
{
  void testDefaultTransition()
  {
    def stateMachine = StateMachineFactoryImpl.factory(script: new MyScriptTestStateMachineFactory1())
    assertEquals(Agent.DEFAULT_TRANSITIONS, stateMachine.transitions)
  }

  void testCustomTransition()
  {
    def stateMachine = StateMachineFactoryImpl.factory(script: new MyScriptTestStateMachineFactory2())
    assertEquals(MyScriptTestStateMachineFactory2.stateMachine, stateMachine.transitions)
  }
}

private class MyScriptTestStateMachineFactory1
{
}

private class MyScriptTestStateMachineFactory2
{
  def static stateMachine =
  [
          NONE: [[to: 'installed', action: 'install']],
          installed: [[to: 'NONE', action: 'uninstall']]

  ]
}