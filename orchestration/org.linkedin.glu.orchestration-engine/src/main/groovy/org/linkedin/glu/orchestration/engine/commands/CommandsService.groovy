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

import org.linkedin.glu.orchestration.engine.fabric.Fabric

/**
 * @author yan@pongasoft.com */
public interface CommandsService
{
  /**
   * Executes the shell command. Note that this is a non blocking call which returns right away
   * with the id of the command being executed
   *
   * @param args see org.linkedin.glu.agent.api.Agent#executeShellCommand for details on args
   * @return the id of the executed command
   */
  String executeShellCommand(Fabric fabric, String agentName, args)

  /**
   * Executes the shell command. Note that this is a blocking call and the
   * <code>commandResultProcessor</code> will be called with a map (<code>id</code>,
   * <code>stream</code>). The <code>stream</code> should be properly read and closed!
   *
   * @return whatever <code>commandResultProcessor</code> returns
   */
  def executeShellCommand(Fabric fabric, String agentName, args, Closure commandResultProcessor)
}