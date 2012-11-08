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

import grails.persistence.Entity
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.util.clock.Timespan

/**
 * @author yan@pongasoft.com */
@Entity
public class DbCommandExecution
{
  public static final String MODULE = DbCommandExecution.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  // for now only SHELL
  enum CommandType
  {
    SHELL
  }

  static constraints = {
    commandId(unique: true, nullable: false, blank: false)
    command(nullable: false, blank: false)
    commandType(nullable: false)
    completionTime(nullable: true)
    fabric(nullable: false, blank: false)
    agent(nullable: false, blank: false)
    username(nullable: false, blank: false)
    stdinFirstBytes(nullable: true, maxSize: 255)
    stdinTotalBytesCount(nullable: true)
    stdoutFirstBytes(nullable: true, maxSize: 255)
    stdoutTotalBytesCount(nullable: true)
    stderrFirstBytes(nullable: true, maxSize: 255)
    stderrTotalBytesCount(nullable: true)
    exitValue(nullable: true)
  }

  /**
   * Id of the command (as returned by the agent)
   */
  String commandId

  /**
   * The command run
   */
  String command

  /**
   * Whether stderr is redirected to stdout (through the flag... not the command)
   */
  boolean redirectStderr

  /**
   * The type of the command
   */
  CommandType commandType

  /**
   * time the command was started
   */
  long startTime

  /**
   * time the command ended
   */
  Long completionTime

  /**
   * @return the duration
   */
  Timespan getDuration()
  {
    if(completionTime)
      return new Timespan(completionTime - startTime)
    else
      return null
  }

  /**
   * The fabric on which the command was run
   */
  String fabric

  /**
   * The agent on which the command was run
   */
  String agent

  /**
   * The user who is running the command
   */
  String username

  /**
   * input provided to the command (first bytes...)
   */
  byte[] stdinFirstBytes

  /**
   * The total number of bytes in stdin
   */
  Long stdinTotalBytesCount

  /**
   * the result (stdout) (first bytes...)
   */
  byte[] stdoutFirstBytes

  /**
   * The total number of bytes in stdout
   */
  Long stdoutTotalBytesCount

  /**
   * The result (stderr) (first bytes...)
   */
  byte[] stderrFirstBytes

  /**
   * The total number of bytes in stderr
   */
  Long stderrTotalBytesCount

  /**
   * The return value of the command
   */
  String exitValue

  byte[] getFirstBytes(def streamType)
  {
    this."${streamType.toString().toLowerCase()}FirstBytes"
  }

  Long getTotalBytesCount(def streamType)
  {
    this."${streamType.toString().toLowerCase()}TotalBytesCount"
  }

  boolean hasMoreBytes(def streamType)
  {
    return getFirstBytes(streamType)?.size() < getTotalBytesCount(streamType)
  }

  boolean isExecuting = false

  /**
   * Means it never completed for some reason (currently not running and exit value was never set!)
   */
  boolean isAborted()
  {
    !isExecuting && exitValue == null
  }

  static transients = [
    'duration', 'executing', 'aborted'
  ]
}