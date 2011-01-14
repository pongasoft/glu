/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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


package org.linkedin.glu.agent.tracker

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.util.lifecycle.Startable
import org.linkedin.util.lifecycle.Destroyable
import org.linkedin.zookeeper.tracker.ErrorListener

/**
 * Tracks the agents (through zookeeper).
 *
 * @author ypujante@linkedin.com
 */
interface AgentsTracker extends Startable, Destroyable
{
  static final String ZK_AGENTS_INSTANCES =  'instances'
  static final String ZK_AGENTS_STATE     =  'state'

  enum AccuracyLevel
  {
    INACCURATE,
    ACCURATE,
    PARTIAL
  }

  /**
   * Registers an event listener on agents
   */
  void registerAgentListener(TrackerEventsListener<AgentInfo, NodeEvent<AgentInfo>> listener)

  /**
   * Registers an event listener on mount points
   */
  void registerMountPointListener(TrackerEventsListener<MountPointInfo, NodeEvent<MountPointInfo>> listener)

  /**
   * listener for handling errors 
   */
  void registerErrorListener(ErrorListener errorListener)

  /**
   * Returns all agent infos
   */
  Map<String, AgentInfo> getAgentInfos()

  /**
   * Returns info about the specified agent
   */
  AgentInfo getAgentInfo(String agentName)

  /**
   * Get all mount points for the given agent
   */
  Map<MountPoint, MountPointInfo> getMountPointInfos(String agentName)

  /**
   * Get a single mount point info
   */
  MountPointInfo getMountPointInfo(String agentName, mountPoint)

  /**
   * @return all the mountpoints
   */
  Map<String, Map<MountPoint, MountPointInfo>> getMountPointInfos()

  /**
   * @return a map [accuracy: _accuracyLevel_, allInfos: [_agentName_: [agent: _agentInfo_, mountPoints: _mountPointInfos_]]
   */
  def getAllInfosWithAccuracy()
}
