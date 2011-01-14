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

package org.linkedin.glu.provisioner.impl.planner

import org.linkedin.glu.provisioner.core.environment.Installation
import org.linkedin.glu.agent.api.Agent
import org.linkedin.groovy.util.state.StateMachine
import org.linkedin.groovy.util.state.StateMachineImpl

/**
 * Uses a standard state machine and adds some extra information that
 * is used by the provisioner to compute transitions
 *
 * author:  Riccardo Ferretti
 * created: Sep 14, 2009
 */
public class InstallationTransitions 
{
  public static final String NO_SCRIPT = 'noscript'

  private StateMachine _sm
  private List _destructiveTransitions
  private String _bounceState
  private String _reinstallState

  def fieldExcludes = []
  def propsExcludes = []

  private static final def TRANSITIONS = [:]

  // we add the transitions for the script management to the standard state machine
  static {
    TRANSITIONS.putAll(Agent.DEFAULT_TRANSITIONS)
    TRANSITIONS[NO_SCRIPT] = [[to: StateMachine.NONE, action: 'installscript']]
    TRANSITIONS[StateMachine.NONE] << [to: NO_SCRIPT, action: 'uninstallscript']
  }

  def InstallationTransitions()
  {
    this (new StateMachineImpl(transitions: TRANSITIONS),
          ['stop', 'unconfigure', 'uninstall','uninstallscript'],
          NO_SCRIPT,
          NO_SCRIPT)
  }

  def InstallationTransitions(StateMachine sm, List destructiveTransitions,
                              String bounceState, String reinstallState)
  {
    _sm = sm
    _destructiveTransitions = destructiveTransitions
    _bounceState = bounceState
    _reinstallState = reinstallState

    _destructiveTransitions.each { t ->
      if (!_sm.availableActions.contains(t))
      {
        throw new IllegalArgumentException("Transition ${t} doesn't exist in state machine (${_sm.availableActions})")
      }
    }
    if (!(_sm.availableStates.contains(bounceState) && _sm.availableStates.contains(reinstallState)))
    {
      throw new IllegalArgumentException("Bounce state (${bounceState}) and reinstall state (${reinstallState}"
        + " must be present in state machine (${_sm.availableStates})")
    }
  }

  /**
   * Returns all available actions
   */
  def getAvailableActions()
  {
    _sm.availableActions
  }

  String getBounceState()
  {
    return _bounceState
  }

  String getReinstallState()
  {
    return _reinstallState
  }

  StateMachine getStateMachine()
  {
    return _sm
  }

  /**
   * Return the through state with the higher priority.
   * Return <code>null</code> if none of the states is considered a through state
   */
  String getThroughState(List states)
  {
    if (states.contains(_reinstallState)) return _reinstallState
    if (states.contains(_bounceState)) return _bounceState
    return null
  }

  /**
   * Return the distructive actions
   */
  List getDestructiveTransitions()
  {
    return _destructiveTransitions
  }
  
  /**
   * Return the through state with the higher priority.
   * Return <code>null</code> if none of the states is considered a through state
   */
  String getThroughState(List states, String throughState)
  {
    return getThroughState(states + [throughState])
  }

  /**
   * Given the two installations, it returns a (optional) through state that
   * needs to be used by the children of this installation
   */
  String getThroughState(Installation first, Installation second)
  {
    def mydelta = new InstallationsDelta(first: first, second: second)
    def throughState

    def sameState = mydelta.isSameState()
    def sameProps = mydelta.areSameProps(propsExcludes)
    def sameInstallation = mydelta.isSameInstallation(fieldExcludes)

    if (sameState && sameProps && sameInstallation)
    {
      // no action required, no through state
    }
    else if (!sameState && sameProps && sameInstallation)
    {
      // only state has changed, no through state
    }
    else if (!sameProps && sameInstallation)
    {
      // bounce, through state is "installed"
      throughState = _bounceState
    }
    else if (!sameInstallation)
    {
      // uninstall/reinstall, through state is "NONE"
      throughState = _reinstallState
    }
    return throughState
  }


  /**
   * Find the shortest path to get from state <code>from</code> to state
   * <code>to</code>.
   * If a through state is not <code>null</code>, the path will take it into account.
   *
   * @return a list of transitions 
   */
  List findShortestPath(String from, String to, String throughState)
  {
    List path

    from = from ?: _reinstallState
    to = to ?: _reinstallState
    
    if (throughState)
    {
      path = _sm.findShortestPath(from, throughState)
      path.addAll (_sm.findShortestPath(throughState, to))
    }
    else
    {
      path = _sm.findShortestPath(from, to)
    }

    return path.collect {it.action}
  }

  List findShortestPath(Installation from, Installation to, String throughState)
  {
    def res = []
    def transitions = findShortestPath(from?.state, to?.state, throughState)

    transitions.each { tr ->
      if (_destructiveTransitions.contains(tr))
      {
        assert from
        res << new Transition(name: tr, target: from, isDestructive: true)
      }
      else
      {
        assert to
        res << new Transition(name: tr, target: to, isDestructive: false)
      }
    }

    return res
  }
}