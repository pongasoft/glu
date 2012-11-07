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

import java.util.concurrent.ExecutorService

import org.apache.commons.io.input.TeeInputStream

import org.linkedin.glu.orchestration.engine.agents.AgentsService
import org.linkedin.glu.orchestration.engine.authorization.AuthorizationService
import org.linkedin.glu.orchestration.engine.commands.DbCommandExecution.CommandType
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.utils.io.DemultiplexedOutputStream
import org.linkedin.glu.utils.io.LimitedOutputStream
import org.linkedin.glu.utils.io.NullOutputStream
import org.linkedin.util.annotations.Initializable
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.util.clock.Timespan

import org.linkedin.util.lang.MemorySize
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeoutException

import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.linkedin.glu.commands.impl.StreamType
import org.linkedin.glu.commands.impl.CommandExecution
import org.linkedin.glu.commands.impl.CommandExecutionIOStorage
import org.linkedin.glu.commands.impl.CommandStreamStorage
import org.linkedin.glu.commands.impl.GluCommandFactory
import org.apache.tools.ant.util.TeeOutputStream

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

  /**
   * This is somewhat hacky but cannot do it in spring due to circular reference...
   */
  void setCommandExecutionIOStorage(CommandExecutionIOStorage storage)
  {
    storage.gluCommandFactory = createGluCommand as GluCommandFactory
    commandExecutionIOStorage = storage
  }

  @Initializable
  AuthorizationService authorizationService

  /**
   * using 255 by default because that is what a <code>String</code> in GORM can hold
   */
  @Initializable(required = true)
  MemorySize commandExecutionFirstBytesSize = MemorySize.parse('255')

  /**
   * This timeout represents how long to we are willing to wait for the command to complete before
   * returning. The command will still complete in the background... this allows "fast" commands
   * to be handled more naturally in the UI.
   */
  @Initializable
  Timespan defaultSynchronousWaitTimeout = Timespan.parse('1s')

  /**
   * Helper class to manage stream capture
   */
  private class CommandExecutionStream
  {
    StreamType streamType
    boolean captureStream
    CommandStreamStorage storage
    def streams

    private ByteArrayOutputStream _firstBytesOutputStream
    private LimitedOutputStream _limitedOutputStream
    private OutputStream _stream

    def capture(Closure c)
    {
      if(captureStream)
      {
        _firstBytesOutputStream =
          new ByteArrayOutputStream((int) commandExecutionFirstBytesSize.sizeInBytes)
        _limitedOutputStream =
          new LimitedOutputStream(_firstBytesOutputStream, commandExecutionFirstBytesSize)

        storage.withStorageOutput(streamType) { OutputStream stream ->

          if(stream)
            _stream = new TeeOutputStream(stream, _limitedOutputStream)
          else
            _stream = _limitedOutputStream

          streams[streamType.multiplexName] = _stream

          _stream.withStream { c(this) }
        }
      }
      else
        c(this)
    }

    byte[] getBytes()
    {
      _firstBytesOutputStream?.toByteArray()
    }

    Long getTotalNumberOfBytes()
    {
      _limitedOutputStream?.totalNumberOfBytes
    }
  }

  /**
   * The commands that are currently executing  */
  private final Map<String, CommandExecution<DbCommandExecution>> _currentCommandExecutions = [:]

  @Override
  Map<String, DbCommandExecution> findCurrentCommandExecutions(Collection<String> commandIds)
  {
    synchronized(_currentCommandExecutions)
    {
      def map = GluGroovyCollectionUtils.subMap(_currentCommandExecutions, commandIds)
      GluGroovyCollectionUtils.collectKey(map, [:]) { k, v -> v.command }
    }
  }

  @Override
  DbCommandExecution findCommandExecution(Fabric fabric, String commandId)
  {
    DbCommandExecution commandExecution

    synchronized(_currentCommandExecutions)
    {
      commandExecution = _currentCommandExecutions[commandId]?.command
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
      map.commandExecutions = map.commandExecutions?.collect { DbCommandExecution ce ->
        def current = _currentCommandExecutions[ce.commandId]?.command
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
    CommandExecution command = doExecuteShellCommand(fabric, agentName, args) { res ->
      // we do not care about the stream in this version, it will be properly stored in storage
      NullOutputStream.INSTANCE << res.stream
    }

    try
    {
      // we are willing to wait a little bit for the command to complete before returning
      command.getExitValue(defaultSynchronousWaitTimeout)
    }
    catch (TimeoutException e)
    {
      // it is ok... we did not get any result during this amount of time
    }

    return command.id
  }

  /**
   * Executes the command asynchronously
   */
  CommandExecution doExecuteShellCommand(Fabric fabric,
                                         String agentName,
                                         args,
                                         Closure onResultStreamAvailable)
  {
    args = GluGroovyCollectionUtils.subMap(args, ['command', 'redirectStderr', 'stdin'])

    // it is a shell command
    args.type = 'shell'

    // set the various parameters for the call
    args.fabric = fabric.name
    args.agent = agentName
    args.username = authorizationService.executingPrincipal

    // prepare the storage for the command execution (this should make a copy of stdin if
    // there is one)
    CommandExecution command = commandExecutionIOStorage.createStorageForCommandExecution(args)

    // define what will do asynchronously
    def asyncProcessing = { CommandStreamStorage storage ->

      def agentArgs = [*:command.args]

      // we execute the command on the proper agent (this is an asynchronous call which return
      // right away)
      storage.withStorageInput(StreamType.STDIN) { stdin ->
        if(stdin)
          agentArgs.stdin = stdin

        agentsService.executeShellCommand(fabric, agentName, agentArgs)
      }

      def streamResultArgs = [
        id: command.id,
        exitValueStream: true,
        exitValueStreamTimeout: 0, // we block until the command completes
        stdoutStream: true,
        username: args.username
      ]

      if(!command.redirectStderr)
        streamResultArgs.stderrStream = true

      // this is a blocking call
      agentsService.streamCommandResults(fabric, agentName, streamResultArgs) { res ->

        def streams = [:]

        // stdout
        new CommandExecutionStream(streamType: StreamType.STDOUT,
                                   captureStream: true,
                                   storage: storage,
                                   streams: streams).capture { stdout ->

          // stderr
          new CommandExecutionStream(streamType: StreamType.STDERR,
                                     captureStream: !command.redirectStderr,
                                     storage: storage,
                                     streams: streams).capture { stderr ->

            // exitValue
            ByteArrayOutputStream exitValueStream = new ByteArrayOutputStream()
            streams[StreamType.EXIT_VALUE.multiplexName] = exitValueStream

            // this will demultiplex the result
            DemultiplexedOutputStream dos = new DemultiplexedOutputStream(streams)

            dos.withStream { OutputStream os ->
              onResultStreamAvailable(id: command.id, stream: new TeeInputStream(res.stream, os))
            }

            long completionTime = clock.currentTimeMillis()

            // we now update the storage with the various results
            def exitValue = commandExecutionStorage.endExecution(command.id,
                                                                 completionTime,
                                                                 stdout.bytes,
                                                                 stdout.totalNumberOfBytes,
                                                                 stderr.bytes,
                                                                 stderr.totalNumberOfBytes,
                                                                 toString(exitValueStream)).exitValue

            return [exitValue: exitValue, completionTime: completionTime]
          }
        }
      }
    }

    // what to do when the command ends
    def endCommandExecution = {
      synchronized(_currentCommandExecutions)
      {
        command.command.isExecuting = false
        _currentCommandExecutions.remove(command.id)
      }
    }

    synchronized(_currentCommandExecutions)
    {
      command.command.isExecuting = true
      _currentCommandExecutions[command.id] = command
      try
      {
        def future = command.asyncCaptureIO(executorService, asyncProcessing)
        future.onCompletionCallback = endCommandExecution
      }
      catch(Throwable th)
      {
        // this is to avoid the case when the command is added to the map but we cannot
        // run the asynchronous execution which will remove it from the map when complete
        endCommandExecution()
        throw th
      }
    }

    return command
  }

  /**
   * Factory to create a command (first try to read it from the db or store it first in the db)
   */
  def createGluCommand = { CommandExecution command ->

    final String commandId = command.id

    def ce = commandExecutionStorage.findCommandExecution(command.args.fabric, commandId)

    if(!ce)
    {
      ByteArrayOutputStream stdinFirstBytes = null

      Long stdinSize =
        command.storage.withStorageInputWithSize(StreamType.STDIN,
                                                 [ len: commandExecutionFirstBytesSize.sizeInBytes ]) { m ->
          stdinFirstBytes = new ByteArrayOutputStream()
          stdinFirstBytes << m.stream
          return m.size
        } as Long

      ce = commandExecutionStorage.startExecution(command.args.fabric,
                                                  command.args.agent,
                                                  command.args.username,
                                                  command.args.command,
                                                  command.redirectStderr,
                                                  stdinFirstBytes?.toByteArray(),
                                                  stdinSize,
                                                  commandId,
                                                  CommandType.SHELL,
                                                  command.startTime)
    }

    return ce
  }

  @Override
  def executeShellCommand(Fabric fabric, String agentName, args, Closure commandResultProcessor)
  {
    doExecuteShellCommand(fabric,
                          agentName,
                          args,
                          commandResultProcessor)
  }

  @Override
  def withCommandExecutionAndWithOrWithoutStreams(Fabric fabric,
                                                  String commandId,
                                                  def args,
                                                  Closure closure)
  {
    def commandExecution = commandExecutionStorage.findCommandExecution(fabric.name, commandId)

    if(commandExecution?.fabric != fabric.name)
      throw new NoSuchCommandExecutionException(commandId)

    commandExecutionIOStorage.withOrWithoutCommandExecutionAndStreams(commandId, args) { m ->
      if(!m)
        throw new NoSuchCommandExecutionException(commandId)
      closure(m)
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