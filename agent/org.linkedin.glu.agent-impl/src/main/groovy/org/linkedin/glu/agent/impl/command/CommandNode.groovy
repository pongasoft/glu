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

package org.linkedin.glu.agent.impl.command

import org.linkedin.glu.agent.api.GluCommand
import org.linkedin.glu.agent.api.Shell
import org.slf4j.Logger
import org.linkedin.glu.agent.api.FutureExecution
import java.util.concurrent.ExecutorService
import org.linkedin.glu.agent.impl.concurrent.FutureTaskExecution
import java.util.concurrent.ExecutionException

/**
 * @author yan@pongasoft.com */
public class CommandNode implements GluCommand
{
  private final def _command
  private final String _commandId

  FutureTaskExecution futureExecution

  CommandNode(def command, String commandId)
  {
    _command = command
    _commandId = commandId
  }

  def getCommand()
  {
    return _command
  }

  def getInvocable()
  {
    return command
  }

  @Override
  String getId()
  {
    return _commandId
  }

  @Override
  Shell getShell()
  {
    return command.shell
  }

  @Override
  Logger getLog()
  {
    return command.log
  }

  @Override
  GluCommand getSelf()
  {
    return this
  }

  /**
   * when the execution started */
  long getStartTime()
  {
    futureExecution.startTime
  }

  /**
   * when the execution completes */
  long getCompletionTime()
  {
    futureExecution.completionTime
  }

  boolean isCompleted()
  {
    completionTime > 0
  }

  FutureExecution runAsync(ExecutorService executorService)
  {
    futureExecution.runAsync(executorService)
  }

  def waitForCompletion(def timeout)
  {
    try
    {
      futureExecution.get(timeout)
    }
    catch(ExecutionException e)
    {
      throw e.cause
    }
  }

  boolean interruptExecution()
  {
    return futureExecution.cancel(true)
  }

  def getExitValue()
  {
    try
    {
      futureExecution.get()
    }
    catch(ExecutionException e)
    {
      throw e.cause
    }
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
  }
}