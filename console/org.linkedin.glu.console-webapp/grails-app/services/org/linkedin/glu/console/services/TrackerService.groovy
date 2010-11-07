/*
 * Copyright 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.console.services

import org.linkedin.glu.agent.tracker.AgentsTracker
import org.springframework.beans.factory.DisposableBean
import org.linkedin.glu.agent.tracker.AgentsTrackerImpl
import org.linkedin.glu.agent.tracker.TrackerEventsListener
import org.apache.zookeeper.WatchedEvent
import org.linkedin.glu.console.domain.Fabric
import org.linkedin.zookeeper.tracker.NodeEventType
import org.linkedin.zookeeper.tracker.ErrorListener
import org.linkedin.util.clock.Chronos
import java.util.concurrent.TimeoutException

/**
 * @author ypujante
 */
class TrackerService implements DisposableBean
{
  public static final String MODULE = TrackerService.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  boolean transactional = false

  private final def _trackers = [:]

  def getAgentInfos(Fabric fabric)
  {
    return getAgentsTrackerByFabric(fabric).getAgentInfos()
  }

  def getAgentInfo(Fabric fabric, String agentName)
  {
    return getAgentsTrackerByFabric(fabric).getAgentInfo(agentName)
  }

  def getAllInfosWithAccuracy(Fabric fabric)
  {
    return getAgentsTrackerByFabric(fabric).getAllInfosWithAccuracy()
  }

  def getMountPointInfos(Fabric fabric, String agentName)
  {
    return getAgentsTrackerByFabric(fabric).getMountPointInfos(agentName)
  }

  def getMountPointInfo(Fabric fabric, String agentName, mountPoint)
  {
    return getAgentsTrackerByFabric(fabric).getMountPointInfo(agentName, mountPoint)
  }

  private synchronized AgentsTracker getAgentsTrackerByFabric(Fabric fabric)
  {
    def fabricName = fabric.name
    AgentsTracker tracker = _trackers[fabricName]?.tracker

    // we make sure that the fabric has not changed otherwise we need to change the tracker
    if(tracker && _trackers[fabricName]?.fabric != fabric)
    {
      tracker.destroy()
      tracker = null
      _trackers[fabricName] = null
    }

    if(!tracker)
    {
      if(fabric)
      {
        tracker = new AgentsTrackerImpl(fabric.zkClient, "/org/glu/agents/fabrics/${fabricName}".toString())

        _trackers[fabricName] = [tracker: tracker, fabric: fabric]

        def eventsListener = { events ->
          events.each { event ->
            switch(event.eventType)
            {
              case NodeEventType.ADDED:
                if(log.isDebugEnabled())
                  log.debug "added ${event.nodeInfo.agentName} to fabric ${fabricName}"
                break

              case NodeEventType.DELETED:
              if(log.isDebugEnabled())
                log.debug "removed ${event.nodeInfo.agentName} from fabric ${fabricName}"
              break
            }
          }
        }
        tracker.registerAgentListener(eventsListener as TrackerEventsListener)

        def errorListener = { WatchedEvent event, Throwable throwable ->
          log.warn("Error detected in agent with ${event}", throwable)
        }

        tracker.registerErrorListener(errorListener as ErrorListener)

        tracker.start()

        def timeout = '10s'
        try
        {
          Chronos c = new Chronos()
          log.info "Waiting for tracker ${fabricName} to start for ${timeout}..."
          tracker.waitForStart(timeout)
          log.info "Tracker for ${fabricName} successfully started in ${c.elapsedTimeAsHMS}"
        }
        catch(TimeoutException e)
        {
          log.warn "Tracker for ${fabricName} still not started after ${timeout}... continuing..."
        }

        if(log.isDebugEnabled())
          log.debug "Added tracker for ${fabricName}"
      }
      else
      {
        throw new IllegalArgumentException("unknown fabric ${fabricName}".toString())
      }
    }

    return tracker
  }

  public synchronized void destroy()
  {
    _trackers.values().each { map ->
      map.tracker.destory()
    }
  }
}
