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

import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution
import java.util.concurrent.ExecutorService

/**
 * @author yan@pongasoft.com */
public interface CommandStreamStorage
{
  // output
  OutputStream findStorageOutput(StreamType streamType)
  def withStorageOutput(StreamType streamType, Closure c)

  // input
  InputStream findStorageInput(StreamType streamType)
  def withStorageInput(StreamType streamType, Closure c)

  /**
   * @return map with <code>stream</code> and <code>size</code> or <code>null</code>
   */
  def findStorageInputWithSize(StreamType streamType)

  /**
   * Return the stream for the command.
   *
   * @param args.offset where to start in the stream (optional, <code>int</code>,
   *                    <code>0</code> by default)
   * @param args.len how many bytes to read maximum (optional, <code>int</code>,
   *                 <code>-1</code> by default which means read all)
   * @return a map with <code>stream</code> and <code>size</code> or <code>null</code>
   */
  def findStorageInputWithSize(StreamType streamType, def args)

  /**
   * Same as {@link #findStorageInputWithSize} but call the closure with the map (and make sure
   * that the stream gets closed properly). This version does not call the closure if
   * there is no such stream
   *
   * @param args.offset where to start in the stream (optional, <code>int</code>,
   *                    <code>0</code> by default)
   * @param args.len how many bytes to read maximum (optional, <code>int</code>,
   *                 <code>-1</code> by default which means read all)
   * @return whatever the closure returns
   */
  def withStorageInputWithSize(StreamType streamType, def args, Closure closure)

  /**
   * Same as {@link #findStorageInputWithSize} but call the closure with the map (and make sure
   * that the stream gets closed properly). This version always calls the closure, potentially
   * with <code>null</code>
   *
   * @param args.offset where to start in the stream (optional, <code>int</code>,
   *                    <code>0</code> by default)
   * @param args.len how many bytes to read maximum (optional, <code>int</code>,
   *                 <code>-1</code> by default which means read all)
   * @return whatever the closure returns
   */
  def withOrWithoutStorageInputWithSize(StreamType streamType, def args, Closure closure)

  /**
   * This method allows you to start streaming the results (stdout, stderr,...) while the command
   * is still running. If you request more than 1 stream, you will get a
   * <code>MultiplexedInputStream</code>.
   *
   * @param args.exitValueStream if you want the exit value to be part of the stream
   *                             (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.exitValueStreamTimeout how long to wait to get the exit value if the command is
   *                                    not completed yet (optional, in the event that
   *                                    <code>exitValueStream</code> is set to
   *                                    <code>true</code> and <code>exitValueStreamTimeout</code>
   *                                    is not provided, it will not block and return no exit value
   *                                    stream)
   * @param args.stdinStream if you want stdin to be part of the stream
   *                         (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.stdinOffset where to start in the stdin stream (optional, <code>int</code>,
   *                          <code>0</code> by default)
   * @param args.stdinLen how many bytes to read maximum (optional, <code>int</code>,
   *                       <code>-1</code> by default which means read all)
   * @param args.stdoutStream if you want stdout to be part of the stream
   *                          (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.stdoutOffset where to start in the stdout stream (optional, <code>int</code>,
   *                          <code>0</code> by default)
   * @param args.stdoutLen how many bytes to read maximum (optional, <code>int</code>,
   *                       <code>-1</code> by default which means read all)
   * @param args.stderrStream if you want stdout to be part of the stream
   *                          (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.stderrOffset where to start in the stdout stream (optional, <code>int</code>,
   *                          <code>0</code> by default)
   * @param args.stderrLen how many bytes to read maximum (optional, <code>int</code>,
   *                       <code>-1</code> by default which means read all)
   * @return a <code>stream</code> or <code>null</code>
   */
  InputStream findStorageInput(def args)

  /**
   * Similar to {@link #findStorageInput} but calls the closure with the stream
   * or with <code>null</code>
   * @return whatever the closure returns
   */
  def withOrWithoutStorageInput(def args, Closure closure)

  /**
   * Synchronously executes the command
   * @return the exitValue of the command execution
   */
  def syncCaptureIO(Closure closure)

  /**
   * Asynchronously executes the command. Returns right away
   */
  FutureTaskExecution asyncCaptureIO(ExecutorService executorService,
                                     Closure closure)
}