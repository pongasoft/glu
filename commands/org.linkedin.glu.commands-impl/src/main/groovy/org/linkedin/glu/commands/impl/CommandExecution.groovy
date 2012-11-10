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
import java.util.concurrent.ExecutorService
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution
import org.slf4j.Logger
import org.linkedin.groovy.util.config.Config
import java.util.concurrent.TimeoutException
import java.util.concurrent.CancellationException

/**
 * @author yan@pongasoft.com */
public class CommandExecution<T>
{
  private final String _commandId
  private final def _args
  T command

  synchronized long startTime = 0L
  synchronized long completionTime = 0L

  FutureTaskExecution futureExecution
  CommandStreamStorage storage

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

  void waitForCompletion(def timeout)
  {
    if(!isCompleted())
    {
      try
      {
        futureExecution.get(timeout)
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
        futureExecution.get(timeout)
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
    return futureExecution.cancel(true)
  }

  def getExitValue()
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
      futureExecution.get(timeout)
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
      futureExecution.get(timeout)
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
  FutureTaskExecution asyncCaptureIO(ExecutorService executorService,
                                     Closure closure)
  {
    storage.asyncCaptureIO(executorService, closure)
  }
}