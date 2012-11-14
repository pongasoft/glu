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

import org.linkedin.glu.groovy.utils.json.GluGroovyJsonUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author yan@pongasoft.com */
public abstract class AbstractCommandExecutionStorage implements CommandExecutionStorage
{
  public static final String MODULE = AbstractCommandExecutionStorage.class.getName ();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Override
  DbCommandExecution endExecution(String commandId,
                                  long endTime,
                                  byte[] stdoutFirstBytes,
                                  Long stdoutTotalBytesCount,
                                  byte[] stderrFirstBytes,
                                  Long stderrTotalBytesCount,
                                  String exitValueOrError,
                                  boolean isError)
  {
    doEndExecution(commandId,
                   endTime,
                   stdoutFirstBytes,
                   stdoutTotalBytesCount,
                   stderrFirstBytes,
                   stderrTotalBytesCount,
                   isError ? null : exitValueOrError,
                   isError ? exitValueOrError : null)
  }

  protected abstract DbCommandExecution doEndExecution(String commandId,
                                                       long endTime,
                                                       byte[] stdoutFirstBytes,
                                                       Long stdoutTotalBytesCount,
                                                       byte[] stderrFirstBytes,
                                                       Long stderrTotalBytesCount,
                                                       String exitValue,
                                                       String exitError)

  protected abstract DbCommandExecution doFindByCommandId(String commandId)

  protected DbCommandExecution doUpdate(String commandId,
                                        long endTime,
                                        byte[] stdoutFirstBytes,
                                        Long stdoutTotalBytesCount,
                                        byte[] stderrFirstBytes,
                                        Long stderrTotalBytesCount,
                                        String exitValue,
                                        String exitError)
  {
    DbCommandExecution execution = doFindByCommandId(commandId)

    if(!execution)
    {
      log.warn("could not find command execution ${commandId}")
    }
    else
    {
      execution.completionTime = endTime
      execution.stdoutFirstBytes = stdoutFirstBytes
      execution.stdoutTotalBytesCount = stdoutTotalBytesCount
      execution.stderrFirstBytes = stderrFirstBytes
      execution.stderrTotalBytesCount = stderrTotalBytesCount
      execution.exitValue = exitValue
      execution.exitError = exitError
    }

    return execution
  }

  @Override
  DbCommandExecution endExecution(String commandId,
                                  long endTime,
                                  byte[] stdoutFirstBytes,
                                  Long stdoutTotalBytesCount,
                                  byte[] stderrFirstBytes,
                                  Long stderrTotalBytesCount,
                                  Throwable exception)
  {
    doEndExecution(commandId,
                   endTime,
                   stdoutFirstBytes,
                   stdoutTotalBytesCount,
                   stderrFirstBytes,
                   stderrTotalBytesCount,
                   null,
                   GluGroovyJsonUtils.exceptionToJSON(exception))
  }
}