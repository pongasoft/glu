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


package test.agent.tracker

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.tracker.AgentsTracker
import org.linkedin.glu.agent.tracker.AgentsTrackerImpl
import org.linkedin.glu.agent.tracker.TrackerEventsListener
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.Watcher.Event.KeeperState
import org.apache.zookeeper.ZooDefs.Ids
import org.apache.zookeeper.ZooKeeper
import org.json.JSONObject
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.util.exceptions.InternalException
import org.linkedin.zookeeper.server.StandaloneZooKeeperServer
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.util.clock.Timespan
import org.linkedin.zookeeper.client.ZooKeeperImpl
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.zookeeper.client.IZooKeeperFactory
import org.linkedin.zookeeper.tracker.NodeEventType
import org.linkedin.util.io.PathUtils
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock

/**
 * Tests for agents tracker
 *
 * @author ypujante@linkedin.com
 */
class TestAgentsTracker extends GroovyTestCase
{
  private final static String ROOT_PATH = '/test'
  private final static String AGENTS_INSTANCES = "${ROOT_PATH}/${AgentsTracker.ZK_AGENTS_INSTANCES}"
  private final static String AGENTS_STATE = "${ROOT_PATH}/${AgentsTracker.ZK_AGENTS_STATE}"
  
  Clock clock = SystemClock.INSTANCE
  FileSystemImpl fs = FileSystemImpl.createTempFileSystem()
  StandaloneZooKeeperServer zookeeperServer
  IZKClient client

  def private agentEvents = []

  private ZooKeeper _testableZooKeeper
  private boolean _failCreation = false
  private int _failedCount = 0

  private testableZooKeeperFactory = { Watcher watcher ->
    if(_failCreation)
    {
      _failedCount++
      throw new InternalException('TestAgentsTracker', 'failing creation')
    }

    _testableZooKeeper = new ZooKeeper('localhost:2121', (int) Timespan.parse('1m').durationInMilliseconds, watcher)
    new ZooKeeperImpl(_testableZooKeeper)
  }

  protected void setUp()
  {
    super.setUp();

    zookeeperServer = new StandaloneZooKeeperServer(tickTime: 2000,
                                                    clientPort: 2121,
                                                    dataDir: fs.root.file.canonicalPath)
    zookeeperServer.start()

    client = new ZKClient(testableZooKeeperFactory as IZooKeeperFactory)
    client.reconnectTimeout = Timespan.parse('500')

    client.start()
    client.waitForStart(Timespan.parse('5s'))
  }

  protected void tearDown()
  {
    try
    {
      client.destroy()
      zookeeperServer.shutdown()
      zookeeperServer.waitForShutdown(100)
      fs.destroy()
    }
    finally
    {
      super.tearDown();
    }
  }

  /**
   * Test for adding / removing / modifying the state
   */
  void testAgentsTracker()
  {
    AgentsTracker tracker = new AgentsTrackerImpl(client, ROOT_PATH)

    def agentEvents = []

    def agentListener = { events ->
      synchronized(agentEvents)
      {
        agentEvents.addAll(events)
        agentEvents.notifyAll()
      }
    }
    tracker.registerAgentListener(agentListener as TrackerEventsListener)

    def mountPointEvents = []

    def mountPointListener = { events ->
      synchronized(mountPointEvents)
      {
        mountPointEvents.addAll(events)
        mountPointEvents.notifyAll()
      }
    }
    tracker.registerMountPointListener(mountPointListener as TrackerEventsListener)

    tracker.start()
    tracker.waitForStart('5s')
    try
    {
      def events
      def range = (1..50)

      //////////////////////////////
      // add N agent instances
      range.each {
        addAgentInstance("a${it}", [p1: "a${it}v1".toString()])
      }

      // check for correct values
      events = waitForEvents(agentEvents, range.size())
      range.each {
        events = checkAgentEvents(events, "a${it}", [p1: "a${it}v1".toString()], NodeEventType.ADDED)
      }
      assertEquals(0, events.size())



      //////////////////////////////
      // add 1 state per agent
      range.each {
        setAgentState("a${it}", "/s${it}", [state: "a${it}v1".toString()])
      }

      // check for correct values
      events = waitForEvents(mountPointEvents, range.size())
      range.each {
        events = checkMountPointEvents(events, "a${it}", "/s${it}", [state: "a${it}v1".toString()], NodeEventType.ADDED)
      }
      assertEquals(0, events.size())



      //////////////////////////////
      // modify the state
      range.each {
        setAgentState("a${it}", "/s${it}", [state: "a${it}v1.1"])
      }

      // check for correct values
      events = waitForEvents(mountPointEvents, range.size())
      range.each {
        events = checkMountPointEvents(events, "a${it}", "/s${it}", [state: "a${it}v1.1".toString()], NodeEventType.UPDATED)
      }
      assertEquals(0, events.size())



      //////////////////////////////
      // delete the state
      range.each {
        deleteAgentState("a${it}", "/s${it}")
      }

      // check for correct values
      events = waitForEvents(mountPointEvents, range.size())
      range.each {
        events = checkMountPointEvents(events, "a${it}", "/s${it}", [state: "a${it}v1.1".toString()], NodeEventType.DELETED)
      }
      assertEquals(0, events.size())



      //////////////////////////////
      // resets the state
      range.each {
        setAgentState("a${it}", "/s${it}", [state: "a${it}v1".toString()])
      }

      // check for correct values
      events = waitForEvents(mountPointEvents, range.size())
      range.each {
        events = checkMountPointEvents(events, "a${it}", "/s${it}", [state: "a${it}v1".toString()], NodeEventType.ADDED)
      }
      assertEquals(0, events.size())



      //////////////////////////////
      // delete the instance (will trigger many events)
      range.each {
        removeAgentInstance("a${it}")
      }

      // check for correct values (states)
      events = waitForEvents(mountPointEvents, range.size())
      range.each {
        events = checkMountPointEvents(events, "a${it}", "/s${it}", [state: "a${it}v1".toString()], NodeEventType.DELETED)
      }
      assertEquals(0, events.size())

      // check for correct values (agents)
      events = waitForEvents(agentEvents, range.size())
      range.each {
        events = checkAgentEvents(events, "a${it}", [p1: "a${it}v1".toString()], NodeEventType.DELETED)
      }
      assertEquals(0, events.size())

      ///////////// Recovery /////////////

      //////////////////////////////
      // add N agent instances
      range.each {
        addAgentInstance("a${it}", [p1: "a${it}v1".toString()])
      }

      // check for correct values
      events = waitForEvents(agentEvents, range.size())
      range.each {
        events = checkAgentEvents(events, "a${it}", [p1: "a${it}v1".toString()], NodeEventType.ADDED)
      }
      assertEquals(0, events.size())

      assertEquals(range.size(), tracker.agentInfos.size())

      // we force an expired event
      _failCreation = true
      _testableZooKeeper.close()
      client.process(new WatchedEvent(Watcher.Event.EventType.None, KeeperState.Expired, null))

      // should receive the disconnected event
      GroovyConcurrentUtils.waitForCondition(clock, '5s', '200') { !tracker.connected }

      assertEquals(range.size(), tracker.agentInfos.size())

      _failCreation = false
      
      tracker.waitForStart('5s')

      addAgentInstance("a100", [p1: "a100v1"])
      events = waitForEvents(agentEvents, range.size() + 1)
      range.each {
        events = checkAgentEvents(events, "a${it}", [p1: "a${it}v1".toString()], NodeEventType.ADDED)
      }
      events = checkAgentEvents(events, "a100", [p1: "a100v1"], NodeEventType.ADDED)
      assertEquals(0, events.size())

      assertEquals(range.size() + 1, tracker.agentInfos.size())

    }
    finally
    {
      tracker.destroy()
    }
  }

  private def waitForEvents(events, size)
  {
    synchronized(events)
    {
      GroovyConcurrentUtils.awaitFor(clock, '5s', events) {
        events.size() == size
      }

      def res = [*events]
      events.clear()
      return res
    }
  }

  private def checkAgentEvents(events, expectedName, expectedData, expectedEventType)
  {
    def event = events.find { it.nodeInfo.agentName == expectedName }
    assertNotNull("event not found for ${expectedName}", event)
    assertEquals(expectedEventType, event.eventType)
    assertEquals(expectedData, event.nodeInfo.agentProperties)

    return events.findAll { it.nodeInfo.agentName != expectedName }
  }

  private def checkMountPointEvents(events, expectedName, expectedMountPoint, expectedData, expectedEventType)
  {
    def event = events.find { it.nodeInfo.agentName == expectedName }
    assertNotNull("event not found for ${expectedName}", event)
    assertEquals(expectedEventType, event.eventType)
    assertEquals(MountPoint.create(expectedMountPoint.toString()), event.nodeInfo.mountPoint)
    assertEquals(expectedData, event.nodeInfo.data)

    return events.findAll { it.nodeInfo.agentName != expectedName }
  }

  private def addAgentInstance(String name, data)
  {
    client.createOrSetWithParents(PathUtils.addPaths(AGENTS_INSTANCES, name),
                                  new JSONObject(data).toString(), 
                                  Ids.OPEN_ACL_UNSAFE,
                                  CreateMode.PERSISTENT)

    return [name: name, data: data]
  }

  private void removeAgentInstance(String name)
  {
    // delete all the states
    def path = PathUtils.addPaths(AGENTS_STATE, name)
    if(client.exists(path))
    {
      client.getChildren(path).each { child ->
        client.delete(PathUtils.addPaths(path, child))
      }
      client.delete(path)
    }

    // delete the instance
    client.delete(PathUtils.addPaths(AGENTS_INSTANCES, name))
  }

  private void setAgentState(String name, mountPoint, state)
  {
    def path = PathUtils.addPaths(AGENTS_STATE, name)
    path = PathUtils.addPaths(path, toPath(MountPoint.create(mountPoint.toString())))
    client.createOrSetWithParents(path,
                                  JsonUtils.toJSON(state).toString(),
                                  Ids.OPEN_ACL_UNSAFE,
                                  CreateMode.PERSISTENT)
  }

  private void deleteAgentState(String name, mountPoint)
  {
    def path = PathUtils.addPaths(AGENTS_STATE, name)
    path = PathUtils.addPaths(path, toPath(MountPoint.create(mountPoint.toString())))
    client.delete(path)
  }

  private String toPath(MountPoint mp)
  {
    return mp.path.replace('/', '_')
  }

  private MountPoint fromPath(String path)
  {
    return MountPoint.create(path.replace('_', '/'))
  }
}
