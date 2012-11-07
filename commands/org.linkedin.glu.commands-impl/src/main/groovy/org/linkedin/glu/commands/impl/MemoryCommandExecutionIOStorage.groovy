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

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.utils.collections.EvictingWithLRUPolicyMap
import org.linkedin.util.annotations.Initializer

/**
 * This implementation is obviously very ephemeral and should be used carefully
 *
 * @author yan@pongasoft.com */
public class MemoryCommandExecutionIOStorage extends AbstractCommandExecutionIOStorage
{
  public static final String MODULE = MemoryCommandExecutionIOStorage.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Initializable
  int maxNumberOfElements = 25

  /**
   * Completed commands: keeps a maximum number of elements.
   */
  Map<String, CommandExecution> completedCommands =
    new EvictingWithLRUPolicyMap<String, CommandExecution>(maxNumberOfElements)

  /**
   * The commands that are currently executing */
  Map<String, CommandExecution> executingCommands = [:]

  /**
   * For the compile to stop bugging me with commands being non final... */
  private final Object _lock = new Object()

  @Initializer
  void setMaxNumberOfElements(int maxNumberOfElements)
  {
    this.maxNumberOfElements = maxNumberOfElements
    completedCommands = new EvictingWithLRUPolicyMap<String, CommandExecution>(maxNumberOfElements)
  }

  @Override
  CommandExecution findCommandExecution(String commandId)
  {
    CommandExecution commandExecution
    synchronized(_lock)
    {
      commandExecution = executingCommands[commandId]
      if(!commandExecution)
        commandExecution = completedCommands[commandId]
    }
    return commandExecution
  }

  @Override
  protected AbstractCommandStreamStorage saveCommandExecution(CommandExecution commandExecution,
                                                              def stdin)
  {
    synchronized(_lock)
    {
      if(completedCommands.containsKey(commandExecution.id) ||
         executingCommands.containsKey(commandExecution.id))
        throw new IllegalArgumentException("duplicate command id [${commandExecution.id}]")

      def storage = new MemoryStreamStorage(commandExecution: commandExecution)

      if(!commandExecution.redirectStderr)
        storage.stderr = new ByteArrayOutputStream()

      if(stdin)
      {
        storage.stdin = new ByteArrayOutputStream()
        new BufferedInputStream(stdin).withStream { storage.stdin << it }
      }

      return storage
    }
  }

  def captureIO(CommandExecution commandExecution, Closure closure)
  {
    def commandId = commandExecution.id

    synchronized(_lock)
    {
      executingCommands[commandId] = commandExecution
    }

    try
    {
      return closure(commandExecution.storage)
    }
    finally
    {
      synchronized(_lock)
      {
        executingCommands.remove(commandId)
        completedCommands[commandId] = commandExecution
      }
    }
  }
}