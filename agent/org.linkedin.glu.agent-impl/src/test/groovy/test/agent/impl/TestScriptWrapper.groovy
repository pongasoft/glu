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


package test.agent.impl

import org.linkedin.glu.agent.impl.script.ScriptWrapperImpl
import org.linkedin.glu.agent.api.ScriptExecutionException
import org.linkedin.groovy.util.state.StateMachine

/**
 * Test for ScriptWrapper
 *
 * @author ypujante@linkedin.com
 */
def class TestScriptWrapper extends GroovyTestCase
{
  void testScriptWrapper()
  {
    def shell = [wrap: {args -> return [shell: args]}]

    def stateMachine = [
            getAvailableActions: { ['m1', 'm2'] },
            executeAction: { action, closure ->
              return [stateMachine: closure()]
            }
    ] as StateMachine

    def wrapper = new ScriptWrapperImpl()

    def script = new MyScriptTestScriptWrapper()
    // we check that MyScriptTestScriptWrapper does not have any methods
    assertEquals new HashSet(script.metaClass.methods.name.findAll { ['m1', 'm2', 'm3'].contains(it) }),
                 new HashSet([])

    def scriptClosures =
      ScriptWrapperImpl.getAvailableActionsClosures(stateMachine, 'testScript')

    script = ScriptWrapperImpl.scriptWrapper(script: script,
                                             scriptClosures: scriptClosures,
                                             scriptProperties: [shell: shell, params: [p1: 'v1']])

    assertEquals([stateMachine: [shell: 1]], script.m1(1))
    assertEquals([stateMachine: 'v1'], script.m2(p: 'p1', v: 'v1'))
    assertEquals(12, script.m3(12))

    // we verify that only m1 and m2 have been converted into methods (m3 is not part of the lifecycle methods)
    assertEquals new HashSet(script.metaClass.methods.name.findAll { ['m1', 'm2', 'm3'].contains(it) }),
                 new HashSet(['m1', 'm2'])

    // we make sure that wrapping the script a second time is not wiping out the previous call
    script = ScriptWrapperImpl.scriptWrapper(script: script,
                                             scriptProperties: [extraProps: 'v2'])
    
    assertEquals([stateMachine: [shell: 1]], script.m1(1))
    assertEquals([stateMachine: 'v1'], script.m2(p: 'p1', v: 'v1'))
    assertEquals(12, script.m3(12))

    // now we make the script throw an exception
    def e = new Exception()
    shouldFail(ScriptExecutionException) {
      script.m2(p: 'p1', v: 'v1', e: e)        
    }
  }
}

private class MyScriptTestScriptWrapper
{
  // test shell variable
  def m1 = { args ->
    def res = shell.wrap(args)
    assert res == [shell: args]
    return res
  }

  // test params
  def m2 = { args ->
    assert params[args.p] == args.v
    if(args.e)
      throw args.e
    return params[args.p]
  }

  // test non lifecycle method
  def m3 = { args ->
    assert shell != null
    assert params != null
    return args
  }
}
