/*
 * Copyright (c) 2012 Yan Pujante
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



package org.linkedin.glu.commands.impl

import java.util.concurrent.ExecutionException
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution
import org.slf4j.Logger
import org.linkedin.groovy.util.config.Config
import java.util.concurrent.TimeoutException
import java.util.concurrent.CancellationException
import org.linkedin.glu.utils.concurrent.Submitter
import org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils
import org.linkedin.util.clock.SystemClock

/**
 * @author yan@pongasoft.com */
public class CommandExecution<T>
{
  private final String _commandId
  private final def _args
  T command

  synchronized long startTime = 0L
  synchronized long completionTime = 0L

  FutureTaskExecution _futureExecution
  CommandStreamStorage storage

  private final Object _lock = new Object()

  CommandExecution(String commandId, args)
  {
    _args = [*:args] // copy
    _args.redirectStderr = Config.getOptionalBoolean(args, 'redirectStderr', false)
    _commandId = commandId
  }

  String getId()
  {
    _commandId
  }

  Logger getLog()
  {
    command.log
  }

  /**
   * Map of arguments used to create the command
   */
  def getArgs()
  {
    return _args
  }

  boolean isRedirectStderr()
  {
    return _args.redirectStderr
  }

  boolean hasStdin()
  {
    return _args.stdin
  }

  boolean isCompleted()
  {
    completionTime > 0
  }

  void setFutureExecution(FutureTaskExecution futureExecution)
  {
    synchronized(_lock)
    {
      _futureExecution = futureExecution
      _lock.notifyAll()
    }
  }

  void waitForStart(def timeout = null)
  {
    waitForFutureExecution(timeout)
  }

  /**
   * This method waits for 1. the future to be set, 2. the future to have started
   * no longer than the timeout (overall). If another condition is provided then also await
   * for that condition
   *
   * @param timeout
   * @param condition
   * @return whatever <code>condition</code> returns or <code>null</code> if non
   */
  def waitForFutureExecution(def timeout = null, Closure condition = null)
  {
    // already completed => no need to wait
    if(isCompleted())
    {
      if(condition)
        return condition(timeout)
      else
        return null
    }

    def conditions = []

    def clock = SystemClock.INSTANCE

    // 1. wait for future to be set
    if(_futureExecution == null)
      conditions << { t ->
        GroovyConcurrentUtils.awaitFor(clock, t, _lock) {
          _futureExecution != null
        }
      }
    else
      clock = _futureExecution.clock

    // 2. wait for future to be started
    if(!_futureExecution?.isStarted())
      conditions << { t ->
        _futureExecution.waitForStart(t)
      }

    // no other conditions?
    if(conditions.isEmpty())
    {
      if(condition)
        return condition(timeout)
      else
        return null
    }
    else
    {
      // add the condition if provided
      if(condition)
        conditions << condition

      // wait for everything to complete
      GroovyConcurrentUtils.waitMultiple(clock, timeout, conditions)
    }
  }

  void waitForCompletion(def timeout)
  {
    if(!isCompleted())
    {
      try
      {
        waitForFutureExecution(timeout) { t ->
          _futureExecution.get(t)
        }
      }
      catch(ExecutionException e)
      {
        // ok we just want to wait until the command completes but no more than the timeout
      }
      catch(CancellationException e)
      {
        // ok the command was cancelled
      }
    }
  }

  /**
   * @param timeout
   * @return <code>true</code> if the command is completed or completes within the timeout...
   *         <code>false</code> otherwise
   */
  boolean waitForCompletionNoException(def timeout)
  {
    if(!isCompleted())
    {
      try
      {
        waitForFutureExecution(timeout) { t ->
          _futureExecution.get(t)
        }
      }
      catch(ExecutionException e)
      {
        // ok we just want to wait until the command completes but no more than the timeout
      }
      catch(TimeoutException e)
      {
        // ok we just want to wait until the command completes but no more than the timeout
      }
      catch(CancellationException e)
      {
        // ok the command was cancelled
      }
    }

    return isCompleted()
  }

  boolean interruptExecution()
  {
    if(_futureExecution)
      return _futureExecution.cancel(true)
    else
      return false
  }

  def getExitValueIfCompleted()
  {
    if(isCompleted())
      getExitValue(0)
    else
      null
  }

  def getExitValue(timeout)
  {
    try
    {
      waitForFutureExecution(timeout) { t ->
        _futureExecution.get(t)
      }
    }
    catch(ExecutionException e)
    {
      throw e.cause
    }
    catch(CancellationException e)
    {
      // ok the command was cancelled
      return null
    }
  }

  /**
   * Completion value either return the result of the call if succeeded or the exception
   * if an exception was thrown. Does not throw an exception! Does not wait!
   */
  def getCompletionValue()
  {
    if(isCompleted())
      getCompletionValue(0)
    else
      return null
  }

  /**
   * Completion value either return the result of the call if succeeded or the exception
   * if an exception was thrown. Throws only the <code>TimeoutException</code> if cannot get
   * a result in the timeout provided
   */
  def getCompletionValue(timeout) throws TimeoutException
  {
    try
    {
      waitForFutureExecution(timeout) { t ->
        _futureExecution.get(t)
      }
    }
    catch(ExecutionException e)
    {
      e.cause
    }
    catch(CancellationException e)
    {
      // ok the command was cancelled
      return null
    }
  }

  /**
   * Synchronously executes the command
   * @return the exitValue of the command execution
   */
  def syncCaptureIO(Closure closure)
  {
    storage.syncCaptureIO(closure)
  }

  /**
   * Asynchronously executes the command. Returns right away
   */
  FutureTaskExecution asyncCaptureIO(Submitter submitter,
                                     Closure closure)
  {
    storage.asyncCaptureIO(submitter, closure)
  }
}