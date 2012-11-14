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

import org.linkedin.glu.utils.collections.EvictingWithLRUPolicyMap
import org.linkedin.util.annotations.Initializable
import org.linkedin.util.annotations.Initializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils

/**
 * Mostly used for testing purposes...
 *
 * @author yan@pongasoft.com */
public class MemoryCommandExecutionStorage extends AbstractCommandExecutionStorage
{
  public static final String MODULE = MemoryCommandExecutionStorage.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable
  int maxResults = 25

  private int _maxNumberOfElements = 25

  @Initializable(required=true)
  Map<String, DbCommandExecution> memory =
    new EvictingWithLRUPolicyMap<String, DbCommandExecution>(_maxNumberOfElements)

  int getMaxNumberOfElements()
  {
    return _maxNumberOfElements
  }

  @Initializer
  void setMaxNumberOfElements(int maxNumberOfElements)
  {
    _maxNumberOfElements = maxNumberOfElements
    memory = new EvictingWithLRUPolicyMap<String, DbCommandExecution>(maxNumberOfElements)
  }

  @Override
  synchronized DbCommandExecution startExecution(String fabric,
                                                 String agent,
                                                 String username,
                                                 String command,
                                                 boolean redirectStderr,
                                                 byte[] stdinFirstBytes,
                                                 Long stdinTotalBytesCount,
                                                 String commandId,
                                                 DbCommandExecution.CommandType commandType,
                                                 long startTime)
  {
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

    memory[commandId] = res

    return res
  }

  @Override
  protected synchronized DbCommandExecution doFindByCommandId(String commandId)
  {
    return memory[commandId]
  }

  @Override
  protected synchronized DbCommandExecution doEndExecution(String commandId,
                                                           long endTime,
                                                           byte[] stdoutFirstBytes,
                                                           Long stdoutTotalBytesCount,
                                                           byte[] stderrFirstBytes,
                                                           Long stderrTotalBytesCount,
                                                           String exitValue,
                                                           String exitError)
  {
    return doUpdate(commandId,
                    endTime,
                    stdoutFirstBytes,
                    stdoutTotalBytesCount,
                    stderrFirstBytes,
                    stderrTotalBytesCount,
                    exitValue,
                    exitError)
  }

  @Override
  synchronized DbCommandExecution findCommandExecution(String fabric, String commandId)
  {
    DbCommandExecution execution = memory[commandId]
    if(execution?.fabric == fabric)
      return execution
    else
      return null
  }

  @Override
  synchronized Map findCommandExecutions(String fabric, String agent, def params)
  {
    params = GluGroovyCollectionUtils.subMap(params ?: [:], ['offset', 'max', 'sort', 'order'])

    params.offset = params.offset?.toInteger() ?: 0
    params.max = Math.min(params.max ? params.max.toInteger() : maxResults, maxResults)
    params.sort = params.sort ?: 'startTime'
    params.order = params.order ?: 'desc'

    def ces = memory.values().findAll { DbCommandExecution ce ->
      ce.fabric == fabric && (agent == null || ce.agent == agent)
    }

    def count = ces.size()

    // paginate
    ces = GluGroovyCollectionUtils.paginate(ces, params.max, params.offset)

    return [commandExecutions: ces, count: count]
  }
}