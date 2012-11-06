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
   * Keeps a maximum number of elements.
   */
  Map<String, MemoryStreamStorage> commands =
    new EvictingWithLRUPolicyMap<String, MemoryStreamStorage>(maxNumberOfElements)

  /**
   * For the compile to stop bugging me with commands being non final... */
  private final Object _lock = new Object()

  @Initializer
  void setMaxNumberOfElements(int maxNumberOfElements)
  {
    this.maxNumberOfElements = maxNumberOfElements
    commands = new EvictingWithLRUPolicyMap<String, MemoryStreamStorage>(maxNumberOfElements)
  }

  @Override
  CommandExecution findCommandExecution(String commandId)
  {
    synchronized(_lock)
    {
      commands[commandId]?.commandExecution
    }
  }

  @Override
  protected CommandExecution saveCommandExecution(CommandExecution commandExecution, def stdin)
  {
    synchronized(_lock)
    {
      if(commands.containsKey(commandExecution.id))
        throw new IllegalArgumentException("duplicate command id [${commandExecution.id}]")

      def storage = new MemoryStreamStorage(commandExecution: commandExecution)

      if(!commandExecution.redirectStderr)
        storage.stderr = new ByteArrayOutputStream()

      if(stdin)
      {
        storage.stdin = new ByteArrayOutputStream()
        stdin.withStream { storage.stdin << it }
      }

      commands[commandExecution.id] = storage

      return commandExecution
    }
  }

  @Override
  protected findInputStreamWithSize(CommandExecution commandExecution, StreamType streamType)
  {
    synchronized(_lock)
    {
      commands[commandExecution.id]?.findStorageInputWithSize(streamType)
    }
  }

  def captureIO(CommandExecution commandExecution, Closure closure)
  {
    CommandStreamStorage streamStorage

    synchronized(_lock)
    {
      streamStorage = commands[commandExecution.id]
    }

    if(streamStorage == null || !streamStorage.commandExecution.is(commandExecution))
      throw new IllegalArgumentException("not the same command node...")

    return closure(streamStorage)
  }
}