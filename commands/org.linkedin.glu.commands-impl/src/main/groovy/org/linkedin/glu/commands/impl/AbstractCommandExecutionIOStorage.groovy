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

import org.linkedin.glu.groovy.utils.io.InputGeneratorStream
import org.linkedin.glu.utils.io.LimitedInputStream
import org.linkedin.glu.utils.io.MultiplexedInputStream
import org.linkedin.groovy.util.config.Config
import org.linkedin.util.annotations.Initializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeoutException
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution
import java.util.concurrent.ExecutorService

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

    commandExecution = saveCommandExecution(commandExecution, stdin)
    commandExecution.storage = this

    commandExecution.command = gluCommandFactory.createGluCommand(commandExecution)

    return commandExecution
  }

  /**
   * {@inheritdoc}
   */
  @Override
  def findCommandExecutionAndStreams(String commandId, def args)
  {
    args= GluGroovyCollectionUtils.subMap(args,
                                          [
                                            'exitValueStream',
                                            'exitValueStreamTimeout',
                                            'stdinStream',
                                            'stdinOffset',
                                            'stdinLen',
                                            'stdoutStream',
                                            'stdoutOffset',
                                            'stdoutLen',
                                            'stderrStream',
                                            'stderrOffset',
                                            'stderrLen',
                                          ])

    int numberOfStreams = 0

    CommandExecution commandExecution = findCommandExecution(commandId)

    if(commandExecution == null)
      return null

    def res = [:]

    res.commandExecution = commandExecution

    Map<String, InputStream> streams = [:]

    def exitValueStream = Config.getOptionalBoolean(args, 'exitValueStream', false)

    // Factory to compute the exit value
    def exitValueFactory = { null }

    if(exitValueStream)
      numberOfStreams++

    def timeout = args.exitValueStreamTimeout

    if(exitValueStream && (timeout != null || commandExecution.isCompleted()))
    {
      exitValueFactory = {
        try
        {
          return commandExecution.getExitValue(timeout).toString()
        }
        catch(TimeoutException e)
        {
          if(log.isDebugEnabled())
            log.debug("timeout reached", e)
          // ok: ignored...
          return null
        }
      }

      InputStream exitValueInputStream = new InputGeneratorStream(exitValueFactory)
      streams[StreamType.EXIT_VALUE.multiplexName] = exitValueInputStream
    }

    def m = [:]
    m[StreamType.STDIN] = commandExecution.hasStdin()
    m[StreamType.STDOUT] = true
    m[StreamType.STDERR] = !commandExecution.redirectStderr

    m.each { streamType, include ->

      def name = streamType.toString().toLowerCase()

      // is the stream requested?
      if(Config.getOptionalBoolean(args, "${name}Stream", false))
      {
        numberOfStreams++

        if(include)
        {
          def inputStreamFactory = {

            // either wait or not
            exitValueFactory()

            def streamAndSize = findStreamWithSize(commandExecution,
                                                   streamType,
                                                   [
                                                     offset: args."${name}Offset",
                                                     len: args."${name}Len",
                                                   ])

            if(streamAndSize)
            {
              return new LimitedInputStream(streamAndSize.stream, streamAndSize.size)
            }
            else
              return null
          }

          streams[streamType.multiplexName] = new InputGeneratorStream(inputStreamFactory)
        }
      }
    }

    // no streams were found
    if(streams.size() > 0)
    {
      // case when requesting only 1 stream
      if(numberOfStreams == 1)
      {
        // we return the only stream
        res.stream = streams.values().iterator().next()
      }
      else
      {
        // we multiplex the result
        res.stream = new MultiplexedInputStream(streams)
      }
    }

    return res
  }

  @Override
  def withOrWithoutCommandExecutionAndStreams(String commandId, args, Closure closure)
  {
    def m = findCommandExecutionAndStreams(commandId, args)
    if(m?.stream)
      m.stream.withStream { closure([stream: it, commandExecution: m.commandExecution])}
    else
      closure(m)
  }

  def findStreamWithSize(String commandId, StreamType streamType, def args)
  {
    findStreamWithSize(findCommandExecution(commandId), streamType, args)
  }

  def findStreamWithSize(CommandExecution command, StreamType streamType, def args)
  {
    def m = findInputStreamWithSize(command, streamType)
    if(m != null)
    {
      long offset = GluGroovyLangUtils.getOptionalLong(args, "offset", 0)
      long len = GluGroovyLangUtils.getOptionalLong(args, "len", -1)

      InputStream is = m.stream

      if(offset < 0)
        offset = m.size + offset

      if(offset > 0)
        is.skip(offset)

      if(len > -1)
        is = new LimitedInputStream(is, len)

      return [stream: is, size: m.size]
    }
    else
      return null
  }

  @Override
  def withStreamAndSize(String commandId, StreamType streamType, def args, Closure closure)
  {
    withOrWithoutStreamAndSize(commandId, streamType, args) { m ->
      if(m) closure(m)
    }
  }

  @Override
  def withOrWithoutStreamAndSize(String commandId, StreamType streamType, def args, Closure closure)
  {
    def m = findStreamWithSize(commandId, streamType, args)

    if(m)
      m.stream.withStream { closure([stream: it, size: m.size]) }
    else
      closure(null)
  }

  /**
   * Synchronously executes the command
   * @return the exitValue of the command execution
   */
  def syncCaptureIO(CommandExecution commandExecution, Closure closure)
  {
    doCaptureIO(commandExecution, null, closure)
  }

  /**
   * Asynchronously executes the command. Returns right away.
   */
  FutureTaskExecution asyncCaptureIO(CommandExecution commandExecution,
                                     ExecutorService executorService,
                                     Closure closure)
  {
    doCaptureIO(commandExecution, executorService, closure)
  }

  private def doCaptureIO(CommandExecution commandExecution,
                          ExecutorService executorService,
                          Closure closure)
  {
    def processing = {
      def res = captureIO(commandExecution, closure)
      commandExecution.completionTime = res.completionTime ?: clock.currentTimeMillis()
      if(res.exception)
        throw res.exception
      else
        return res.exitValue
    }
    def futureExecution = new FutureTaskExecution(processing)
    futureExecution.clock = clock
    commandExecution.futureExecution = futureExecution

    if(executorService)
      futureExecution.runAsync(executorService)
    else
      futureExecution.runSync()
  }

  /**
   * Should save the command in persistent state (as well as stdin if there is any)
   * @return the command (eventually tweaked)
   */
  protected abstract CommandExecution saveCommandExecution(CommandExecution commandExecution,
                                                           def stdin)

  /**
   * @return an input stream for the commandId as well as its size
   */
  protected abstract def findInputStreamWithSize(CommandExecution commandExecution,
                                                 StreamType streamType)

  /**
   * Will be called back to capture the IO
   * @return a map with exitValue of the command execution and completionTime
   */
  protected abstract def captureIO(CommandExecution commandExecution, Closure closure)
}