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

package org.linkedin.glu.groovy.utils.shell

import org.apache.tools.ant.taskdefs.Execute
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution
import org.linkedin.glu.groovy.utils.io.DestroyProcessInputStream
import org.linkedin.glu.groovy.utils.io.InputGeneratorStream
import org.linkedin.glu.groovy.utils.io.StreamType
import org.linkedin.glu.groovy.utils.json.GluGroovyJsonUtils
import org.linkedin.glu.groovy.utils.lang.ProcessWithResult
import org.linkedin.glu.utils.io.EmptyInputStream
import org.linkedin.glu.utils.io.MultiplexedInputStream
import org.linkedin.glu.utils.io.NullOutputStream
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.ExecutionException

/**
 * Because the logic of exec is quite complicated, it requires its own class
 *
 * @author yan@pongasoft.com  */
class ShellExec
{
  public static final String MODULE = ShellExec.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  // the shell which created this class
  ShellImpl shell

  // the args passed to shell.exec
  Map args

  private def _commandLine
  private InputStream _stdin
  private boolean _redirectStderr
  private boolean _failOnError
  private def _requiredRes

  // second map contains: stream, streamOfBytes
  private Map<StreamType, Map> _processIO = [:]

  // process
  private boolean _destroyProcessInFinally = true
  private Process _process

  /**
   * Use bash -c to run the command line
   */
  public static def buildCommandLine(def commandLine)
  {
    if(commandLine instanceof List<String>)
      commandLine
    else
      ['bash', '-c', commandLine.toString()]
  }

  def exec()
  {
    initCommandLine()

    _stdin = shell.toInputStream(args.stdin)

    _redirectStderr = GluGroovyLangUtils.getOptionalBoolean(args.redirectStderr, false)
    _failOnError = GluGroovyLangUtils.getOptionalBoolean(args.failOnError, true)
    _requiredRes = args.res ?: 'stdout'

    // stdout
    initOutput(StreamType.stdout)

    // stderr
    if(!_redirectStderr)
      initOutput(StreamType.stderr)

    // builds the process
    def pb = new ProcessBuilder(buildCommandLine(_commandLine))
    pb.redirectErrorStream(_redirectStderr)

    // pwd
    if(args.pwd)
      pb.directory(args.pwd as File)

    // environment
    Map<String, String> environment = pb.environment()
    if(!GluGroovyLangUtils.getOptionalBoolean(args.inheritEnv, true))
      environment.clear()

    if(args.env)
    {
      args.env.each { k, v ->
        k = k.toString()

        if(v == null)
          environment.remove(k)
        else
          environment[k] = v.toString()
      }
    }

    try
    {
      _process = pb.start()
    }
    catch(IOException e)
    {
      _process = new ProcessWithResult(outputStream: NullOutputStream.INSTANCE,
                                       inputStream: EmptyInputStream.INSTANCE,
                                       errorStream: new InputGeneratorStream(e.message),
                                       exitValue: 2)
    }

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
    startOutputThread(StreamType.stdout) { _process.inputStream }

    // when redirecting stderr, there is nothing on stderr... no need to create a thread!
    if(!_redirectStderr)
    {
      startOutputThread(StreamType.stderr) { _process.errorStream }
    }

    if(_requiredRes.toLowerCase().endsWith("stream"))
      return createRequiredInputStream()
    else
      return executeBlockingCall()
  }

  /**
   * @return always a string
   */
  private String getCommandLineAsString()
  {
    if(_commandLine instanceof Collection)
      _commandLine.join(' ')
    else
      _commandLine.toString()
  }

  /**
   * Creates the appropriate stream but make sure we still destroy the process when the
   * stream closes
   */
  private InputStream createRequiredInputStream()
  {
    // execute the block call asynchronously
    FutureTaskExecution future = new FutureTaskExecution(executeBlockingCall)
    future.description = commandLineAsString

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

    future.runAsync(shell.submitter)

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
    if(_processIO[StreamType.stdout]?.stream == null)
    {
      streams[StreamType.stdout.multiplexName] = _process.inputStream
    }

    // stderr
    if(_processIO[StreamType.stderr]?.stream == null)
    {
      streams[StreamType.stderr.multiplexName] = _process.errorStream
    }

    // exit value (as a stream)
    InputStream exitValueInputStream = new InputGeneratorStream({
      try
      {
        future.get().toString()
      }
      catch(Throwable ignored)
      {
        // ok to ignore... will be part of the exit error stream...
      }
    })
    streams[StreamType.exitValue.multiplexName] = exitValueInputStream

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
    streams[StreamType.exitError.multiplexName] = exitErrorInputStream

    def stream = new MultiplexedInputStream(streams)
    stream.submitter = shell.submitter
    return stream
  }

  private def executeBlockingCall = {

    // if stdin then provide it to subprocess
    _stdin?.withStream { InputStream sis ->
      new BufferedOutputStream(_process.outputStream).withStream { os ->
        os << new BufferedInputStream(sis)
      }
    }

    // make sure that the thread complete properly
    [StreamType.stdout, StreamType.stderr].each {
      _processIO[it]?.future?.get()
    }

    // we wait for the process to be done
    int exitValue = _process.waitFor()

    Map<StreamType, byte[]> bytes =
      GroovyCollectionsUtils.toMapKey([StreamType.stdout, StreamType.stderr]) {
        _processIO[it]?.streamOfBytes?.toByteArray() ?: new byte[0]
      }

    // handling failOnError flag
    if(_failOnError && Execute.isFailure(exitValue))
    {
      if(log.isDebugEnabled())
      {
        log.debug("Error while executing command ${commandLineAsString}: ${exitValue}")
        log.debug("output=${shell.toStringOutput(bytes[StreamType.stdout])}")
        log.debug("error=${shell.toStringOutput(bytes[StreamType.stderr])}")
      }

      ShellExecException exception =
      new ShellExecException("Error while executing command ${commandLineAsString}: res=${exitValue} - output=${shell.toLimitedStringOutput(bytes[StreamType.stdout], 512)} - error=${shell.toLimitedStringOutput(bytes[StreamType.stderr], 512)}".toString())
      exception.res = exitValue
      exception.output = bytes[StreamType.stdout]
      exception.error = bytes[StreamType.stderr]

      throw exception
    }

    // handling final output
    switch(_requiredRes)
    {
      case 'stdout':
        return shell.toStringOutput(bytes[StreamType.stdout])

      case 'stdoutBytes':
        return bytes[StreamType.stdout]

      case 'stderr':
        return shell.toStringOutput(bytes[StreamType.stderr])

      case 'stderrBytes':
        return bytes[StreamType.stderr]

      case 'exitValue':
      case "stdoutStream":
      case "stderrStream":
      case "exitValueStream":
      case 'stream':
        return exitValue

      case 'all':
        return [
          exitValue: exitValue,
          stdout: shell.toStringOutput(bytes[StreamType.stdout]),
          stderr: shell.toStringOutput(bytes[StreamType.stderr])
        ]

      case 'allBytes':
        return [
          exitValue: exitValue,
          stdout: bytes[StreamType.stdout],
          stderr: bytes[StreamType.stderr]
        ]

      default:
        throw new IllegalArgumentException("unknown [${args.res}] res value")
    }
  }

  private void initCommandLine()
  {
    def command = args.command

    if(command instanceof GString)
      command = command.toString()

    if(!(command instanceof String))
    {
     // clone and make sure it is a collection of strings
     command = command.findAll { it != null }.collect { it.toString() }
      if(command.size() > 0 && command[0].toString().startsWith('file:'))
        command[0] -= 'file:'
    }
    else
    {
      if(command.startsWith('file:'))
        command -= 'file:'
    }

    _commandLine = command
  }

  private void initOutput(StreamType streamType, String argName = streamType.name())
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
      future.description = "${commandLineAsString} > ${streamType.name()}"
      _processIO[streamType].future = future
      future.runAsync(shell.submitter)
    }
  }
}