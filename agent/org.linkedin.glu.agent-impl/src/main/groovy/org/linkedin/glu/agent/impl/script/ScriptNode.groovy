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
import org.linkedin.glu.agent.api.AgentException
import org.slf4j.Logger
import org.linkedin.glu.agent.api.ScriptIllegalStateException
import java.util.concurrent.TimeoutException
import org.linkedin.glu.agent.api.NoSuchActionException

import java.util.concurrent.ExecutionException
import org.linkedin.util.lifecycle.Shutdownable
import org.linkedin.groovy.util.state.StateMachine
import org.linkedin.util.lifecycle.Startable
import org.linkedin.glu.agent.api.GluScript
import org.linkedin.glu.agent.api.Shell
import org.linkedin.glu.agent.api.StateManager
import org.linkedin.glu.agent.api.Timers
import org.linkedin.glu.groovy.utils.concurrent.FutureExecutionImpl
import org.linkedin.glu.groovy.utils.concurrent.FutureExecution

/**
 * A script node (recursive structure)
 *
 * @author ypujante@linkedin.com
 */
def class ScriptNode implements Shutdownable, Startable, GluScript
{
  private final ScriptState _scriptState
  private final ScriptExecution _scriptExecution
  private final Logger _log

  private final Set<MountPoint> _children = new LinkedHashSet()

  ScriptNode(AgentContext agentContext,
             ScriptDefinition scriptDefinition,
             StateMachine stateMachine,
             script,
             Logger log)
  {
    _scriptState = new ScriptState(scriptDefinition, stateMachine, script)
    _log = log
    _scriptExecution = new ScriptExecution(this,
                                           scriptDefinition.getMountPoint().toString(),
                                           _log)
    _scriptExecution.clock = agentContext.clock
  }

  @Override
  void start()
  {
    _scriptExecution.start()
  }

  void shutdown()
  {
    _scriptExecution.shutdown()
  }

  /**
   * {@link #waitForShutdown} waits for shutdown to be complete. On the other end, this call waits
   * for shutdown to be called.
   */
  void waitForShutdownInvocation()
  {
    _scriptExecution.waitForShutdownInvocation()
  }

  void waitForShutdown()
  {
    _scriptExecution.waitForShutdown()
  }

  void waitForShutdown(Object timeout)
  {
    _scriptExecution.waitForShutdown(timeout)
  }

  String getName()
  {
    return mountPoint.toString()
  }

  Logger getLog()
  {
    return _log
  }

  @Override
  Shell getShell()
  {
    script."shell"
  }

  @Override
  Map getParams()
  {
    script."params"
  }

  @Override
  StateManager getStateManager()
  {
    script."stateManager"
  }

  @Override
  Timers getTimers()
  {
    script."timers"
  }

  @Override
  Collection<GluScript> getChildren()
  {
    script."children"
  }

  @Override
  GluScript getParent()
  {
    script."parent"
  }

  @Override
  GluScript getSelf()
  {
    this
  }

  def getScriptDefinition()
  {
    return _scriptState.scriptDefinition
  }

  ScriptExecution getScriptExecution()
  {
    return _scriptExecution
  }

  def getScriptState()
  {
    return _scriptState
  }

  def addChild(MountPoint childMountPoint,
               childInitParameters,
               childScript,
               closure)
  {
    synchronized(_children) {
      assert !_children.contains(childMountPoint)

      if(script.hasProperty('createChild'))
      {
        def child = script.createChild(mountPoint: childMountPoint,
                                       script: childScript,
                                       initParameters: childInitParameters)

        child = closure(child)

        _children << childMountPoint

        if(script.hasProperty('onChildAdded'))
        {
          script.onChildAdded(child: child)
        }

        return child
      }
      else
      {
        throw new AgentException("cannot create a child on a leaf")
      }
    }
  }

  def removeChild(ScriptNode child)
  {
    synchronized(_children) {
      assert _children.contains(child.mountPoint)

      if(script.hasProperty('onChildRemoved'))
      {
        script.onChildRemoved(child: child)
      }

      if(script.hasProperty('destroyChild'))
      {
        script.destroyChild(mountPoint: child.mountPoint, script: child.script)
      }

      _children.remove(child.mountPoint)
    }
  }

  MountPoint getMountPoint()
  {
    return scriptDefinition.mountPoint
  }

  MountPoint getParentMountPoint()
  {
    return scriptDefinition.parent
  }

  Collection<MountPoint> getChildrenMountPoints()
  {
    synchronized(_children) {
      return _children.collect { it }
    }
  }

  def getState()
  {
    return _scriptState.stateMachine.state
  }

  def getFullState()
  {
    return _scriptState.externalFullState
  }

  def getScript()
  {
    return _scriptState.script
  }

  def getInvocable()
  {
    return script
  }

  def waitForState(state, timeout)
  {
    return _scriptState.stateMachine.waitForState(state, timeout)
  }

  void forceChangeState(currentState, error)
  {
    _scriptState.stateMachine.forceChangeState(currentState, error)
  }

  void clearError()
  {
    _scriptState.stateMachine.clearError()
  }

  /**
   * Executes the call. Note that this method is *not* a blocking call
   *
   * @param args.call the call you want to execute
   * @param args.callArgs the arguments to provide the call
   * @return a future execution (asynchronous execution, cancellable...)
   */
  FutureExecution executeCall(args)
  {
    _scriptExecution.executeCall(args.call, args.callArgs, null)
  }

  /**
   * @param args.action
   * @param args.actionArgs
   * @return a future execution (asynchronous execution, cancellable...)
   */
  FutureExecution executeAction(args)
  {
    doExecute(args.action, args.actionArgs)
  }

  /**
   * Schedule a timer.
   * @param args.timer there can only be one timer with a given name
   * @param args.initialFrequency how long to wait the first time (<code>optional</code>)
   * @param args.repeatFrequency how long to wait after the first time
   * @return a future execution (asynchronous execution, cancellable...)
   */
  FutureExecution scheduleTimer(args)
  {
    def timer = computeTimerName(args)
    if(args.repeatFrequency == null)
    {
      throw new NullPointerException("missing repeatFrequency argument")
    }
    
    FutureExecutionImpl future =
      _scriptExecution.scheduleTimer(timer, args.initialFrequency, args.repeatFrequency) {
      _scriptState.removeTimer(timer)
    }
    _scriptState.addTimer([timer: timer, repeatFrequency: args.repeatFrequency])

    return future
  }

  /**
   * @return <code>false</code> if the execution could not be cancelled, typically because it has already completed
   * normally; <code>true</code> otherwise
   */
  boolean cancelTimer(args)
  {
    def timer = computeTimerName(args)
    _scriptExecution.cancelTimer(timer, true)
  }

  /**
   * Extracts the timer name (ok to get a closure in which case the name will be inferred)
   */
  private String computeTimerName(args)
  {
    def timer = args.timer
    if(timer instanceof Closure)
    {
       def timerName = script.metaClass.properties.find { p ->
         timer.is(p.getProperty(script))
       }?.name

      if(timerName == null)
        throw new IllegalArgumentException("invalid timer... only timer properties are allowed")

      timer = timerName
    }
    if(timer == null)
    {
      throw new NullPointerException("missing timer argument")
    }
    return timer
  }

  /**
   * @return <code>false</code> if the execution could not be cancelled, typically because it has already completed
   * normally; <code>true</code> otherwise
   */
  boolean interruptCurrentExecution()
  {
    def current = _scriptExecution.current
    if(current)
      return current.cancel(true)
    else
      return false
  }

  /**
   * @param args.action the name of the action to interrupt
   * @param args.actionId the id of the action to interrupt (only one of each)
   * @return <code>false</code> if the action could not be cancelled, typically because it has already completed
   * normally; <code>true</code> otherwise
   */
  boolean interruptAction(args)
  {
    if(args.action || args.actionId)
    {
      FutureExecution future = findFutureByNameOrId(args)

      if(future)
      {
        return future.cancel(true)
      }
      else
      {
        return false
      }
    }
    else
    {
      return interruptCurrentExecution()
    }
  }

  private FutureExecution findFutureByNameOrId(args)
  {
    FutureExecution future = null
    if(args.action)
    {
      future = _scriptExecution.findFutureActionByName(args.action)
    }
    else
    {
      if(args.actionId)
      {
        future = _scriptExecution.findFutureActionById(args.actionId)
      }
    }
    return future
  }

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
   * @param args.action the name of the action to wait for
   * @param args.actionId the id of the action to wait for (only one of each)
   * @param args.timeout if not <code>null</code>, the amount of time to wait maximum
   * @return the value of the execution
   * @throws org.linkedin.glu.agent.api.NoSuchActionException if the action cannot be found
   * @throws TimeoutException if the action cannot be completed in the given amount of time
   */
  def waitForAction(args)
  {
    FutureExecution future = findFutureByNameOrId(args)

    if(future)
    {
      try
      {
        return future.get(args.timeout)
      }
      catch(ExecutionException e)
      {
        throw e.cause
      }
    }
    else
      throw new NoSuchActionException("action=${args.action} / actionId=${args.actionId}")
  }

  private FutureExecution doExecute(method, methodArgs)
  {
    _scriptExecution.executeAction(method, methodArgs, null)
  }

  /**
   * Checks if this is a valid transition at this moment in time.
   *
   * @throws ScriptIllegalStateException if it is not possible to execute the action because the state
   * does not allow it
   */
  def checkValidTransitionForAction(action)
  {
    try
    {
      return _scriptState.stateMachine.findEndState(action)
    }
    catch(IllegalStateException e)
    {
      throw new ScriptIllegalStateException(e)
    }
  }

  def propertyMissing(String name)
  {
    if(_log.isDebugEnabled())
    {
      _log.debug("propertyMissing: ${name}")
    }
    return script."${name}"
  }

  def methodMissing(String name, args)
  {
    if(_log.isDebugEnabled())
    {
      _log.debug("methodMissing: ${name}(${args})")
    }
    if(_scriptState.stateMachine.availableActions.contains(name))
      executeAction(action: name, actionArgs: args).get()
    else
      _scriptExecution.executeCall(name, *args, null).get()
  }

  public String toString()
  {
    return "${script.class.name} [${mountPoint}]".toString()
  }
}
