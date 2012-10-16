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
 * This class encapsulates the storage of IO for commands, this way it is easy to "disable" the
 * storage.
 *
 * @author yan@pongasoft.com */
public interface CommandExecutionIOStorage
{
  /**
   * @return <code>null</code> is there is no stdin associated
   */
  InputStream findStdinInputStream(CommandExecution commandExecution)

  /**
   * @return <code>null</code> is there is no IO storage for result
   */
  InputStream findResultInputStream(CommandExecution commandExecution)

  /**
   * Call the closure back with a {@link StreamStorage} instance
   *
   * @return whatever the closure returns
   */
  def captureIO(Closure closure)
}

public interface StreamStorage
{
  void setCommandExecution(CommandExecution commandExecution)
  OutputStream findStdinStorage()
  OutputStream findResultStreamStorage()
}
