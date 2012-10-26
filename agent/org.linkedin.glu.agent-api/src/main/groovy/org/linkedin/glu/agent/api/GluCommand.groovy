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

package org.linkedin.glu.agent.api

import org.slf4j.Logger

/**
 * All glu command will at runtime implement this interface. This essentially serves as documentation
 * in order to know what is available when you write a glu command.
 *
 * @author yan@pongasoft.com */
public interface GluCommand
{
  /**
   * @return the (unique) id of the command
   */
  String getId()

  /**
   * @return the shell for (unix) shell like capabilities
   * @see Shell
   */
  Shell getShell()

  /**
   * @return a logger to log information (in the agent log file)
   */
  Logger getLog()

  /**
   * @return a reference to 'this' script
   */
  GluCommand getSelf()
}