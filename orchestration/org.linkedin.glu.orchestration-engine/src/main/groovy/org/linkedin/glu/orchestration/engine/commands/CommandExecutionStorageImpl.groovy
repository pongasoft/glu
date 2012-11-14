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

import org.linkedin.glu.orchestration.engine.commands.DbCommandExecution.CommandType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils

/**
 * @author yan@pongasoft.com */
public class CommandExecutionStorageImpl extends AbstractCommandExecutionStorage
{
  public static final String MODULE = CommandExecutionStorageImpl.class.getName ();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable
  int maxResults = 25

  @Override
  DbCommandExecution startExecution(String fabric,
                                    String agent,
                                    String username,
                                    String command,
                                    boolean redirectStderr,
                                    byte[] stdinFirstBytes,
                                    Long stdinTotalBytesCount,
                                    String commandId,
                                    CommandType commandType,
                                    long startTime)
  {
    DbCommandExecution.withTransaction {
      DbCommandExecution res = new DbCommandExecution(fabric: fabric,
                                                      agent: agent,
                                                      username: username,
                                                      command: command,
                                                      redirectStderr: redirectStderr,
                                                      stdinFirstBytes: stdinFirstBytes,
                                                      stdinTotalBytesCount: stdinTotalBytesCount,
                                                      commandId: commandId,
                                                      commandType: commandType,
                                                      startTime: startTime)

      if(!res.save())
        throw new Exception("cannot save command execution ${commandId}: ${res.errors}")
      
      return res
    }
  }

  @Override
  protected DbCommandExecution doFindByCommandId(String commandId)
  {
    DbCommandExecution.findByCommandId(commandId)
  }

  @Override
  protected DbCommandExecution doEndExecution(String commandId,
                                              long endTime,
                                              byte[] stdoutFirstBytes,
                                              Long stdoutTotalBytesCount,
                                              byte[] stderrFirstBytes,
                                              Long stderrTotalBytesCount,
                                              String exitValue,
                                              String exitError)
  {
    DbCommandExecution.withTransaction {

      DbCommandExecution execution = doUpdate(commandId,
                                              endTime,
                                              stdoutFirstBytes,
                                              stdoutTotalBytesCount,
                                              stderrFirstBytes,
                                              stderrTotalBytesCount,
                                              exitValue,
                                              exitError)

      if(execution && !execution.save())
        log.warn("could not save command execution ${commandId}")

      return execution
    }
  }

  @Override
  DbCommandExecution findCommandExecution(String fabric, String commandId)
  {
    DbCommandExecution.findByCommandIdAndFabric(commandId, fabric)
  }

  @Override
  Map findCommandExecutions(String fabric, String agent, def params)
  {
    params = GluGroovyCollectionUtils.subMap(params ?: [:], ['offset', 'max', 'sort', 'order'])

    params.offset = params.offset?.toInteger() ?: 0
    params.max = Math.min(params.max ? params.max.toInteger() : maxResults, maxResults)
    params.sort = params.sort ?: 'startTime'
    params.order = params.order ?: 'desc'

    def ces
    def count
    if(agent)
    {
      ces = DbCommandExecution.findAllByFabricAndAgent(fabric, agent, params)
      count = DbCommandExecution.countByFabricAndAgent(fabric, agent)
    }
    else
    {
      ces = DbCommandExecution.findAllByFabric(fabric, params)
      count = DbCommandExecution.countByFabric(fabric)
    }

    [ commandExecutions: ces, count: count ]
  }
}