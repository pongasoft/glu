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

package org.linkedin.glu.orchestration.engine.commands

import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.util.clock.SystemClock
import org.linkedin.util.clock.Clock
import java.util.concurrent.ExecutorService
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import org.linkedin.glu.orchestration.engine.authorization.AuthorizationService
import org.linkedin.glu.orchestration.engine.commands.CommandExecution.CommandType
import org.apache.commons.io.input.TeeInputStream
import org.linkedin.util.io.IOUtils
import org.linkedin.glu.utils.io.NullOutputStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit
import org.linkedin.util.clock.Timespan

/**
 * @author yan@pongasoft.com */
public class CommandsServiceImpl implements CommandsService
{
  public static final String MODULE = CommandsServiceImpl.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  // will be dependency injected
  @Initializable(required = true)
  AgentsService agentsService

  @Initializable
  Clock clock = SystemClock.INSTANCE

  @Initializable(required = true)
  ExecutorService executorService

  @Initializable(required = true)
  CommandExecutionStorage commandExecutionStorage

  @Initializable
  AuthorizationService authorizationService

  /**
   * This timeout represents how long to we are willing to wait for the command to complete before
   * returning. The commmand will still complete in the background... this allows "fast" commands
   * to be handled more naturally in the UI.
   */
  @Initializable
  Timespan defaultSynchronousWaitTimeout = Timespan.parse('1s')

  /**
   * The commands that are currently executing */
  private final Map<String, CommandExecution> _currentCommandExecutions = [:]

  @Override
  String executeShellCommand(Fabric fabric, String agentName, args)
  {
    CountDownLatch commandStarted = new CountDownLatch(1)

    String commandId = null

    def cop = { res ->
      commandId = res.id

      // the command has been started and the id set
      commandStarted.countDown()

      // we can now consume the entire result (we disregard the output as it is already
      // being saved locally
      res.stream.withStream { InputStream is ->
        IOUtils.copy(is, NullOutputStream.INSTANCE)
      }
    }

    String username = authorizationService.executingPrincipal

    def commandExecutor = { ->
      try
      {
        doExecuteShellCommand(fabric, agentName, username, args, cop)
      }
      catch(Throwable th)
      {
        // if an exception is thrown before "cop" is executed, then we could block indefinitely!
        commandStarted.countDown()
        log.warn("unexpected exception while executing shell command [${args.command}]", th)
      }
    }

    // we execute the command in a separate thread
    def future = executorService.submit(commandExecutor as Callable)

    try
    {
      future.get(defaultSynchronousWaitTimeout.getDurationInMilliseconds(),
                 TimeUnit.MILLISECONDS)
    }
    catch(TimeoutException)
    {
      // it is ok... we did not get any result during this amount of time
    }

    // we wait for the command to be started...
    commandStarted.await()

    return commandId
  }

  @Override
  def executeShellCommand(Fabric fabric, String agentName, args, Closure commandResultProcessor)
  {
    doExecuteShellCommand(fabric,
                        agentName,
                        authorizationService.executingPrincipal,
                        args,
                        commandResultProcessor)
  }

  /**
   * This method can be called from a thread where there is no more executing principal...
   * hence the username argument
   */
  private def doExecuteShellCommand(Fabric fabric,
                                    String agentName,
                                    String username,
                                    args,
                                    Closure commandResultProcessor)
  {
    long startTime = clock.currentTimeMillis()

    agentsService.executeShellCommand(fabric, agentName, args) { res ->

      String commandId = res.id

      // first we store the command in the storage
      def commandExecution = commandExecutionStorage.startExecution(fabric.name,
                                                                    agentName,
                                                                    username,
                                                                    args.command,
                                                                    commandId,
                                                                    CommandType.SHELL,
                                                                    startTime)

      synchronized(_currentCommandExecutions)
      {
        _currentCommandExecutions[commandId] = commandExecution
      }

      try
      {
        // we can now consume the entire result while making sure we save it as well
        def resultOutputStream = commandExecutionStorage.getResultOutputStream(commandId)
        resultOutputStream = new BufferedOutputStream(resultOutputStream)
        def closureResult = resultOutputStream.withStream { OutputStream os ->
          commandResultProcessor(id: commandId, stream: new TeeInputStream(res.stream, os))
        }

        // we now update the storage with the various results
        commandExecutionStorage.endExecution(commandId,
                                             clock.currentTimeMillis(),
                                             null,
                                             null,
                                             null,
                                             null)

        return closureResult
      }
      finally
      {
        synchronized(_currentCommandExecutions)
        {
          _currentCommandExecutions.remove(commandId)
        }
      }
    }
  }
}