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

/**
 * This class encapsulates the storage of IO for commands, this way it is easy to "disable" the
 * storage.
 *
 * @author yan@pongasoft.com */
public interface CommandExecutionIOStorage
{
  /**
   * This method allows you to start streaming the results (stdout, stderr,...) while the command
   * is still running.
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
   * @return a map with <code>commandExecution</code> and <code>stream</code> (if any)
   *         (<code>null</code> if no such command)
   */
  def findCommandExecutionAndStreams(String commandId, def args)

  /**
   * Similar to {@link #findCommandExecutionAndStreams} but calls the closure with the map
   * or with <code>null</code>
   * @return whatever the closure returns
   */
  def withOrWithoutCommandExecutionAndStreams(String commandId, def args, Closure closure)

  /**
   * @return the command of a previously run/stored command
   */
  CommandExecution findCommandExecution(String commandId)

  /**
   * @param gluCommandFactory create storage for command execution
   */
  CommandExecution createStorageForCommandExecution(def args)
}
