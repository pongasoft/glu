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
import org.linkedin.glu.groovy.utils.concurrent.FutureExecution
import org.linkedin.glu.agent.api.GluCommand
import org.linkedin.glu.agent.api.Shell
import org.slf4j.Logger

/**
 * @author yan@pongasoft.com */
public class CommandExecution implements GluCommand
{
  private final def _args
  private final def _gluCommand

  FutureTaskExecution futureExecution
  CommandExecutionIOStorage storage

  CommandExecution(args, def gluCommand)
  {
    _args = args
    _gluCommand = gluCommand
  }

  @Override
  String getId()
  {
    _gluCommand.id
  }

  @Override
  Shell getShell()
  {
    _gluCommand.shell
  }

  @Override
  Logger getLog()
  {
    _gluCommand.log
  }

  @Override
  GluCommand getSelf()
  {
    _gluCommand.self
  }

  def getInvocable()
  {
    _gluCommand
  }

  /**
   * Map of arguments used to create the command
   */
  def getArgs()
  {
    return _args
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

  def captureIO(Closure closure)
  {
    storage.captureIO(this, closure)
  }
}