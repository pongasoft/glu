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

/**
 * This class encapsulates the storage of IO for commands, this way it is easy to "disable" the
 * storage.
 *
 * @author yan@pongasoft.com */
public interface CommandExecutionIOStorage
{
  def findCommandNodeAndStreams(String commandId, def args)

  /**
   * @return the command node of a previously run/stored command
   */
  CommandNode findCommandNode(String commandId)

  CommandNodeWithStorage createStorageForCommand(def args)
}
