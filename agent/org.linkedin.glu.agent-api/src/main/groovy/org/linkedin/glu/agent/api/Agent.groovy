/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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

  def static SELF_UPGRADE_TRANSITIONS =
  [
      NONE: [[to: 'installed', action: 'install']],
      installed: [[to: 'NONE', action: 'uninstall'], [to: 'prepared', action: 'prepare']],
      prepared: [[to: 'upgraded', action: 'commit'], [to: 'installed', action: 'rollback']],
      upgraded: [[to: 'NONE', action: 'uninstall']]
  ]

  /********************************************************************
   * Script management
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
   * Interrupts the action provided or the current one if neither <code>action</code> nor
   * <code>actionId</code> is provided. If you provide one, you should provide only one of
   * <code>action</code> or <code>actionId</code>.
   *
   * @param args.mountPoint same mount point provided during {@link #installScript(Object)}
   * @param args.action the lifecycle method you want to interrupt
   * @param args.actionId the id of the action to interrupt
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

  /********************************************************************
   * Agent calls
   ********************************************************************/

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
   * Returns the content of the file at the given location. The difference between
   * <code>maxSize</code> and <code>offset</code> is essentially the fact that with
   * <code>offset</code> you get a map with additional information.
   *
   * @params args.location which file to read the content (note that this needs to be
   *                       properly URI encoded)
   * @params args.maxLine the number of lines maximum to read
   * @params args.maxSize the maximum size to read (can be a <code>MemorySize</code>)
   * @params args.offset the offset in the file where to start (in bytes). If negative, then
   *                     count backward from the end of the file.
   *                     (see {@link Shell#tailFromOffset(Object)})
   *
   * @return * If the file is a directory then ls() (<code>Map</code>) with key is filename
   *           and value is another <code>Map</code>:
   *            - <code>length</code> the total size of the file
   *            - <code>lastModified</code> when the file was modified last
   *            - <code>canonicalPath</code> the file canonical path
   *            - <code>isDirectory</code> <code>boolean</code> for directory yes/no
   *            - <code>isSymbolicLink</code> <code>boolean</code> for symbolic link yes/no
   *         * If <code>offset</code> is specified, then return a map with
   *            - <code>tailStream</code>, the stream to read from (<code>InputStream</code>)
   *            - <code>tailStreamMaxLength</code> how many bytes maximum <code>tailStream</code> contains
   *              (note that <code>tailStream</code> may contain less, but will never contain more!)
   *            - <code>length</code> the total size of the file
   *            - <code>created</code> when the file was created
   *            - <code>lastModified</code> when the file was modified last
   *            - <code>lastAccessed</code> when the file was accessed last
   *               (note that since this call accesses the file, it returns the time it was last
   *                accessed prior to accessing it!)
   *            - <code>canonicalPath</code> the file canonical path
   *            - <code>isSymbolicLink</code> <code>boolean</code> for symbolic link yes/no
   *         * Otherwise returns tail() (<code>InputStream</code>)
   */
  def getFileContent(args) throws AgentException

  /********************************************************************
   * Commands
   ********************************************************************/

  /**
   * Executes the shell command. Note that this methods returns right away and does not wait
   * for the command to complete.
   *
   * @param args.command the command to execute. It will be delegated to the shell so it should
   *                     be native to the OS on which the agent runs (required)
   * @param args.stdin any input that can "reasonably" be converted into an
   *                   <code>InputStream</code>) to provide to the command line execution
   *                   (optional, default to no stdin)
   * @param args.redirectStderr <code>boolean</code> to redirect stderr into stdout
   *                            (optional, default to <code>false</code>). Note that this can also
   *                            be accomplished with the command itself with something like "2>&1"
   * @return a map with id being the id of the command being executed
   */
  def executeShellCommand(args) throws AgentException

  /**
   * Wait (no longer than the timeout provided) for the command to complete and return the exit
   * value
   *
   * @param args.id the id of the command (as returned by {@lin #executeShellCommand})
   * @param args.timeout if not <code>null</code>, the amount of time to wait maximum. If
   *                     <code>null</code>, wait until the command completes.
   * @return the exit value
   */
  def waitForCommand(args) throws NoSuchCommandException, TimeOutException, AgentException

  /**
   * This method allows you to start streaming the results (stdout, stderr,...) while the command
   * is still running.
   *
   * @param args.id the id of the command (as returned by {@lin #executeShellCommand})
   * @param args.exitValueStream if you want the exit value to be part of the stream
   *                             (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.exitValueStreamTimeout how long to wait to get the exit value if the command is
   *                                    not completed yet (optional, in the event that
   *                                    <code>exitValueStream</code> is set to
   *                                    <code>true</code> and <code>exitValueStreamTimeout</code>
   *                                    is not provided, it will not block and return no exit value
   *                                    stream)
   * @param args.stdinStream if you want stdin to be part of the stream
   *                         (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.stdinOffset where to start in the stdin stream (optional, <code>int</code>,
   *                          <code>0</code> by default)
   * @param args.stdinLen how many bytes to read maximum (optional, <code>int</code>,
   *                       <code>-1</code> by default which means read all)
   * @param args.stdoutStream if you want stdout to be part of the stream
   *                          (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.stdoutOffset where to start in the stdout stream (optional, <code>int</code>,
   *                          <code>0</code> by default)
   * @param args.stdoutLen how many bytes to read maximum (optional, <code>int</code>,
   *                       <code>-1</code> by default which means read all)
   * @param args.stderrStream if you want stdout to be part of the stream
   *                          (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.stderrOffset where to start in the stdout stream (optional, <code>int</code>,
   *                          <code>0</code> by default)
   * @param args.stderrLen how many bytes to read maximum (optional, <code>int</code>,
   *                       <code>-1</code> by default which means read all)
   * @return a map with <code>startTime</code>, <code>completionTime</code> (if any) and
   *         <code>stream</code> (if any)
   * @throw NoSuchCommandException if not found
   */
  def streamCommandResults(def args) throws NoSuchCommandException, AgentException

  /**
   * Interrupts the command.
   *
   * @param args.id the id of the command to interrupt
   * @return <code>true</code> if the command was interrupted properly or <code>false</code> if
   * there was no such command or already completed
   */
  boolean interruptCommand(args) throws AgentException

  /********************************************************************
   * Tags
   ********************************************************************/
  /**
   * @return the number of tags
   */
  int getTagsCount() throws AgentException

  /**
   * @return <code>true</code> if there are no tags
   */
  boolean hasTags() throws AgentException

  /**
   * @return the set of all tags
   */
  Set<String> getTags() throws AgentException

  /**
   * @return <code>true</code> if the given tag is present
   */
  boolean hasTag(String tag) throws AgentException

  /**
   * @return <code>true</code> if the given tags are present (all of them)
   */
  boolean hasAllTags(Collection<String> tags) throws AgentException

  /**
   * @return <code>true</code> if at least one of them is present
   */
  boolean hasAnyTag(Collection<String> tags) throws AgentException

  /**
   * Adds a tag.
   *
   * @return <code>true</code> if the tag which was added was a new tag, otherwise
   * <code>false</code>
   */
  boolean addTag(String tag) throws AgentException

  /**
   * Adds all the tags.
   *
   * @return the set of tags that were added (empty set if they were all already present)
   */
  Set<String> addTags(Collection<String> tags) throws AgentException

  /**
   * Removes the provided tag.
   *
   * @return <code>true</code> if the tag was removed or <code>false</code> if the tag was not
   * present in the first place
   */
  boolean removeTag(String tag) throws AgentException

  /**
   * Removes all the tags.
   *
   * @return the set of tags that were not present (empty set if they were all already present)
   */
  Set<String> removeTags(Collection<String> tags) throws AgentException


  /**
   * Allow you to set the exact set of tags you want. Equivalent to calling (in an atomic fashion)
   * <pre>
   * removeTags(getTags())
   * addTags(tags)
   * </pre>
   * @param tags tags to set
   */
  void setTags(Collection<String> tags) throws AgentException;
}