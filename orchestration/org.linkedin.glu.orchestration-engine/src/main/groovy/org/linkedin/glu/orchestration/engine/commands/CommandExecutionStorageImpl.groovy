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

import org.linkedin.glu.orchestration.engine.commands.CommandExecution.CommandType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils

/**
 * @author yan@pongasoft.com */
public class CommandExecutionStorageImpl implements CommandExecutionStorage
{
  public static final String MODULE = CommandExecutionStorageImpl.class.getName ();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable
  int maxResults = 25

  @Override
  CommandExecution startExecution(String fabric,
                                  String agent,
                                  String username,
                                  String command,
                                  boolean redirectStderr,
                                  String commandId,
                                  CommandType commandType,
                                  long startTime)
  {
    CommandExecution.withTransaction {
      CommandExecution res = new CommandExecution(fabric: fabric,
                                                  agent: agent,
                                                  username: username,
                                                  command: command,
                                                  redirectStderr: redirectStderr,
                                                  commandId: commandId,
                                                  commandType: commandType,
                                                  startTime: startTime)

      if(!res.save())
        throw new Exception("cannot save command execution ${commandId}: ${res.errors}")
      
      return res
    }
  }

  @Override
  CommandExecution endExecution(String commandId,
                                long endTime,
                                byte[] stdinFirstBtes,
                                Long stdinTotalBytesCount,
                                byte[] stdoutFirstBytes,
                                Long stdoutTotalBytesCount,
                                byte[] stderrFirstBytes,
                                Long stderrTotalBytesCount,
                                String exitValue)
  {
    CommandExecution.withTransaction {
      CommandExecution execution = CommandExecution.findByCommandId(commandId)
      if(!execution)
      {
        log.warn("could not find command execution ${commandId}")
      }
      else
      {
        execution.endTime = endTime
        execution.stdinFirstBytes = stdinFirstBtes
        execution.stdinTotalBytesCount = stdinTotalBytesCount
        execution.stdoutFirstBytes = stdoutFirstBytes
        execution.stdoutTotalBytesCount = stdoutTotalBytesCount
        execution.stderrFirstBytes = stderrFirstBytes
        execution.stderrTotalBytesCount = stderrTotalBytesCount
        execution.exitValue = exitValue

        if(!execution.save())
        {
          log.warn("could not save command execution ${commandId}")
        }
      }
      return execution
    }
  }

  @Override
  CommandExecution findCommandExecution(String fabric, String commandId)
  {
    CommandExecution.findByCommandIdAndFabric(commandId, fabric)
  }

  @Override
  Map findCommandExecutions(String fabric, String agent, def params)
  {
    params = GluGroovyCollectionUtils.subMap(params, ['offset', 'max', 'sort', 'order'])

    if(params.offset == null)
      params.offset = 0
    params.max = Math.min(params.max ? params.max.toInteger() : maxResults, maxResults)
    params.sort = params.sort ?: 'startTime'
    params.order = params.order ?: 'desc'

    def ces
    def count
    if(agent)
    {
      ces = CommandExecution.findAllByFabricAndAgent(fabric, agent, params)
      count = CommandExecution.countByFabricAndAgent(fabric, agent)
    }
    else
    {
      ces = CommandExecution.findAllByFabric(fabric, params)
      count = CommandExecution.countByFabric(fabric)
    }

    [ commandExecutions: ces, count: count ]
  }
}