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

package org.linkedin.glu.agent.api

/**
 * This interface is available from any GLU script using <code>stateManager</code> property
 *
 * Each script is backed by a state machine (<code>org.linkedin.glu.util.state.StateMachine</code>).
 *
 * @author ypujante@linkedin.com */
interface StateManager
{
  /**
   * @return the state of the script (from a state machine point of view) (see info
   * <code>org.linkedin.glu.util.state.StateMachine#getState()</code>)
   */
  def getState()

  /**
   * @return a map with the following definition: <pre>
   * [
   *   scriptDefinition: [ mountPoint: x,
   *                       parent: x,
   *                       scriptFactory: x,
   *                       initParameters: x ], // all values provided when installing the glu script
   *    scriptState: [
   *                   script: [x:x], // all serializable variables in the glu script
   *                   stateMachine: getState(),
   *                   timers: [[timer: x, repeatFrequency: x], ...]
   *                 ]
   * ]
   * </pre>
   */
  def getFullState()

  /**
   * This method is used to change the state and should be used carefully. This method cannot
   * be used when in transition state
   *
   * @param currentState the new current state (can be <code>null</code> in which case we keep
   *                     the current one)
   * @param error the new error state
   */
  void forceChangeState(currentState, error)
}
