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

package org.linkedin.glu.agent.api

/**
 * @author ypujante@linkedin.com
 *
 */
public interface Agent
{
  /**
   * Default transitions for the state machine in the script (if the script does not provide its own)
   */
  def static DEFAULT_TRANSITIONS =
  [
    NONE: [[to: 'installed', action: 'install']],
    installed: [[to: 'stopped', action: 'configure'], [to: 'NONE', action: 'uninstall']],
    stopped: [[to: 'running', action: 'start'], [to: 'installed', action: 'unconfigure']],
    running: [[to: 'stopped', action: 'stop']]
  ]

  /********************************************************************
   * Software management
   ********************************************************************/
  /**
   * Installs a script.
   *
   * @param args.mountPoint: where to 'mount' the script (ex '/container/i001')
   *     (<code>String</code> or <code>MountPoint</code>)
   * @param args.parent: the parent of this script (optional: will attach to the root mount point)
   * @param args.initParameters: a map of init parameters (ex: [skeleton: 'ivy:/s/k/1.0.0'])
   * @param args.scriptContent: content of the script (byte[]) or
   * @param args.scriptLocation: a uri (String is ok) pointing to the script (ex: 'ivy:/a/b/1.0.0',
   *     'file://path/to/script') or
   * @param args.scriptFactory: a script factory (for testing only...)
   */
  void installScript(args) throws AgentException

  /**
   * Removed a previously installed script. Note that the lifecyle must be ended first otherwise
   * an exception will be thrown.
   *  
   * @param args.mountPoint same mount point provided during {@link #installScript(Object)}
   * @param args.force set to <code>true</code> if you want to force uninstalling the script
   * regardless of its state
   */
  void uninstallScript(args) throws AgentException

  /**
   * Executes the action on the software that was installed on the given mount point. Note
   * that this method returns immediately and does not wait for the action to complete.
   *
   * @param args.mountPoint same mount point provided during {@link #installScript(Object)}
   * @param args.action the lifecycle method you want to execute
   * @param args.actionArgs the arguments to provide the action
   * @return a unique id referring to the action to be used in {@link #waitForAction(Object)}
   */
  String executeAction(args) throws AgentException

  /**
   * Interrupts the action.
   *
   * @param args.mountPoint same mount point provided during {@link #installScript(Object)}
   * @param args.action the lifecycle method you want to interrupt on (for double checking)
   *        (optional: if not provided will cancel any action currently running on the mountpoint)
   * @return <code>true</code> if the action was interrupted properly or <code>false</code> if
   * there was no such action or already completed
   */
  boolean interruptAction(args) throws AgentException

  /**
   * Clears the error attached to the mountpoint.
   *
   * @param args.mountPoint same mount point provided during {@link #installScript(Object)}
   * @throws NoSuchMountPointException when no mount point
   */
  void clearError(args) throws AgentException

  /**
   * Executes the call on the software that was installed on the given mount point. Note
   * that this method is a blocking call and will wait for the result of the call.
   *
   * @param args.mountPoint same mount point provided during {@link #installScript(Object)}
   * @param args.call the call you want to execute
   * @param args.callArgs the arguments to provide the call
   * @return whatever value the call returns
   */
  def executeCall(args) throws AgentException

  /**
   * @param args.mountPoint the mount point of the script you want to get the state
   * @return the state of the script (currentState/transitionState/error)
   * @throws NoSuchMountPointException when no mount point
   * @see StateManager#getState()
   */
  def getState(args) throws AgentException
  
  /**
   * @param args.mountPoint the mount point of the script you want to get the (full) state
   * @return the full state of the mountpoint
   * (<code>[scriptDefinition: [...], scriptState: [...]]</code>.
   *
   * Ex:
   * [scriptDefinition:[initParameters:[:], parent:/, mountPoint:/container/i001,
   *                    scriptFactory:[location:file:/tmp/TomcatServerWithWarsScript.groovy, 
   *                                   class:org.linkedin.glu.agent.impl.script.FromLocationScriptFactory]],
   *  scriptState:[stateMachine:[currentState:installed], script:[:]]]
   *
   * @see StateManager#getFullState() 
   */
  def getFullState(args) throws AgentException

  /**
   * Waits for the script to be in the state
   *
   * @param args.mountPoint the mount point of the script you want to wait for
   * @param args.state the desired state to wait for
   * @param args.timeout if not <code>null</code>, the amount of time to wait maximum
   * @return <code>true</code> if the state was reached within the timeout, <code>false</code>
   * otherwise
   * @throws NoSuchMountPointException when no mount point
   */
  boolean waitForState(args) throws AgentException

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
   * @throws NoSuchActionException if the action cannot be found
   * @throws TimeOutException if the action cannot be completed in the given amount of time
   */
  def waitForAction(args) throws AgentException

  /**
   * Shortcut for both actions.
   * 
   * @see #executeAction(Object)
   * @see #waitForState(Object)
   */
  boolean executeActionAndWaitForState(args) throws AgentException

  /**
   * Shortcut for both actions. Although it is a shortcut, there is a slight difference than
   * issuing those 2 calls yourself: this one will never throw the <code>NoSuchActionException</code>
   *
   * @see #executeAction(Object)
   * @see #waitForAction(Object)
   */
  def executeActionAndWait(args) throws AgentException

  /**
   * @return a list of all mount points currently mounted in the agent 
   */
  def getMountPoints() throws AgentException

  /**
   * @return information about the host 
   */
  def getHostInfo() throws AgentException

  /**
   * Equivalent to the ps command on unix: returns information about all the processes running
   * @return a map where the key is the pid and the value is a map with the following entries when 
   * applicable:
   * <ul>
   * <li>env</li>
   * <li>args</li>
   * <li>cpu</li>
   * <li>credName</li>
   * <li>exe</li>
   * <li>fd</li>
   * <li>mem</li>
   * <li>modules</li>
   * <li>state</li>
   * <li>time</li>
   * </ul>
   */
  def ps() throws AgentException

  /**
   * Sends the signal to the process with the given pid
   */
  void kill(long pid, int signal) throws AgentException


  /**
   * Resynchronizes the agent (to call in case the agent loose synchronization with zookeeper for
   * example)
   */
  void sync() throws AgentException


  /**
   * tails the agent log file
   *
   * @params args.log which log to tail (optional, default to glu-agent.out)
   * @params args.maxLine the number of lines maximum to read
   * @params args.maxSize the maximum size to read
   */
  InputStream tailAgentLog(args) throws AgentException

  /**
   * Returns the content of the file at the given location. If the file is a directory then
   * returns ls() (<code>Map</code>) otherwise returns tail() (<code>InputStream</code>)
   *
   * @params args.location which file to read the content
   * @params args.maxLine the number of lines maximum to read
   * @params args.maxSize the maximum size to read
   */
  def getFileContent(args) throws AgentException
}