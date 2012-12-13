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


package org.linkedin.glu.agent.tracker

import org.linkedin.glu.agent.api.MountPoint
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.ZooDefs.Ids
import org.linkedin.glu.agent.tracker.AgentsTracker.AccuracyLevel
import org.apache.zookeeper.WatchedEvent
import org.linkedin.zookeeper.client.LifecycleListener
import org.linkedin.zookeeper.tracker.ErrorListener
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.util.clock.Chronos
import java.util.concurrent.TimeoutException
import org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.util.annotations.Initializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.zookeeper.tracker.ZooKeeperTreeTracker
import org.linkedin.zookeeper.tracker.NodeEventsListener
import org.linkedin.zookeeper.tracker.NodeEventType
import org.linkedin.zookeeper.tracker.ZKStringDataReader
import org.apache.zookeeper.KeeperException

/**
 * Tracks the agents (through zookeeper). Note that it has recovery built in!
 *
 * @author ypujante@linkedin.com
 */
class AgentsTrackerImpl implements AgentsTracker, LifecycleListener, ErrorListener
{
  public static final String MODULE = AgentsTrackerImpl.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  enum State
  {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
  }

  @Initializable
  Clock clock = SystemClock.INSTANCE

  @Initializable
  AgentInfoPropertyAccessor agentInfoPropertyAccessor = PrefixAgentInfoPropertyAccessor.DEFAULT

  private final IZKClient _zk
  private final String _zkAgentsInstances
  private final String _zkAgentsState

  private volatile AgentsTrackerInstance _agentsTrackerInstance

  private final def _agentListeners = new HashSet<TrackerEventsListener<AgentInfo, NodeEvent<AgentInfo>>>()
  private final def _mountPointListeners = new HashSet<TrackerEventsListener<MountPointInfo, NodeEvent<MountPointInfo>>>()
  private final def _errorListeners = new HashSet<ErrorListener>()
  private volatile def _destroyed = false
  private volatile State _state = State.DISCONNECTED

  private final Object _lock = new Object()

  AgentsTrackerImpl(IZKClient zk, String zkAgentsInstances, String zkAgentsState)
  {
    _zk = zk
    _zkAgentsInstances = zkAgentsInstances
    _zkAgentsState = zkAgentsState
  }

  AgentsTrackerImpl(IZKClient zk, String zkAgentRoot)
  {
    _zk = zk
    _zkAgentsInstances = "${zkAgentRoot}/${ZK_AGENTS_INSTANCES}".toString()
    _zkAgentsState = "${zkAgentRoot}/${ZK_AGENTS_STATE}".toString()
  }

  boolean isConnected()
  {
    return _state == State.CONNECTED
  }

  State getState()
  {
    return _state
  }

  public void onConnected()
  {
    synchronized(_lock)
    {
      if(_destroyed || _state != State.DISCONNECTED)
        return

      _state = State.CONNECTING
      def agentsTrackerInstance =
        new AgentsTrackerInstance(this,
                                  _zk,
                                  _zkAgentsInstances,
                                  _zkAgentsState,
                                  agentInfoPropertyAccessor)
      _agentListeners.each { agentsTrackerInstance.registerAgentListener(it) }
      _mountPointListeners.each { agentsTrackerInstance.registerMountPointListener(it) }
      _errorListeners.each { agentsTrackerInstance.registerErrorListener(it) }
      agentsTrackerInstance.registerErrorListener(this)
      Chronos c = new Chronos()
      agentsTrackerInstance.track()

      _agentsTrackerInstance?.destroy() // destroy previous instance if there was one...
      _agentsTrackerInstance = agentsTrackerInstance

      _state = State.CONNECTED
      _lock.notifyAll()
      
      log.info "Connected to ZooKeeper in ${c.elapsedTimeAsHMS}... starting to track changes"
    }
  }


  public void onDisconnected()
  {
    synchronized(_lock)
    {
      // note that we do not clear the agents/mountPoints so that they are still available!
      _agentsTrackerInstance?.untrack()

      _state = State.DISCONNECTED
      _lock.notifyAll()

      log.info "Disconnected from ZooKeeper... stopping to track changes"
    }
  }

  void onError(WatchedEvent event, Throwable throwable)
  {
    log.warn("Detected error while processing event: path=${event.path}, type=${event.type}, state=${event.state}... (ignored)",
             throwable)
  }

  public void start()
  {
    synchronized(_lock)
    {
      _zk.registerListener(this)
      _destroyed = false
    }
  }

  /**
   * Wait (no longer than timeout if provided) for the client to be started
   */
  public void waitForStart() throws InterruptedException
  {
    try
    {
      waitForStart(null);
    }
    catch(TimeoutException e)
    {
      // should not happen...
      throw new RuntimeException(e);
    }
  }

  /**
   * Wait (no longer than timeout if provided) for the client to be started
   */
  public void waitForStart(timeout) throws TimeoutException, InterruptedException
  {
    GroovyConcurrentUtils.awaitFor(clock, timeout, _lock) { _state == State.CONNECTED }
  }

  public void destroy()
  {
    synchronized(_lock)
    {
      _destroyed = true
      _zk.removeListener(this)
      _agentsTrackerInstance?.destroy()
    }
  }

  void registerAgentListener(TrackerEventsListener<AgentInfo, NodeEvent<AgentInfo>> listener)
  {
    synchronized(_lock)
    {
      _agentListeners << listener
      _agentsTrackerInstance?.registerAgentListener(listener)
    }
  }

  void registerMountPointListener(TrackerEventsListener<MountPointInfo, NodeEvent<MountPointInfo>> listener)
  {
    synchronized(_lock)
    {
      _mountPointListeners << listener
      _agentsTrackerInstance?.registerMountPointListener(listener)
    }
  }

  synchronized void registerErrorListener(ErrorListener errorListener)
  {
    synchronized(_lock)
    {
      _errorListeners << errorListener
      _agentsTrackerInstance?.registerErrorListener(errorListener)
    }
  }

  /**
   * @return a map [accuracy: _accuracyLevel_, allInfos: [h1: [agent: _agentInfo_, mountPoints: _mountPointInfos_]]
   */
  def getAllInfosWithAccuracy()
  {
    def agentsTrackerInstance = _agentsTrackerInstance
    if(agentsTrackerInstance)
    {
      return agentsTrackerInstance.allInfosWithAccuracy
    }
    else
    {
      return [accuracy: AccuracyLevel.INACCURATE, allInfos: [:]]
    }
  }

  /**
   * Returns all agent infos
   */
  Map<String, AgentInfo> getAgentInfos()
  {
    return _agentsTrackerInstance?.agentInfos ?: [:]
  }

  /**
   * Returns info about the specified agent
   */
  AgentInfo getAgentInfo(String agentName)
  {
    return _agentsTrackerInstance?.getAgentInfo(agentName)
  }

  /**
   * Get all mount points for the given agent
   */
  Map<MountPoint, MountPointInfo> getMountPointInfos(String agentName)
  {
    return _agentsTrackerInstance?.getMountPointInfos(agentName) ?: [:]
  }

  /**
   * Get a single mount point info
   */
  MountPointInfo getMountPointInfo(String agentName, mountPoint)
  {
    return getMountPointInfos(agentName)?.get(MountPoint.create(mountPoint.toString()))
  }

  /**
   * @return all the mountpoints
   */
  Map<String, Map<MountPoint, MountPointInfo>> getMountPointInfos()
  {
    return _agentsTrackerInstance?.getMountPointInfos() ?: [:]
  }

  @Override
  boolean clearAgentInfo(String agentName)
  {
    if(_agentsTrackerInstance?.getAgentInfo(agentName))
      throw new IllegalStateException("agent ${agentName} is still up!")

    try
    {
      // YP implementation note: this call is asynchronous: the tracker will receive events when the delete
      // operation has propagated, or in other words calling 'getMountPointInfos' right after
      // this call may potentially not return an empty map!
      _zk.deleteWithChildren("${_zkAgentsState}/${agentName}")
      return true
    }
    catch (KeeperException.NoNodeException e)
    {
      return false
    }
  }

  /**
   * Returns a factory that will create AgentsTrackerImpl
   */
  public static AgentsTrackerFactory createFactory(IZKClient zk, String zkAgentRoot)
  {
    return {
      new AgentsTrackerImpl(zk, zkAgentRoot)
    } as AgentsTrackerFactory
  }
}

class AgentsTrackerInstance
{
  public static final String MODULE = AgentsTrackerInstance.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);
  public static final Logger agentsLog = LoggerFactory.getLogger("${MODULE}_agents");
  public static final Logger agentsDetailsLog = LoggerFactory.getLogger("${MODULE}_agents_details");
  public static final Logger mountPointsLog = LoggerFactory.getLogger("${MODULE}_mountPoints");
  public static final Logger mountPointsDetailsLog = LoggerFactory.getLogger("${MODULE}_mountPoints_details");

  private final AgentsTrackerImpl _parent
  private final IZKClient _zk
  private final String _zkAgentsInstances
  private final String _zkAgentsState
  private final AgentInfoPropertyAccessor _agentInfoPropertyAccessor

  private ZooKeeperTreeTracker _agentsTracker
  private ZooKeeperTreeTracker _mountPointsTracker

  private volatile Map<String, AgentInfo> _agents = [:]
  private final Map<String, Map<MountPoint, MountPointInfo>> _mountPoints = [:]

  private final def _agentListeners = new HashSet<TrackerEventsListener<AgentInfo, NodeEvent<AgentInfo>>>()
  private final def _mountPointListeners = new HashSet<TrackerEventsListener<MountPointInfo, NodeEvent<MountPointInfo>>>()
  private final def _errorListeners = new HashSet<ErrorListener>()
  private volatile def _stopTracking = false

  private final Object _lock = new Object()

  AgentsTrackerInstance(AgentsTrackerImpl parent,
                        IZKClient zk,
                        String zkAgentsInstances,
                        String zkAgentsState,
                        AgentInfoPropertyAccessor agentInfoPropertyAccessor)
  {
    _parent = parent
    _zk = zk
    _zkAgentsInstances = zkAgentsInstances
    _zkAgentsState = zkAgentsState
    _agentInfoPropertyAccessor = agentInfoPropertyAccessor
  }

  public void track()
  {
    synchronized(_lock)
    {
      _agentsTracker = track(_zkAgentsInstances, agentsListener as NodeEventsListener, 1)
      _mountPointsTracker = track(_zkAgentsState, mountPointsListener as NodeEventsListener, 2)
      _stopTracking = false
    }
  }

  /**
   * Must be called from a synchronized section
   */
  private ZooKeeperTreeTracker track(String path, NodeEventsListener listener, int depth)
  {
    if(!_zk.exists(path))
    {
      _zk.createWithParents(path, null, Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT)
    }

    def tracker = new ZooKeeperTreeTracker(_zk, ZKStringDataReader.INSTANCE, path, depth)
    _errorListeners.each { tracker.registerErrorListener(it) }
    tracker.track(listener)

    return tracker
  }

  void untrack()
  {
    synchronized(_lock)
    {
      _stopTracking = true
      _mountPointsTracker?.destroy()
      _agentsTracker?.destroy()
    }
  }

  void registerAgentListener(TrackerEventsListener<AgentInfo, NodeEvent<AgentInfo>> listener)
  {
    synchronized(_lock)
    {
      _agentListeners << listener
    }
  }

  void registerMountPointListener(TrackerEventsListener<MountPointInfo, NodeEvent<MountPointInfo>> listener)
  {
    synchronized(_lock)
    {
      _mountPointListeners << listener
    }
  }

  synchronized void registerErrorListener(ErrorListener errorListener)
  {
    synchronized(_lock)
    {
      _errorListeners << errorListener
      _agentsTracker?.registerErrorListener(errorListener)
      _mountPointsTracker?.registerErrorListener(errorListener)
    }
  }

  public void destroy()
  {
    synchronized(_lock)
    {
      untrack()
      _agents = [:]
      synchronized(_mountPoints)
      {
        _mountPoints.clear()
      }
      _mountPointsTracker?.destroy()
      _agentsTracker?.destroy()
    }
  }

  /**
   * Returns all agent infos
   */
  Map<String, AgentInfo> getAgentInfos()
  {
    return _agents
  }

  /**
   * Returns info about the specified agent
   */
  AgentInfo getAgentInfo(String agentName)
  {
    return _agents[agentName]
  }

  /**
   */
  long getTransactionId()
  {
    return Math.max(_agentsTracker.lastZkTxId, _mountPointsTracker.lastZkTxId)
  }

  /**
   * @return a map with <code>accuracy</code> and <code>allInfos</code>
   */
  def getAllInfosWithAccuracy()
  {
    def txBefore = transactionId

    def res = [:]

    def agents = getAgentInfos()

    agents.each { agentName, info ->
      def agent = [:]
      agent.info = info
      agent.mountPoints = getMountPointInfos(agentName) ?: [:]
      res[agentName] = agent
    }

    def txAfter = transactionId

    def accuracy = txBefore == txAfter ? AccuracyLevel.ACCURATE : AccuracyLevel.PARTIAL

    if(_stopTracking)
      accuracy = AccuracyLevel.INACCURATE

    return [accuracy: accuracy, allInfos: res]
  }

  /**
   * Get all mount points for the given agent
   */
  Map<MountPoint, MountPointInfo> getMountPointInfos(String agentName)
  {
    // it is ok to return the underlying map as it is *not* modified (it is copied and replaced)
    // outside of this synchronized block
    synchronized(_mountPoints)
    {
      return _mountPoints[agentName]
    }
  }

  /**
   * @return all the mountpoints
   */
  Map<String, Map<MountPoint, MountPointInfo>> getMountPointInfos()
  {
    synchronized(_mountPoints)
    {
      def res = [:]

      // copying the outer map as this one changes...
      _mountPoints.each { k, v -> res[k] = v }

      return res
    }
  }

  /**
   * Handler for children of /glu/agents/instances
   */
  private def agentsListener = { events ->
    if(agentsLog.isDebugEnabled())
      agentsLog.debug "agentsListener: ${eventsLogString(events, true)}"
    if(agentsDetailsLog.isDebugEnabled())
      agentsDetailsLog.debug "agentsListener: ${eventsLogString(events, false)}"

    synchronized(_lock)
    {
      if(_stopTracking)
        return

      def newEvents = []

      def agents = new HashMap(_agents)

      events.each { org.linkedin.zookeeper.tracker.NodeEvent event ->
        // we handle only events for children of instances
        if(event.depth == 1)
        {
          def agentName = event.name.toString()
          AgentInfo info
          switch(event.eventType)
          {
            case NodeEventType.ADDED:
            case NodeEventType.UPDATED:
              info = new AgentInfo(agentInfoPropertyAccessor: _agentInfoPropertyAccessor,
                                   agentName: agentName, 
                                   trackedNode: event.node)
              agents[agentName] = info
              break;

            case NodeEventType.DELETED:
              info = agents.remove(agentName)
              break;
          }
          newEvents << new NodeEvent(nodeInfo: info, eventType: event.eventType)
        }
      }

      _agents = agents

      if(newEvents)
      {
        _agentListeners.each { listener ->
          listener.onEvents(newEvents)
        }
      }
    }
  }

  /**
   * Handler for state of a mountpoint in an agent
   * ex:/glu/agents/state/ypujante-md.linkedin.biz/_container_i001
   */
  private def mountPointsListener = { events ->
    if(mountPointsLog.isDebugEnabled())
      mountPointsLog.debug "mountPointsListener: ${eventsLogString(events, true)}"
    if(mountPointsDetailsLog.isDebugEnabled())
      mountPointsDetailsLog.debug "mountPointsListener: ${eventsLogString(events, false)}"

    synchronized(_lock)
    {
      if(_stopTracking)
        return

      def newEvents = []

      events.each { org.linkedin.zookeeper.tracker.NodeEvent event ->
        // handling only the leaves
        if(event.depth == 2)
        {
          MountPoint mountPoint = MountPoint.fromPathWithNoSlash(event.name)
          // not tracking root.. no real value
          if(mountPoint == MountPoint.ROOT)
            return
          MountPointInfo info
          def agentMountPoints = new HashMap(_mountPoints[event.parentName] ?: [:])
          switch(event.eventType)
          {
            case NodeEventType.ADDED:
            case NodeEventType.UPDATED:
              info = new MountPointInfo(mountPoint: mountPoint,
                                        agentName: event.parentName,
                                        trackedNode: event.node)
              agentMountPoints[mountPoint] = info
              break

            case NodeEventType.DELETED:
              info = agentMountPoints.remove(mountPoint)
              break

            default:
              throw new RuntimeException("not reached [${event.eventType}]")
          }

          synchronized(_mountPoints)
          {
            if(agentMountPoints)
              _mountPoints[info.agentName] = agentMountPoints
            else
              _mountPoints.remove(info.agentName)
          }

          newEvents << new NodeEvent(nodeInfo: info, eventType: event.eventType)
        }
      }

      if(newEvents)
      {
        _mountPointListeners.each { listener ->
          listener.onEvents(newEvents)
        }
      }
    }
  }

  private String eventsLogString(events, boolean summary)
  {
    if(summary)
    {
      StringBuilder sb = new StringBuilder()
      events?.each { org.linkedin.zookeeper.tracker.NodeEvent e ->
        if(sb.size() > 0)
          sb << ';'
        sb << "class=${e.class.simpleName},type=${e.eventType},path=${e.path}"
      }
      return "[${sb}]".toString()
    }
    else
    {
      return "${events}".toString()
    }
  }
}