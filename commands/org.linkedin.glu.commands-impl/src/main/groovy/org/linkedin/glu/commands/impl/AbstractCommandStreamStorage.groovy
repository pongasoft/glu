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
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.glu.utils.io.EmptyInputStream
import org.linkedin.glu.groovy.utils.json.GluGroovyJsonUtils
import org.linkedin.glu.utils.concurrent.Submitter

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
                                            'exitErrorStream',
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

    InputStream res = null

    Map<String, InputStream> streams = [:]

    def exitValueStream = Config.getOptionalBoolean(args, 'exitValueStream', false)
    def exitErrorStream = Config.getOptionalBoolean(args, 'exitErrorStream', false)

    if(exitErrorStream)
    {
      streams[StreamType.exitError.multiplexName] = EmptyInputStream.INSTANCE

      // command completed => check for error
      if(commandExecution.isCompleted())
      {
        def completionValue = commandExecution.completionValue
        if(completionValue instanceof Throwable)
        {
          streams[StreamType.exitError.multiplexName] =
            new InputGeneratorStream(GluGroovyJsonUtils.exceptionToJSON(completionValue))
        }
      }
    }

    // Wait for command completion
    def waitForCommandCompletion = { null }

    def timeout = args.exitValueStreamTimeout

    if(exitValueStream)
    {
      streams[StreamType.exitValue.multiplexName] = EmptyInputStream.INSTANCE

      // command completed => exit value only when no error
      if(commandExecution.isCompleted())
      {
        def completionValue = commandExecution.completionValue
        if(completionValue != null && !(completionValue instanceof Throwable))
        {
          streams[StreamType.exitValue.multiplexName] = new InputGeneratorStream(completionValue)
        }
      }
      else
      {
        if(timeout != null)
        {
          def exitValueFactory = {
            try
            {
              return commandExecution.getExitValue(timeout)?.toString()
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
          streams[StreamType.exitValue.multiplexName] = exitValueInputStream

          // in the case that the caller is willing to wait until timeout to get the exit value
          // then we delay stdout and stderr as well
          waitForCommandCompletion = {
            commandExecution.waitForCompletionNoException(timeout)
          }
        }
      }
    }

    def m = [:]
    m[StreamType.stdin] = commandExecution.hasStdin()
    m[StreamType.stdout] = true
    m[StreamType.stderr] = !commandExecution.redirectStderr

    m.each { StreamType streamType, boolean include ->

      def name = streamType.name()

      // is the stream requested?
      if(Config.getOptionalBoolean(args, "${name}Stream", false))
      {
        streams[streamType.multiplexName] = EmptyInputStream.INSTANCE

        if(include)
        {
          def inputStreamFactory = {

            // either wait or not
            waitForCommandCompletion()

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
      if(streams.size() == 1)
      {
        // we return the only stream
        res = streams.values().iterator().next()
      }
      else
      {
        // we multiplex the result
        streams = streams.findAll { t, s -> !(s instanceof EmptyInputStream) }
        if(streams.isEmpty())
          res = EmptyInputStream.INSTANCE
        else
        {
          res = new MultiplexedInputStream(streams)
          res.submitter = ioStorage.submitter
        }
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
  InputStream findStorageInput(StreamType streamType)
  {
    findStorageInputWithSize(streamType)?.stream
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
  FutureTaskExecution asyncCaptureIO(Submitter submitter, Closure closure)
  {
    doCaptureIO(submitter, closure) as FutureTaskExecution
  }

  private def doCaptureIO(Submitter submitter,
                          Closure closure)
  {
    def processing = {
      def res = null
      try
      {
        res = ioStorage.captureIO(commandExecution, closure)
        if(res.exception)
          throw res.exception
        else
          return res.exitValue
      }
      finally
      {
        commandExecution.completionTime = res?.completionTime ?: ioStorage.clock.currentTimeMillis()
      }
    }
    def futureExecution = new FutureTaskExecution(processing)
    futureExecution.clock = ioStorage.clock
    commandExecution.futureExecution = futureExecution

    if(submitter)
      futureExecution.runAsync(submitter)
    else
      futureExecution.runSync()
  }
}