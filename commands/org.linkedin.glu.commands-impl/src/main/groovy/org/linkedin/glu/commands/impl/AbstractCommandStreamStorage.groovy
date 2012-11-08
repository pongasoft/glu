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

import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.linkedin.groovy.util.config.Config
import java.util.concurrent.TimeoutException
import org.linkedin.glu.groovy.utils.io.InputGeneratorStream
import org.linkedin.glu.utils.io.LimitedInputStream
import org.linkedin.glu.utils.io.MultiplexedInputStream
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution
import java.util.concurrent.ExecutorService
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author yan@pongasoft.com */
public abstract class AbstractCommandStreamStorage<T extends AbstractCommandExecutionIOStorage>
  implements CommandStreamStorage
{
  public static final String MODULE = AbstractCommandStreamStorage.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  CommandExecution commandExecution
  T ioStorage

  @Override
  def withOrWithoutStorageOutput(StreamType streamType, Closure c)
  {
    def output = findStorageOutput(streamType)
    if(output)
      output.withStream { c(it) }
    else
      c(null)
  }

  @Override
  def withOrWithoutStorageInput(StreamType streamType, Closure c)
  {
    def input = findStorageInput(streamType)
    if(input)
      input.withStream { c(it) }
    else
      c(null)
  }

  @Override
  InputStream findStorageInput(def args)
  {
    args = GluGroovyCollectionUtils.subMap(args,
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

    InputStream res = null

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

    m.each { StreamType streamType, boolean include ->

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

            def streamAndSize = findStorageInputWithSize(streamType,
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
        res = streams.values().iterator().next()
      }
      else
      {
        // we multiplex the result
        res = new MultiplexedInputStream(streams)
      }
    }

    return res
  }

  @Override
  def withOrWithoutStorageInput(def args, Closure closure)
  {
    InputStream stream = findStorageInput(args)
    if(stream)
      stream.withStream { closure(stream) }
    else
      closure(null)
  }

  @Override
  def findStorageInputWithSize(StreamType streamType, def args)
  {
    def m = findStorageInputWithSize(streamType)
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
  def withOrWithoutStorageInputWithSize(StreamType streamType, Object args, Closure closure)
  {
    def m = findStorageInputWithSize(streamType, args)

    if(m)
      m.stream.withStream { closure([stream: it, size: m.size]) }
    else
      closure(null)
  }

  @Override
  def withStorageInputWithSize(StreamType streamType, Object args, Closure closure)
  {
    withOrWithoutStorageInputWithSize(streamType, args) { m ->
      if(m) closure(m)
    }
  }

  @Override
  def syncCaptureIO(Closure closure)
  {
    doCaptureIO(null, closure)
  }

  @Override
  FutureTaskExecution asyncCaptureIO(ExecutorService executorService, Closure closure)
  {
    doCaptureIO(executorService, closure) as FutureTaskExecution
  }

  private def doCaptureIO(ExecutorService executorService,
                          Closure closure)
  {
    def processing = {
      def res = ioStorage.captureIO(commandExecution, closure)
      commandExecution.completionTime = res.completionTime ?: ioStorage.clock.currentTimeMillis()
      if(res.exception)
        throw res.exception
      else
        return res.exitValue
    }
    def futureExecution = new FutureTaskExecution(processing)
    futureExecution.clock = ioStorage.clock
    commandExecution.futureExecution = futureExecution

    if(executorService)
      futureExecution.runAsync(executorService)
    else
      futureExecution.runSync()
  }
}