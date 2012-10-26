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

package org.linkedin.glu.agent.impl.command

import org.linkedin.glu.agent.api.AgentException

/**
 * @author yan@pongasoft.com */
public interface CommandManager
{
  /**
   * TODO HIGH YP
   */
  CommandNode executeShellCommand(def args) throws AgentException

  /**
   * @param args.exitValue if you want the exit value to be part of the stream
   *                       (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.timeout how long to wait to get the exit value if the command is not completed yet
   *                     (optional, in the event that <code>arg.exitValue</code> is set to
   *                      <code>true</code> and timeout is not provided, it will not block and
   *                      return no <code>exitValue</code>)
   * @param args.stdin if you want stdin to be part of the stream
   *                    (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.stdinOffset where to start in the stdin stream (optional, <code>int</code>,
   *                          <code>0</code> by default)
   * @param args.stdinLen how many bytes to read maximum (optional, <code>int</code>,
   *                       <code>-1</code> by default which means read all)
   * @param args.stdout if you want stdout to be part of the stream
   *                    (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.stdoutOffset where to start in the stdout stream (optional, <code>int</code>,
   *                          <code>0</code> by default)
   * @param args.stdoutLen how many bytes to read maximum (optional, <code>int</code>,
   *                       <code>-1</code> by default which means read all)
   * @param args.stderr if you want stdout to be part of the stream
   *                    (<code>boolean</code>, optional, <code>false</code> by default)
   * @param args.stderrOffset where to start in the stdout stream (optional, <code>int</code>,
   *                          <code>0</code> by default)
   * @param args.stderrLen how many bytes to read maximum (optional, <code>int</code>,
   *                       <code>-1</code> by default which means read all)
   * @return a map with <code>commandNode</code> and <code>stream</code> or <code>null</code> if
   *         not found
   */
  def findCommandNodeAndStreams(def args) throws AgentException

  /**
   * TODO HIGH YP
   */
  def waitForCommand(def args) throws AgentException

  boolean interruptCommand(def args) throws AgentException
}