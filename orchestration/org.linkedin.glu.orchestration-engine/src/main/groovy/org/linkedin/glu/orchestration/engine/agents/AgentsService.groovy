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
import org.linkedin.glu.agent.api.TimeOutException

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
   * Wait (no longer than the timeout provided) for the command to complete and return the exit
   * value
   *
   * @param args.id the id of the command (as returned by {@lin #executeShellCommand})
   * @param args.timeout if not <code>null</code>, the amount of time to wait maximum. If
   *                     <code>null</code>, wait until the command completes.
   * @return the exit value
   */
  def waitForCommand(Fabric fabric,
                     String agentName,
                     args) throws TimeOutException

  /**
   * Wait (no longer than the timeout provided) for the command to complete and return the exit
   * value
   *
   * @param args.id the id of the command (as returned by {@lin #executeShellCommand})
   * @param args.timeout if not <code>null</code>, the amount of time to wait maximum. If
   *                     <code>null</code>, wait until the command completes.
   * @return <code>true</code> if the command completed within the timeout or <code>false</code>
   *         otherwise
   */
  boolean waitForCommandNoTimeOutException(Fabric fabric,
                                           String agentName,
                                           args) throws TimeOutException

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

  /**
   * Interrupts the command.
   *
   * @param args.id the id of the command to interrupt
   * @return <code>true</code> if the command was interrupted properly or <code>false</code> if
   * there was no such command or already completed
   */
  boolean interruptCommand(Fabric fabric,
                           String agentName,
                           args)
}
