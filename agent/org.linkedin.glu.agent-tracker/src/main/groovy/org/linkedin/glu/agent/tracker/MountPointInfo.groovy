/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2014 Yan Pujante
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
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.groovy.util.state.StateMachine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Represents an individual mount point on an agent
 *
 * @author ypujante@linkedin.com
 */
class MountPointInfo extends NodeInfo
{
  public static final String MODULE = MountPointInfo.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

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

  def getInitParameters()
  {
    return scriptDefinition?.initParameters
  }

  def getMetadata()
  {
    return initParameters?.metadata
  }

  def getTags()
  {
    return initParameters?.tags
  }

  boolean isCommand()
  {
    mountPoint.path.startsWith('/_/command/')
  }

  MountPointInfo invalidate(Throwable error)
  {
    new MountPointInfo(trackedNode: trackedNode,
                       mountPoint: mountPoint,
                       agentName: agentName,
                       _data: handleInvalidData(error))
  }

  @Override
  protected Map handleInvalidData(Throwable error)
  {
    log.warn("Invalid state detected: key=${agentName}:${mountPoint}; ex=${GluGroovyLangUtils.toString(error)}")
    if(log.isDebugEnabled())
      log.debug("Invalid state detected: key=${agentName}:${mountPoint}", error)

    def res = [:]

    res.scriptState = [
      stateMachine: [
        currentState: StateMachine.NONE,
        error: GluGroovyLangUtils.extractExceptionDetailsWithCause(error)
      ]
    ]

    return res
  }

  @Override
  protected Map validateAndAdjust(Map data)
  {
    try
    {
      // try to access the entries
      def sd = data?.scriptDefinition
      sd?.parent

      def sm = data?.scriptState?.stateMachine
      sm?.currentState
      sm?.transitionState
      sm?.transitionAction
      sm?.error
      sm?.errorStackTrace

      def ip = sd?.initParameters
      ip?.metadata
      ip?.tags

      return data
    }
    catch(Throwable error)
    {
      handleInvalidData(error)
    }
  }

  public String toString()
  {
    return "MountPointInfo:${[agentName: agentName, mountPoint: mountPoint, data: data]}".toString()
  }
}
