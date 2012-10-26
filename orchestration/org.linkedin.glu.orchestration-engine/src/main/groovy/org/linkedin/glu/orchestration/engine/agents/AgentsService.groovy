/*
 * Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.orchestration.engine.agents

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.tracker.AgentInfo
import org.linkedin.glu.agent.tracker.MountPointInfo
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.fabric.Fabric

/**
 * @author ypujante
 */
interface AgentsService
{
  def getAllInfosWithAccuracy(Fabric fabric)

  Map<String, AgentInfo> getAgentInfos(Fabric fabric)

  AgentInfo getAgentInfo(Fabric fabric, String agentName)

  Map<MountPoint, MountPointInfo> getMountPointInfos(Fabric fabric, String agentName)

  MountPointInfo getMountPointInfo(Fabric fabric, String agentName, mountPoint)

  /**
   * Clears the agent info for the given agent
   * @return <code>true</code> if the agent was cleared, <code>false</code> if it was already cleared
   * @throws IllegalStateException when the agent is still up!
   */
  boolean clearAgentInfo(Fabric fabric, String agentName)

  def getFullState(args)

  def clearError(args)

  def uninstallScript(args)

  def forceUninstallScript(args)

  def interruptAction(args)

  def ps(args)

  def sync(args)

  def kill(args)

  void tailLog(args, Closure closure)

  void streamFileContent(args, Closure closure)

  /**
   * Builds the current system model based on the live data from ZooKeeper
   */
  SystemModel getCurrentSystemModel(Fabric fabric)

  /**
   * Executes the shell command. Note that this call is non blocking.
   *
   * @return whatever <code>org.linkedin.glu.agent.api.Agent#executeShellCommand</code> returns
   * @see org.linkedin.glu.agent.api.Agent#executeShellCommand for details on args
   */
  def executeShellCommand(Fabric fabric,
                          String agentName,
                          args)

  /**
   * Streams the results from the command.
   *
   * @param commandResultProcessor a closure which takes the output of
   *                            <code>org.linkedin.glu.agent.api.Agent#streamCommandResults</code>
   * @return whatever the closure returns
   * @see org.linkedin.glu.agent.api.Agent#streamCommandResults for details on args
   */
  def streamCommandResults(Fabric fabric,
                           String agentName,
                           args,
                           Closure commandResultProcessor)

}
