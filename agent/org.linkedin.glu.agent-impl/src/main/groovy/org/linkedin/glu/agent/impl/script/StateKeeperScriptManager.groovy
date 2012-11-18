/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.impl.storage.Storage

/**
 * The purpose of this class is to keep track and record the state of the script manager
 * so that we can restore in the even of a shutdown/restart.
 *
 * @author ypujante@linkedin.com
 */
def class StateKeeperScriptManager implements ScriptManager
{
  public static final String MODULE = StateKeeperScriptManager.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private final @Delegate ScriptManager _scriptManager
  private final Storage _storage

  StateKeeperScriptManager(args)
  {
    _scriptManager = args.scriptManager
    _storage = args.storage
  }

  void restoreScripts()
  {
    def states = [:]

    // we load all the states firsts
    _storage.mountPoints.each { MountPoint mp ->
      try
      {
        states[mp] = _storage.loadState(mp)
      }
      catch(Exception e)
      {
        log.warn("Error while restoring state for ${mp} (ignored)", e)
      }
    }

    // then we restore each one
    states.values().each { state ->
      restoreScript(states, state)
    }
  }

  /**
   * Restores the script making sure to restore its parent first
   */
  private void restoreScript(states, state)
  {
    // already restored
    if(state == null)
      return

    def mp = state.scriptDefinition.mountPoint
    def parent = state.scriptDefinition.parent

    if(parent)
    {
      // restore parent first
      restoreScript(states, states[parent])
    }

    restoreScript(state)
    
    states[mp] = null
  }

  private void restoreScript(state)
  {
    log.info "Restoring state: ${state}"
    ScriptNode node
    if(state.scriptDefinition.mountPoint == MountPoint.ROOT)
    {
      node = _scriptManager.installRootScript([:])
    }
    else
    {
      node = _scriptManager.installScript(state.scriptDefinition)
    }

    node.scriptState.restore(state)

    // restarting timers if there was any
    state.scriptState.timers?.each {
      node.scheduleTimer(it)
    }
    addListener(node)
  }

  public ScriptNode installRootScript(actionArgs)
  {
    def node = _scriptManager.installRootScript(actionArgs)
    addListener(node)
    return node
  }

  ScriptNode installScript(args)
  {
    def node = _scriptManager.installScript(args)
    addListener(node)
    return node
  }

  private void addListener(ScriptNode node)
  {
    node.scriptState.setStateChangeListener { oldState, newState ->
      _storage.storeState(node.mountPoint, newState)
      if(log.isDebugEnabled())
        log.debug("stateChanged for ${node.mountPoint}: ${oldState} => ${newState}")
    }
  }

  /**
   * Uninstall the script
   * @param force force uninstall regardless of the state of the script
   */
  void uninstallScript(mountPoint, boolean force)
  {
    _scriptManager.uninstallScript(mountPoint, force)
    _storage.clearState(MountPoint.create(mountPoint))
  }

  void shutdown()
  {
    _scriptManager.shutdown()
  }

  void waitForShutdown()
  {
    _scriptManager.waitForShutdown()
  }

  void waitForShutdown(Object timeout)
  {
    _scriptManager.waitForShutdown(timeout)
  }
}
