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


package org.linkedin.glu.agent.impl.capabilities

import org.linkedin.glu.agent.impl.script.MOP
import org.linkedin.glu.agent.impl.script.ScriptWrapperImpl
import org.linkedin.glu.agent.impl.state.StateMachineFactoryImpl

/**
 * @author ypujante@linkedin.com
 */
def class MOPImpl implements MOP
{
  private def _scriptWrapper
  private def _stateMachineFactory

  def MOPImpl()
  {
    this(null)
  }

  def MOPImpl(args)
  {
    _scriptWrapper = args?.scriptWrapper ?: ScriptWrapperImpl.scriptWrapper
    _stateMachineFactory = args?.stateMachineFactory ?: StateMachineFactoryImpl.factory
  }

  public wrapScript(args)
  {
    return _scriptWrapper(args)
  }

  public createStateMachine(args)
  {
    return _stateMachineFactory(args);
  }
}
