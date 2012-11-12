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

import org.linkedin.glu.orchestration.engine.fabric.Fabric

/**
 * @author yan@pongasoft.com */
public interface CommandsService
{
  /**
   * Executes the shell command. Note that this is a non blocking call which returns right away
   * with the id of the command being executed
   *
   * @param args see org.linkedin.glu.agent.api.Agent#executeShellCommand for details on args
   * @return the id of the executed command
   */
  String executeShellCommand(Fabric fabric, String agentName, args)

  /**
   * Executes the shell command. Note that this is a blocking call and the
   * <code>commandResultProcessor</code> will be called with a map (<code>command</code>,
   * <code>stream</code>). The <code>stream</code> should be properly read and closed!
   *
   * @return the exit value of the shell command
   */
  def executeShellCommand(Fabric fabric, String agentName, args, Closure commandResultProcessor)

  /**
   * @param closure will be called back with a map with <code>commandExecution</code>
   *        and <code>stream</code> (if any)
   * @throws NoSuchCommandExecutionException if there is no such command
   */
  def withCommandExecutionAndWithOrWithoutStreams(Fabric fabric,
                                                  String commandId,
                                                  def args,
                                                  Closure closure)
    throws NoSuchCommandExecutionException

  /**
   * @return a map with all currently running commands
   */
  Map<String, DbCommandExecution> findCurrentCommandExecutions()

  /**
   * @return a map with all currently running commands (filtered by the commandIds)
   */
  Map<String, DbCommandExecution> findCurrentCommandExecutions(Collection<String> commandIds)

  DbCommandExecution findCommandExecution(Fabric fabric, String commandId)

  Map findCommandExecutions(Fabric fabric, String agentName, def params)

  /**
   * Interrupts the command.
   *
   * @param args.id the id of the command to interrupt
   * @return <code>true</code> if the command was interrupted properly or <code>false</code> if
   * there was no such command or already completed
   */
  boolean interruptCommand(Fabric fabric,
                           String agentName,
                           String commandId)
}
