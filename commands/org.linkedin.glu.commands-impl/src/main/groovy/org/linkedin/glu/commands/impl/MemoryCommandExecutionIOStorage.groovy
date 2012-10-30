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

/**
 * @author yan@pongasoft.com */
public class MemoryCommandExecutionIOStorage extends AbstractCommandExecutionIOStorage
{
  public static final String MODULE = MemoryCommandExecutionIOStorage.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  final Map<String, MemoryStreamStorage> commands = [:]

  private static class MemoryStreamStorage implements CommandStreamStorage
  {
    ByteArrayOutputStream stdin = new ByteArrayOutputStream()
    ByteArrayOutputStream stdout = new ByteArrayOutputStream()
    ByteArrayOutputStream stderr
    CommandExecution commandExecution

    @Override
    OutputStream findStdinStorage() { stdin }

    @Override
    OutputStream findStdoutStorage() { stdout }

    @Override
    OutputStream findStderrStorage() { stderr = new ByteArrayOutputStream(); return stderr }
  }

  byte[] findBytes(String commandId, StreamType streamType)
  {
    synchronized(commands)
    {
      commands[commandId]?."${streamType.toString().toLowerCase()}"?.toByteArray()
    }
  }

  @Override
  CommandExecution findCommandExecution(String commandId)
  {
    synchronized(commands)
    {
      commands[commandId]?.commandExecution
    }
  }

  @Override
  protected findInputStreamWithSize(String commandId, StreamType streamType)
  {
    def bytes = findBytes(commandId, streamType)
    if(bytes == null)
      return null
    return [stream: new ByteArrayInputStream(bytes), size: bytes.size()]
  }

  @Override
  protected CommandExecution saveCommandExecution(CommandExecution commandExecution)
  {
    synchronized(commands)
    {
      if(commands.containsKey(commandExecution.id))
        throw new IllegalArgumentException("duplicate command id [${commandExecution.id}]")

      commands[commandExecution.id] = new MemoryStreamStorage(commandExecution: commandExecution)
    }

    return commandExecution
  }

  def captureIO(CommandExecution commandExecution, Closure closure)
  {
    CommandStreamStorage streamStorage

    synchronized(commands)
    {
      streamStorage = commands[commandExecution.id]
    }

    if(streamStorage == null || !streamStorage.commandExecution.is(commandExecution))
      throw new IllegalArgumentException("not the same command node...")

    return closure(streamStorage)
  }
}