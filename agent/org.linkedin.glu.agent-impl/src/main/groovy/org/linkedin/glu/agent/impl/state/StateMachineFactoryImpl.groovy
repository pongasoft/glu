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


package org.linkedin.glu.agent.impl.state

import org.linkedin.glu.agent.api.Agent
import org.linkedin.groovy.util.state.StateMachineImpl

/**
 * Simple factory which looks at a static field in the script 
 *
 * @author ypujante@linkedin.com
 */
def class StateMachineFactoryImpl
{
  /**
   * Look for a properties called stateMachine on the script to determine the transitions
   * in the state machine
   */
  def static factory = { args ->
    args = new HashMap(args)

    def transitions = null
    if(args.script?.hasProperty('stateMachine'))
    {
      args.transitions = args.script.stateMachine
    }

    // no transitions => use default
    if(!args.transitions)
      args.transitions = Agent.DEFAULT_TRANSITIONS

    return new StateMachineImpl(args)
  }
}
