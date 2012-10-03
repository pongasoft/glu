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
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.glu.utils.io.NullOutputStream
import org.linkedin.util.io.IOUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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

  private String commandLine
  private InputStream stdin
  private boolean redirectStderr
  private boolean failOnError
  private def requiredRes

  // stdout
  private OutputStream stdout = NullOutputStream.INSTANCE
  private ByteArrayOutputStream stdoutAsStreamOfBytes = null

  // stderr
  private OutputStream stderr = NullOutputStream.INSTANCE
  private ByteArrayOutputStream stderrAsStreamOfBytes = null

  // process
  private boolean destroyProcessInFinally = true
  private Process process

  // threads to read output (asynchronously)
  private Thread stdoutThreadReader = null
  private Thread stderrThreadReader = null

  def exec()
  {
    initCommandLine()

    stdin = shell.toInputStream(args.stdin)

    redirectStderr = GluGroovyLangUtils.getOptionalBoolean(args.redirectStderr, false)
    failOnError = GluGroovyLangUtils.getOptionalBoolean(args.failOnError, true)
    requiredRes = args.res ?: 'stdout'

    // stdout
    initOutput("stdout")

    // stderr
    initOutput("stderr")

    // builds the process
    def pb = new ProcessBuilder(['bash', '-c', commandLine])
    pb.redirectErrorStream(redirectStderr)

    process = pb.start()

    try
    {
      afterProcessStarted()
    }
    finally
    {
      if(destroyProcessInFinally)
        process?.destroy()
    }
  }

  private def afterProcessStarted()
  {
    // stdout
    startOutputThread("stdout") { process.inputStream }

    // when redirecting stderr, there is nothing on stderr... no need to create a thread!
    if(!redirectStderr)
    {
      startOutputThread("stderr") { process.errorStream }
    }

    if(requiredRes == "stdoutStream" || requiredRes == "stderrStream")
    {
      // we can no longer destroy the process in the finally, it will be destroyed when the
      // input stream is closed
      destroyProcessInFinally = false

      // execute the block call asynchronously
      Thread processThread = Thread.start(commandLine, executeBlockingCall)

      InputStream inputStream

      //return the appropriate stream
      if(requiredRes == "stdoutStream")
        inputStream = process.inputStream
      else
        inputStream = process.errorStream

      return new DestroyProcessInputStream(processThread, process, inputStream)
    }
    else
      executeBlockingCall()
  }

  private def executeBlockingCall = {

    // if stdin then provide it to subprocess
    if(stdin)
    {
      def stream = new BufferedOutputStream(process.outputStream)
      IOUtils.copy(new BufferedInputStream(stdin), stream)
      stream.close()
    }

    // make sure that the thread complete properly
    stdoutThreadReader?.join()
    stderrThreadReader?.join()

    // we wait for the process to be done
    int exitValue = process.waitFor()

    byte[] stdoutAsBytes = stdoutAsStreamOfBytes?.toByteArray() ?: new byte[0]
    byte[] stderrAsBytes = stderrAsStreamOfBytes?.toByteArray() ?: new byte[0]

    // handling failOnError flag
    if(failOnError && Execute.isFailure(exitValue))
    {
      if(log.isDebugEnabled())
      {
        log.debug("Error while executing command ${commandLine}: ${exitValue}")
        log.debug("output=${shell.toStringOutput(stdoutAsBytes)}")
        log.debug("error=${shell.toStringOutput(stderrAsBytes)}")
      }

      ShellExecException exception =
      new ShellExecException("Error while executing command ${commandLine}: res=${exitValue} - output=${shell.toLimitedStringOutput(stdoutAsBytes, 512)} - error=${shell.toLimitedStringOutput(stderrAsBytes, 512)}".toString())
      exception.res = exitValue
      exception.output = stdoutAsBytes
      exception.error = stderrAsBytes

      throw exception
    }

    // handling final output
    switch(requiredRes)
    {
      case 'stdout':
        return shell.toStringOutput(stdoutAsBytes)

      case 'stdoutBytes':
        return stdoutAsBytes

      case 'stderr':
        return shell.toStringOutput(stderrAsBytes)

      case 'stderrBytes':
        return stderrAsBytes

      case 'exitValue':
        return exitValue

      case 'all':
        return [
          exitValue: exitValue,
          stdout: shell.toStringOutput(stdoutAsBytes),
          stderr: shell.toStringOutput(stderrAsBytes)
        ]

      case 'allBytes':
        return [
          exitValue: exitValue,
          stdout: stdoutAsBytes,
          stderr: stderrAsBytes
        ]

      case 'stdoutStream':
      case 'stderrStream':
        // already handled
        return null

      default:
        throw new IllegalArgumentException("unknown [${args.res}] res value")
    }
  }

  private void initCommandLine()
  {
    commandLine = shell.toStringCommandLine(args.command)

    if(commandLine.startsWith('file:'))
    {
      commandLine -= 'file:'
    }
  }

  private void initOutput(String outputName)
  {
    def output = args."${outputName}"

    if(output instanceof OutputStream)
      this."${outputName}" = output
    else
    {
      if(output instanceof Closure || requiredRes == "${outputName}Stream")
        this."${outputName}" = null
      else
      {
        if(requiredRes == outputName ||
           requiredRes == "${outputName}Bytes".toString() ||
           requiredRes == 'all' ||
           requiredRes == 'allBytes')
        {
          this."${outputName}AsStreamOfBytes" = new ByteArrayOutputStream()
          this."${outputName}" = this."${outputName}AsStreamOfBytes"
        }
      }
    }
  }

  private void startOutputThread(String outputName, Closure inputStreamProvider)
  {
    if(requiredRes != "${outputName}Stream")
    {
      // need to read output (in a separate thread!)
      this."${outputName}ThreadReader" = Thread.start("${commandLine} > ${outputName}") {

        InputStream inputStream = inputStreamProvider()

        // this happens when a closure is provided.. invoke the closure
        if(this."${outputName}" == null)
        {
          args."${outputName}"(inputStream)
        }
        else
        {
          // read stdout
          IOUtils.copy(new BufferedInputStream(inputStream), this."${outputName}")
        }
      }
    }
  }
}