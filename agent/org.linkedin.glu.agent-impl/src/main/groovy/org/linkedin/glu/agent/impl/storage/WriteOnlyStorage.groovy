/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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


package org.linkedin.glu.agent.impl.storage

import org.linkedin.glu.agent.api.MountPoint

/**
 * Represent the write only part of the storage
 *
 * @author ypujante@linkedin.com
 */
interface WriteOnlyStorage
{
  void storeState(MountPoint mountPoint, state)
  void clearState(MountPoint mountPoint)
  void clearAllStates()

  /**
   * Invalidates the state of the given mountPoint (as it has been detected to be "bad")
   *
   * @return the location of where the invalidated state has been moved to (or <code>null</code> if
   *         it has been deleted from the storage entirely)
   */
  def invalidateState(MountPoint mountPoint)

  AgentProperties saveAgentProperties(AgentProperties agentProperties)
  AgentProperties updateAgentProperty(String name, String value)
}
