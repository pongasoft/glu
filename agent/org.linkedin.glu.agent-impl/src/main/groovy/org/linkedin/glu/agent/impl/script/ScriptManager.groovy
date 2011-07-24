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


package org.linkedin.glu.agent.impl.script

import java.util.concurrent.TimeoutException
import org.slf4j.Logger
import org.linkedin.glu.agent.api.AgentException
import org.linkedin.glu.agent.api.FutureExecution
import org.linkedin.util.lifecycle.Shutdownable

/**
 * Manager for scripts
 *
 * @author ypujante@linkedin.com
 */
def interface ScriptManager extends Shutdownable
{
  /**
   * Install scripts.
   *
   * @see org.linkedin.glu.agent.api.Agent#installScript(Object) for details
   */
  ScriptNode installScript(args)

  /**
   * @return a script previously installed (or <code>null</code> if not found)
   */
  ScriptNode findScript(mountPoint)

  /**
   * Installs the root script
   * 
   * @actionArgs arguments for the install action
   */
  ScriptNode installRootScript(actionArgs)

  /**
   * @return <code>true</code> if there is a script mounted at the given mount point
   */
  boolean isMounted(mountPoint)

  /**
   * @return a list of all mount points currently mounted 
   */
  def getMountPoints()

  /**
   * @return the state of the script (currentState/transitionState/error)
   */
  def getState(mountPoint)

  /**
   * @return the full state of the mountpoint (<code>[scriptDefinition: [...], scriptState: [...]]</code>
   */
  def getFullState(mountPoint)

  /**
   * Clears the error of the script mounted at the provided mount point
   */
  void clearError(mountPoint)

  /**
   * Uninstall the script
   * @param force force uninstall regardless of the state of the script
   */
  void uninstallScript(mountPoint, boolean force)

  /**
   * Executes the action on the software that was installed on the given mount point. Note
   * that contrary to the similar action on the agent, this method is blocking.
   *
   * @param args.mountPoint same mount point provided during {@link #installScript(Object)}
   * @param args.action the lifecycle method you want to execute
   * @param args.actionArgs the arguments to provide the action
   * @return the value returned by the action
   */
  FutureExecution executeAction(args)

  /**
   * @param args.mountPoint same mount point provided during {@link #installScript(Object)}
   * @param args.action the name of the action to interrupt
   * @param args.actionId the id of the action to interrupt (only one of each)
   * @return <code>false</code> if the task could not be cancelled, typically because it has already completed
   * normally; <code>true</code> otherwise
   */
  boolean interruptAction(args)

  /**
   * Waits for the script to be in the state
   *
   * @param args.mountPoint the mount point of the script you want to wait for
   * @param args.state the desired state to wait for
   * @param args.timeout if not <code>null</code>, the amount of time to wait maximum
   * @return <code>true</code> if the state was reached within the timeout, <code>false</code>
   */
  boolean waitForState(args)

  /**
   * Waits for the action (previously scheduled in {@link #executeAction(Object)}) to complete no
   * longer than the timeout provided. Note that due to the nature of the call it is possible
   * that this method throws a <code>NoSuchActionException</code> in some cases:
   * <ul>
   * <li>if the agent restarts between the call to {@link #executeAction(Object)} and this call</li>
   * <li>if this call is made a long time after completion (for obvious memory constraint, the agent
   * cannot keep the result forever...)</li>
   * </ul>
   *
   * @param args.mountPoint the mount point of the script you want to wait for
   * @param args.actionId the id of the action to wait for
   * @param args.timeout if not <code>null</code>, the amount of time to wait maximum
   * @return the value of the execution
   * @throws org.linkedin.glu.agent.api.NoSuchActionException if the action cannot be found
   * @throws TimeoutException if the action cannot be completed in the given amount of time
   */
  def waitForAction(args) throws TimeoutException, AgentException

  /**
   * Executes the call on the software that was installed on the given mount point. Note
   * that this method is a blocking call and will wait for the result of the call.
   *
   * @param args.mountPoint same mount point provided during {@link #installScript(Object)}
   * @param args.call the call you want to execute
   * @param args.callArgs the arguments to provide the call
   * @return whatever value the call returns
   */
  def executeCall(args)

  /**
   * @return the log for the given mountpoint (or <code>null</code> if no such mountpoint)
   */
  Logger findLog(mountPoint)
}
