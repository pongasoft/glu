/*
 * Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.agent.api

import org.slf4j.Logger

/**
 * All glu script will at runtime implement this interface. This essentially serves as documentation
 * in order to know what is available when you write a glu script.
 *
 * @author yan@pongasoft.com */
public interface GluScript
{
  /**
   * @return the mountPoint on which the script was installed. In general, this property is used
   *         to install the application in a unique location (since the mountPoint is unique)
   */
  MountPoint getMountPoint()

  /**
   * @return the shell for (unix) shell like capabilities
   * @see Shell
   */
  Shell getShell()

  /**
   * @return the initParameters provided at installation time
   */
  Map getParams()

  /**
   * @return a logger to log information (in the agent log file)
   */
  Logger getLog()

  /**
   * @return the state manager to be able to get information about the state
   * @see StateManager
   */
  StateManager getStateManager()

  /**
   * Shortcut to <code>getStateManager().state</code>
   */
  def getState()

  /**
   * @return the timers object allows you to schedule and cancel timers
   * @see Timers
   */
  Timers getTimers()
}