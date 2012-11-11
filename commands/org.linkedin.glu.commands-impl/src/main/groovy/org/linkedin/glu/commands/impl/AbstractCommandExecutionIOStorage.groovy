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

import org.linkedin.util.annotations.Initializable
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.glu.utils.concurrent.Submitter
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution

/**
 * @author yan@pongasoft.com */
public abstract class AbstractCommandExecutionIOStorage implements CommandExecutionIOStorage
{
  public static final String MODULE = AbstractCommandExecutionIOStorage.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable(required = false)
  Clock clock = SystemClock.INSTANCE

  @Initializable(required = true)
  GluCommandFactory gluCommandFactory

  @Initializable
  Submitter submitter = FutureTaskExecution.DEFAULT_SUBMITTER

  @Override
  CommandExecution createStorageForCommandExecution(def args)
  {
    args = [*:args]

    def startTime = clock.currentTimeMillis()

    String commandId = args.id ?:
      "${Long.toHexString(startTime)}-${UUID.randomUUID().toString()}"

    args.id = commandId

    def stdin = args.remove('stdin')
    if(stdin)
      args.stdin = true

    CommandExecution commandExecution = new CommandExecution(commandId, args)
    commandExecution.startTime = startTime

    AbstractCommandStreamStorage storage = saveCommandExecution(commandExecution, stdin)
    storage.ioStorage = this
    commandExecution.storage = storage

    commandExecution.command = gluCommandFactory.createGluCommand(commandExecution)

    return commandExecution
  }

  /**
   * {@inheritdoc}
   */
  @Override
  def findCommandExecutionAndStreams(String commandId, def args)
  {
    CommandExecution commandExecution = findCommandExecution(commandId)

    if(commandExecution)
      [commandExecution: commandExecution, stream: commandExecution.storage.findStorageInput(args)]
    else
      null
  }

  @Override
  def withOrWithoutCommandExecutionAndStreams(String commandId, args, Closure closure)
  {
    CommandExecution commandExecution = findCommandExecution(commandId)

    if(commandExecution)
    {
      commandExecution.storage.withOrWithoutStorageInput(args) { stream ->
        closure([stream: stream, commandExecution: commandExecution])
      }
    }
    else
      closure(null)
  }

  /**
   * Should save the command in persistent state (as well as stdin if there is any)
   * @return the storage
   */
  protected abstract AbstractCommandStreamStorage saveCommandExecution(CommandExecution commandExecution,
                                                                       def stdin)

  /**
   * Will be called back to capture the IO
   * @return a map with exitValue of the command execution and completionTime
   */
  protected abstract def captureIO(CommandExecution commandExecution, Closure closure)
}