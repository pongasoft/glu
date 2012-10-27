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

import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import org.apache.commons.io.input.TeeInputStream
import org.apache.commons.io.output.TeeOutputStream
import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.orchestration.engine.authorization.AuthorizationService
import org.linkedin.glu.orchestration.engine.commands.CommandExecution.CommandType
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.utils.io.DemultiplexedOutputStream
import org.linkedin.glu.utils.io.LimitedOutputStream
import org.linkedin.glu.utils.io.NullOutputStream
import org.linkedin.util.annotations.Initializable
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.util.clock.Timespan
import org.linkedin.util.io.IOUtils
import org.linkedin.util.lang.MemorySize
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.glu.agent.rest.common.AgentRestUtils
import org.linkedin.groovy.util.config.Config
import java.util.concurrent.TimeoutException
import org.linkedin.glu.groovy.utils.concurrent.GluGroovyConcurrentUtils
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils

/**
 * @author yan@pongasoft.com  */
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

  @Initializable(required = true)
  CommandExecutionIOStorage commandExecutionIOStorage

  @Initializable
  AuthorizationService authorizationService

  /**
   * using 255 by default because that is what a <code>String</code> in GORM can hold
   */
  @Initializable(required = true)
  MemorySize commandExecutionFirstBytesSize = MemorySize.parse('255')

  /**
   * This timeout represents how long to we are willing to wait for the command to complete before
   * returning. The commmand will still complete in the background... this allows "fast" commands
   * to be handled more naturally in the UI.
   */
  @Initializable
  Timespan defaultSynchronousWaitTimeout = Timespan.parse('1s')

  /**
   * The commands that are currently executing  */
  private final Map<String, CommandExecution> _currentCommandExecutions = [:]

  @Override
  Map<String, CommandExecution> findCurrentCommandExecutions(Collection<String> commandIds)
  {
    synchronized(_currentCommandExecutions)
    {
      GluGroovyCollectionUtils.subMap(_currentCommandExecutions, commandIds)
    }
  }

  @Override
  CommandExecution findCommandExecution(Fabric fabric, String commandId)
  {
    CommandExecution commandExecution

    synchronized(_currentCommandExecutions)
    {
      commandExecution = _currentCommandExecutions[commandId]
      if(commandExecution?.fabric != fabric.name)
        commandExecution = null
    }

    if(!commandExecution)
      commandExecution = commandExecutionStorage.findCommandExecution(fabric.name, commandId)

    return commandExecution
  }

  @Override
  Map findCommandExecutions(Fabric fabric, String agentName, def params)
  {
    def map = commandExecutionStorage.findCommandExecutions(fabric.name, agentName, params)

    synchronized(_currentCommandExecutions)
    {
      // replace db by current running
      map.commandExecutions = map.commandExecutions?.collect { CommandExecution ce ->
        def current = _currentCommandExecutions[ce.commandId]
        if(current)
          return current
        else
          return ce
      }
    }

    return map
  }

  @Override
  String executeShellCommand(Fabric fabric, String agentName, args)
  {
    CountDownLatch commandStarted = new CountDownLatch(1)

    String commandId = null

    def onCommandStarted = { CommandExecution ce ->
      commandId = ce.commandId

      // the command has been started and the id set
      commandStarted.countDown()
    }

    def onResultStreamAvailable = { res ->
      // we can now consume the entire result (we disregard the output as it is already
      // being saved locally
      res.stream.withStream { InputStream is ->
        IOUtils.copy(is, NullOutputStream.INSTANCE)
      }
    }

    String username = authorizationService.executingPrincipal

    def commandExecutor = {->
      try
      {
        doExecuteShellCommand(fabric,
                              agentName,
                              username,
                              args,
                              onCommandStarted,
                              onResultStreamAvailable)
      }
      finally
      {
        // if an exception is thrown before "onCommandStarted" is executed,
        // then we could block indefinitely!
        commandStarted.countDown()
      }
    }

    // we execute the command in a separate thread
    def future = executorService.submit(GluGroovyConcurrentUtils.asCallable(commandExecutor))

    try
    {
      future.get(defaultSynchronousWaitTimeout.getDurationInMilliseconds(),
                 TimeUnit.MILLISECONDS)
    }
    catch (TimeoutException e)
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
                          null,
                          commandResultProcessor)
  }

  @Override
  void writeStream(Fabric fabric,
                   String commandId,
                   StreamType streamType,
                   Closure closure)
  {
    def commandExecution = commandExecutionStorage.findCommandExecution(fabric.name, commandId)

    if(commandExecution?.fabric != fabric.name)
      throw new NoSuchCommandExecutionException(commandId)


    switch(streamType)
    {
      case StreamType.STDIN:
        commandExecutionIOStorage.withStdinInputStream(commandExecution) { stream, size ->
          doWriteStream(stream,
                        commandExecution,
                        commandExecution.stdinTotalBytesCount ?: -1,
                        closure) { is, os ->
            os << is
          }
        }
        break

      case StreamType.STDOUT:
        commandExecutionIOStorage.withResultInputStream(commandExecution) { stream, size ->
          doWriteStream(stream,
                        commandExecution,
                        commandExecution.stdoutTotalBytesCount ?: -1,
                        closure) { is, os ->
            AgentRestUtils.demultiplexExecStream(is, os, NullOutputStream.INSTANCE)
          }
        }
        break

      case StreamType.STDERR:
        commandExecutionIOStorage.withResultInputStream(commandExecution) { stream, size ->
          doWriteStream(stream,
                        commandExecution,
                        commandExecution.stderrTotalBytesCount ?: -1,
                        closure) { is, os ->
            AgentRestUtils.demultiplexExecStream(is, NullOutputStream.INSTANCE, os)
          }
        }
        break

      case StreamType.MULTIPLEXED:
        commandExecutionIOStorage.withResultInputStream(commandExecution) { stream, size ->
          doWriteStream(stream,
                        commandExecution,
                        size ?: 0,
                        closure) { is, os ->
            os << is
          }
        }
        break

      default:
        throw new RuntimeException("not reached")
    }
  }

  private void doWriteStream(InputStream inputStream,
                             CommandExecution commandExecution,
                             long contentSize,
                             Closure outputProvider,
                             Closure outputWriter)
  {
    OutputStream out = outputProvider(commandExecution, inputStream == null ? 0 : contentSize)
    if(inputStream && out)
    {
      if(contentSize != -1)
        out = new LimitedOutputStream(out, contentSize)
      
      new BufferedOutputStream(out).withStream { stream ->
        outputWriter(inputStream, stream)
      }
    }
  }

  /**
   * This method can be called from a thread where there is no more executing principal...
   * hence the username argument
   */
  private def doExecuteShellCommand(Fabric fabric,
                                    String agentName,
                                    String username,
                                    args,
                                    Closure onCommandStarted,
                                    Closure onResultStreamAvailable)
  {
    long startTime = clock.currentTimeMillis()

    commandExecutionIOStorage.captureIO { StreamStorage storage ->

      // stdin
      ByteArrayOutputStream stdinFirstBytes = null
      LimitedOutputStream stdinLimited = null

      if(args.stdin)
      {
        stdinFirstBytes =
          new ByteArrayOutputStream((int) commandExecutionFirstBytesSize.sizeInBytes)
        stdinLimited =
          new LimitedOutputStream(stdinFirstBytes, commandExecutionFirstBytesSize)

        // in parallel, write stdin to IO storage
        def stream = storage.findStdinStorage()
        if(stream)
          stream = new TeeOutputStream(stdinLimited, new BufferedOutputStream(stream))
        else
          stream = stdinLimited

        args.stdin = new TeeInputStream(args.stdin, stream, true)
      }

      String commandId = agentsService.executeShellCommand(fabric, agentName, args).id

      boolean redirectStderr = Config.getOptionalBoolean(args, 'redirectStderr', false)

      // first we store the command in the storage
      def commandExecution = commandExecutionStorage.startExecution(fabric.name,
                                                                    agentName,
                                                                    username,
                                                                    args.command,
                                                                    redirectStderr,
                                                                    commandId,
                                                                    CommandType.SHELL,
                                                                    startTime)

      synchronized(_currentCommandExecutions)
      {
        commandExecution.isExecuting = true
        _currentCommandExecutions[commandId] = commandExecution
      }

      try
      {

        if(onCommandStarted)
          onCommandStarted(commandExecution)

        // now command execution is known... set it in the storage
        storage.commandExecution = commandExecution


        args = [
                id: commandId,
                exitValueStream: true,
                exitValueStreamTimeout: 0,
                stdoutStream: true,
        ]

        if(!redirectStderr)
          args.stderrStream = true

        agentsService.streamCommandResults(fabric, agentName, args) { res ->
          // stdout
          ByteArrayOutputStream stdoutFirstBytes =
            new ByteArrayOutputStream((int) commandExecutionFirstBytesSize.sizeInBytes)
          LimitedOutputStream stdoutLimited =
            new LimitedOutputStream(stdoutFirstBytes, commandExecutionFirstBytesSize)

          // stderr
          ByteArrayOutputStream stderrFirstBytes =
            new ByteArrayOutputStream((int) commandExecutionFirstBytesSize.sizeInBytes)
          LimitedOutputStream stderrLimited =
            new LimitedOutputStream(stderrFirstBytes, commandExecutionFirstBytesSize)

          // exitValue
          ByteArrayOutputStream exitValueStream = new ByteArrayOutputStream()

          def streams = [
                  "O": stdoutLimited, // no more than 255 bytes
                  "E": stderrLimited, // no more than 255 bytes
                  "V": exitValueStream
          ]

          // this will demultiplex the result
          DemultiplexedOutputStream dos = new DemultiplexedOutputStream(streams)

          // we can now consume the entire result while making sure we save it as well
          def stream = storage.findResultStreamStorage()
          if(stream)
            stream = new TeeOutputStream(dos, new BufferedOutputStream(stream))
          else
            stream = dos

          def closureResult = stream.withStream { OutputStream os ->
            onResultStreamAvailable(id: commandId, stream: new TeeInputStream(res.stream, os))
          }

          // we now update the storage with the various results
          def execution = commandExecutionStorage.endExecution(commandId,
                                                               clock.currentTimeMillis(),
                                                               stdinFirstBytes?.toByteArray(),
                                                               stdinLimited?.totalNumberOfBytes,
                                                               stdoutFirstBytes?.toByteArray(),
                                                               stdoutLimited?.totalNumberOfBytes,
                                                               stderrFirstBytes?.toByteArray(),
                                                               stderrLimited?.totalNumberOfBytes,
                                                               toString(exitValueStream))
          synchronized(_currentCommandExecutions)
          {
            execution.isExecuting = false
            _currentCommandExecutions[commandId] = execution
          }

          return closureResult
        }
      }
      finally
      {
        synchronized(_currentCommandExecutions)
        {
          commandExecution.isExecuting = false
          _currentCommandExecutions.remove(commandId)
        }
      }
    }
  }

  private String toString(ByteArrayOutputStream stream)
  {
    if(stream)
      new String(stream.toByteArray(), "UTF-8")
    else
      null
  }
}