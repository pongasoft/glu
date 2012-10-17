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
   * Calls the closure back with stdin stream (and the size of it as a second argument).
   * Calls the closure with <code>(null, null)</code> when no such stream
   * @return whatever the closure returns
   */
  def withStdinInputStream(CommandExecution commandExecution, Closure closure)

  /**
   * Calls the closure back with the result input stream (and the size of it as a second argument).
   * Calls the closure with <code>(null, null)</code> when no such stream
   * @return whatever the closure returns
   */
  def withResultInputStream(CommandExecution commandExecution, Closure closure)

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
