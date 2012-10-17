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

/**
 * @author yan@pongasoft.com */
public class MemoryCommandExecutionIOStorage implements CommandExecutionIOStorage
{
  final def streams = [:]

  private class MemoryStreamStorage implements StreamStorage
  {
    ByteArrayOutputStream stdin = new ByteArrayOutputStream()
    ByteArrayOutputStream result = new ByteArrayOutputStream()
    CommandExecution commandExecution

    @Override
    OutputStream findStdinStorage()
    {
      stdin
    }

    @Override
    OutputStream findResultStreamStorage()
    {
      result
    }
  }

  @Override
  def withStdinInputStream(CommandExecution commandExecution, Closure closure)
  {
    withInputStream(commandExecution, 'stdin', closure)
  }

  @Override
  def withResultInputStream(CommandExecution commandExecution, Closure closure)
  {
    withInputStream(commandExecution, 'result', closure)
  }

  @Override
  def captureIO(Closure closure)
  {
    MemoryStreamStorage storage = new MemoryStreamStorage()
    def res = closure(storage)
    synchronized(streams)
    {
      def commandId = storage.commandExecution.commandId.toString()

      if(streams.containsKey(commandId))
        throw new IllegalArgumentException("duplicate command id [${commandId}]")
      streams[commandId] = [
        stdin: storage.stdin.toByteArray(),
        result: storage.result.toByteArray()
      ]
    }
    return res
  }

  private def withInputStream(CommandExecution commandExecution, String name, Closure closure)
  {
    byte[] bytes
    synchronized(streams)
    {
      bytes = streams[commandExecution.commandId]?."${name}"
    }

    if(bytes != null)
      closure(new ByteArrayInputStream(bytes), bytes.size())
    else
      closure(null, null)
  }
}