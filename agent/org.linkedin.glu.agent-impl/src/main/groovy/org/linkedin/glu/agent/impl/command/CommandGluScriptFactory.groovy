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

package org.linkedin.glu.agent.impl.command

import org.linkedin.glu.agent.impl.script.ScriptFactory
import org.linkedin.glu.agent.impl.script.ScriptConfig
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.commands.impl.CommandExecutionIOStorage

/**
 * @author yan@pongasoft.com */
public class CommandGluScriptFactory implements ScriptFactory
{
  @Initializable(required = true)
  CommandExecutionIOStorage ioStorage

  @Override
  def createScript(ScriptConfig scriptConfig)
  {
    new CommandGluScript(ioStorage: ioStorage)
  }

  @Override
  def toExternalRepresentation()
  {
    return ['class': 'CommandGluScriptFactory']
  }

  @Override
  public String toString()
  {
    return "CommandGluScriptFactory"
  }

  boolean equals(o)
  {
    if(this.is(o)) return true
    if(getClass() != o.class) return false

    CommandGluScriptFactory that = (CommandGluScriptFactory) o

    if(ioStorage != that.ioStorage) return false

    return true
  }

  int hashCode()
  {
    return (ioStorage != null ? ioStorage.hashCode() : 0)
  }
}