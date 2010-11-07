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


package org.linkedin.glu.agent.tracker

import org.linkedin.glu.agent.api.MountPoint

/**
 * Represents an individual mount point on an agent
 *
 * @author ypujante@linkedin.com
 */
class MountPointInfo extends NodeInfo
{
  MountPoint mountPoint
  String agentName

  MountPoint getParent()
  {
    return MountPoint.create(data?.scriptDefinition?.parent)
  }

  def getScriptDefinition()
  {
    return data?.scriptDefinition
  }

  def getCurrentState()
  {
    return data?.scriptState?.stateMachine?.currentState
  }

  def getTransitionState()
  {
    return data?.scriptState?.stateMachine?.transitionState
  }

  def getTransitionAction()
  {
    return data?.scriptState?.stateMachine?.transitionAction
  }

  def getError()
  {
    return data?.scriptState?.stateMachine?.error
  }

  def getErrorStackTrace()
  {
    return data?.scriptState?.stateMachine?.errorStackTrace
  }

  public String toString()
  {
    return "MountPointInfo:${[agentName: agentName, mountPoint: mountPoint, data: data]}".toString()
  }
}
