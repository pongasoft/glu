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

package org.linkedin.glu.agent.impl.capabilities

import org.apache.tools.ant.taskdefs.Execute
import org.linkedin.glu.agent.api.ShellExecException
import org.linkedin.glu.commands.impl.StreamType
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.glu.groovy.utils.io.InputGeneratorStream
import org.linkedin.glu.groovy.utils.json.GluGroovyJsonUtils
import org.linkedin.glu.utils.io.MultiplexedInputStream
import org.linkedin.glu.utils.io.NullOutputStream

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ExecutionException
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution

import org.linkedin.glu.groovy.utils.io.DestroyProcessInputStream

/**
 * Because the logic of exec is quite complicated, it requires its own class
 *
 * @author yan@pongasoft.com  */
private class ShellExec
{
  public static final String MODULE = ShellExec.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  // the shell which created this class
  ShellImpl shell

  // the args passed to shell.exec
  Map args

  private String _commandLine
  private InputStream _stdin
  private boolean _redirectStderr
  private boolean _failOnError
  private def _requiredRes

  // second map contains: stream, streamOfBytes
  private Map<StreamType, Map> _processIO = [:]

  // process
  private boolean _destroyProcessInFinally = true
  private Process _process

  def exec()
  {
    initCommandLine()

    _stdin = shell.toInputStream(args.stdin)

    _redirectStderr = GluGroovyLangUtils.getOptionalBoolean(args.redirectStderr, false)
    _failOnError = GluGroovyLangUtils.getOptionalBoolean(args.failOnError, true)
    _requiredRes = args.res ?: 'stdout'

    // stdout
    initOutput(StreamType.STDOUT)

    // stderr
    if(!_redirectStderr)
      initOutput(StreamType.STDERR)

    // builds the process
    def pb = new ProcessBuilder(['bash', '-c', _commandLine])
    pb.redirectErrorStream(_redirectStderr)

    _process = pb.start()

    try
    {
      afterProcessStarted()
    }
    finally
    {
      if(_destroyProcessInFinally)
        _process.destroy()
    }
  }

  private def afterProcessStarted()
  {
    // stdout
    startOutputThread(StreamType.STDOUT) { _process.inputStream }

    // when redirecting stderr, there is nothing on stderr... no need to create a thread!
    if(!_redirectStderr)
    {
      startOutputThread(StreamType.STDERR) { _process.errorStream }
    }

    if(_requiredRes.toLowerCase().endsWith("stream"))
      return createRequiredInputStream()
    else
      return executeBlockingCall()
  }

  /**
   * Creates the appropriate stream but make sure we still destroy the process when the
   * stream closes
   */
  private InputStream createRequiredInputStream()
  {
    // execute the block call asynchronously
    FutureTaskExecution future = new FutureTaskExecution(executeBlockingCall)
    future.description = _commandLine

    InputStream inputStream

    switch(_requiredRes)
    {
      case "stdoutStream":
        inputStream = _process.inputStream
        break

      case "stderrStream":
        inputStream = _process.errorStream
        break

      case "exitValueStream":
        inputStream = new InputGeneratorStream({
          try
          {
            future.get().toString()
          }
          catch(ExecutionException e)
          {
            throw e.cause
          }
        })
        break

      case "stream":
        inputStream = createMultiplexedInputStream(future)
        break

      default:
        throw new RuntimeException("should not be here with ${_requiredRes}")
    }

    inputStream = new DestroyProcessInputStream(_process, inputStream)

    // we can no longer destroy the process in the finally, it will be destroyed when the
    // input stream is closed
    _destroyProcessInFinally = false

    future.runAsync(shell.executorService)

    return inputStream
  }

  /**
   * Create one input stream which multiples stdout, stderr, exitValue and exitError
   */
  private InputStream createMultiplexedInputStream(FutureTaskExecution future)
  {
    // execute the block call asynchronously
    def streams = [:]

    // stdout
    if(_processIO[StreamType.STDOUT]?.stream == null)
    {
      streams[StreamType.STDOUT.multiplexName] = _process.inputStream
    }

    // stderr
    if(_processIO[StreamType.STDERR]?.stream == null)
    {
      streams[StreamType.STDERR.multiplexName] = _process.errorStream
    }

    // exit value (as a stream)
    InputStream exitValueInputStream = new InputGeneratorStream({
      try
      {
        future.get().toString()
      }
      catch(Throwable th)
      {
        // ok to ignore... will be part of the exit error stream...
      }
    })
    streams[StreamType.EXIT_VALUE.multiplexName] = exitValueInputStream

    // exit error (as a stream)
    InputStream exitErrorInputStream = new InputGeneratorStream({
      try
      {
        future.get().toString()
        // no error
        return null
      }
      catch(ExecutionException e)
      {
        GluGroovyJsonUtils.exceptionToJSON(e.cause)
      }
      catch(Throwable th)
      {
        GluGroovyJsonUtils.exceptionToJSON(th)
      }
    })
    streams[StreamType.EXIT_ERROR.multiplexName] = exitErrorInputStream

    return new MultiplexedInputStream(streams)
  }

  private def executeBlockingCall = {

    // if stdin then provide it to subprocess
    _stdin?.withStream { InputStream sis ->
      new BufferedOutputStream(_process.outputStream).withStream { os ->
        os << new BufferedInputStream(sis)
      }
    }

    // make sure that the thread complete properly
    [StreamType.STDOUT, StreamType.STDERR].each {
      _processIO[it]?.future?.get()
    }

    // we wait for the process to be done
    int exitValue = _process.waitFor()

    Map<StreamType, byte[]> bytes =
      GroovyCollectionsUtils.toMapKey([StreamType.STDOUT, StreamType.STDERR]) {
        _processIO[it]?.streamOfBytes?.toByteArray() ?: new byte[0]
      }

    // handling failOnError flag
    if(_failOnError && Execute.isFailure(exitValue))
    {
      if(log.isDebugEnabled())
      {
        log.debug("Error while executing command ${_commandLine}: ${exitValue}")
        log.debug("output=${shell.toStringOutput(bytes[StreamType.STDOUT])}")
        log.debug("error=${shell.toStringOutput(bytes[StreamType.STDERR])}")
      }

      ShellExecException exception =
      new ShellExecException("Error while executing command ${_commandLine}: res=${exitValue} - output=${shell.toLimitedStringOutput(bytes[StreamType.STDOUT], 512)} - error=${shell.toLimitedStringOutput(bytes[StreamType.STDERR], 512)}".toString())
      exception.res = exitValue
      exception.output = bytes[StreamType.STDOUT]
      exception.error = bytes[StreamType.STDERR]

      throw exception
    }

    // handling final output
    switch(_requiredRes)
    {
      case 'stdout':
        return shell.toStringOutput(bytes[StreamType.STDOUT])

      case 'stdoutBytes':
        return bytes[StreamType.STDOUT]

      case 'stderr':
        return shell.toStringOutput(bytes[StreamType.STDERR])

      case 'stderrBytes':
        return bytes[StreamType.STDERR]

      case 'exitValue':
      case "stdoutStream":
      case "stderrStream":
      case "exitValueStream":
      case 'stream':
        return exitValue

      case 'all':
        return [
          exitValue: exitValue,
          stdout: shell.toStringOutput(bytes[StreamType.STDOUT]),
          stderr: shell.toStringOutput(bytes[StreamType.STDERR])
        ]

      case 'allBytes':
        return [
          exitValue: exitValue,
          stdout: bytes[StreamType.STDOUT],
          stderr: bytes[StreamType.STDERR]
        ]

      default:
        throw new IllegalArgumentException("unknown [${args.res}] res value")
    }
  }

  private void initCommandLine()
  {
    _commandLine = shell.toStringCommandLine(args.command)

    if(_commandLine.startsWith('file:'))
    {
      _commandLine -= 'file:'
    }
  }

  private void initOutput(StreamType streamType, String argName = streamType.name().toLowerCase())
  {
    def output = args."${argName}"

    def map = [stream: NullOutputStream.INSTANCE]

    _processIO[streamType] = map

    if(output != null)
    {
      if(_requiredRes == "stream" || _requiredRes == "${argName}Stream")
        throw new IllegalArgumentException("args.${argName}=[${output}] incompatible with arg.res=[${_requiredRes}]")

      map.stream = output
    }
    else
    {
      if(_requiredRes == "stream" || _requiredRes == "${argName}Stream")
        map.stream = null
      else
      {
        if(_requiredRes == argName ||
           _requiredRes == "${argName}Bytes" ||
           _requiredRes == 'all' ||
           _requiredRes == 'allBytes')
        {
          map.stream = new ByteArrayOutputStream()
          map.streamOfBytes = map.stream
        }
      }
    }
  }

  private void startOutputThread(StreamType streamType, Closure inputStreamProvider)
  {
    def stream = _processIO[streamType]?.stream

    if(stream != null)
    {
      def consumeStream = {
        inputStreamProvider().withStream { InputStream inputStream ->
          // if it is a closure, invoke the closure
          if(stream instanceof Closure)
          {
            stream(inputStream)
          }
          else
          {
            // read stdout
            new BufferedInputStream(inputStream).withStream {
              stream << it
            }
          }
        }
      }

      // need to consume output (in a separate thread!)
      def future = new FutureTaskExecution(consumeStream)
      future.description = "${_commandLine} > ${streamType.name().toLowerCase()}"
      _processIO[streamType].future = future
      future.runAsync(shell.executorService)
    }
  }
}