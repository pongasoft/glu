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

import org.linkedin.glu.groovy.utils.io.DestroyedProcessException

/**
 * @author yan@pongasoft.com */
public class ShellGluCommand
{
  private volatile InputStream exitValueStream
  private volatile boolean _destroyed = false

  def run = { args ->

    if(_destroyed)
      return null

    def stream = shell.exec(*: args, failOnError: false, res: "exitValueStream")

    synchronized(this)
    {
      if(!_destroyed)
        exitValueStream = stream
      else
      {
        stream.destroy()
        return null
      }
    }

    try
    {
      exitValueStream.text as int
    }
    catch(DestroyedProcessException e)
    {
      // ok it was destroyed...
      return null
    }
    finally
    {
      exitValueStream = null
    }
  }

  def destroy = {
    synchronized(this)
    {
      _destroyed = true
      exitValueStream?.destroy()
    }
  }
}