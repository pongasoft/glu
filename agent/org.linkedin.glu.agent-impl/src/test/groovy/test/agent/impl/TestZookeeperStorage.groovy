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


package test.agent.impl

import org.linkedin.glu.agent.impl.zookeeper.ZooKeeperStorage
import org.linkedin.glu.agent.api.MountPoint
import org.apache.zookeeper.KeeperException
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.zookeeper.server.StandaloneZooKeeperServer
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.util.clock.Timespan
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.glu.agent.impl.storage.AgentProperties
import org.linkedin.zookeeper.client.ZKData
import org.linkedin.glu.groovy.utils.test.GluGroovyTestUtils

/**
 * This test will start a zookeeper server and shuts it down, so it is not relying on one currently
 * running.
 *
 * @author ypujante@linkedin.com
 */
class TestZookeeperStorage extends GroovyTestCase
{
  FileSystemImpl fs = FileSystemImpl.createTempFileSystem()
  StandaloneZooKeeperServer zookeeperServer
  IZKClient client

  protected void setUp()
  {
    super.setUp();

    zkStart()
    client = new ZKClient('localhost:2121',
                          Timespan.parse('100'),
                          null)
    client.reconnectTimeout = Timespan.parse('500')
    zkClientStart()
  }

  private def zkClientStart()
  {
    client.start()
    client.waitForStart(Timespan.parse('5s'))
  }

  private def zkStart()
  {
    zookeeperServer = new StandaloneZooKeeperServer(tickTime: 2000,
                                                    clientPort: 2121,
                                                    dataDir: fs.root.file.canonicalPath)
    zookeeperServer.start()
  }

  protected void tearDown()
  {
    try
    {
      client.destroy()
      zkStop()
      fs.destroy()
    }
    finally
    {
      super.tearDown();
    }
  }

  private def zkStop()
  {
    zookeeperServer.shutdown()
    zookeeperServer.waitForShutdown(100)
  }

  void testWriteStorage()
  {
    IZKClient c = client.chroot('storage')
    IZKClient p = client.chroot('agent.properties')

    AgentProperties agentProperties = new AgentProperties('glu.p1': 'v1', 'p2': 'v2')

    ZooKeeperStorage storage = new ZooKeeperStorage(c, p)

    def state = [scriptState: [stateMachine: [currentState: 'installed']]]

    def ab = MountPoint.create('/a/b')
    def abc = MountPoint.create('/a/b/c')
    def abd = MountPoint.create('/a/b/d')

    storage.storeState(ab, state)

    assertEquals(JsonUtils.compactPrint(state), c.getStringData('_a_b'))

    def e = new Exception('abcdef')
    state.scriptState.stateMachine.error = e
    state.scriptState.stateMachine.transitionState = 'abc->def'

    storage.storeState(ab, state)

    state.scriptState.stateMachine.error = [[name: e.class.name, message: e.message]]

    assertEquals(JsonUtils.compactPrint(state), c.getStringData('_a_b'))

    storage.clearState(ab)

    shouldFail(KeeperException.NoNodeException) {
      c.getStringData('_a_b')
    }

    storage.storeState(ab, state)
    storage.storeState(abc, state)

    assertEquals([ab, abc], storage.getMountPoints().sort())

    storage.clearState(ab)

    assertEquals([abc], storage.getMountPoints())

    storage.saveAgentProperties(agentProperties)

    ZKData<String> data = p.getZKStringData('/')

    assertEquals(['glu.p1': 'v1'], JsonUtils.fromJSON(data.data))
    assertTrue(data.stat.ephemeralOwner > 0) // ephemeral node

    zkStop()

    // we make sure that zookeeper is disconnected
    client.waitForState(ZKClient.State.RECONNECTING, Timespan.parse('5s'))

    // this call should not fail but should not do anything
    storage.storeState(ab, state)

    // we restart zookeeper
    zkStart()

    client.waitForStart(Timespan.parse('5s')) 

    // we store abd (should work)
    storage.storeState(abd, state)

    // we make sure that ab was not added but abd was
    assertEquals([abc, abd], storage.getMountPoints())

    // ephemeral node should survive stop/restart of zookeeper itself
    data = p.getZKStringData('/')
    assertEquals(['glu.p1': 'v1'], JsonUtils.fromJSON(data.data))
    assertTrue(data.stat.ephemeralOwner > 0) // ephemeral node

    client.destroy()
    client = new ZKClient('localhost:2121',
                          Timespan.parse('100'),
                          null)
    client.reconnectTimeout = Timespan.parse('500')
    zkClientStart()

    c = client.chroot('storage')
    p = client.chroot('agent.properties')

    storage = new ZooKeeperStorage(c, p)

    // the mountpoints should still be there
    assertEquals([abc, abd], storage.getMountPoints())

    // on the other end the agent.properties are gone because it is an ephemeral node
    shouldFail(KeeperException.NoNodeException) { p.getZKStringData('/') }
  }

  public void testWeirdCharactersInMountPoint()
  {
    IZKClient c = client.chroot('storage')
    IZKClient p = client.chroot('agent.properties')

    ZooKeeperStorage storage = new ZooKeeperStorage(c, p)

    def state = [scriptState: [stateMachine: [currentState: 'installed']]]

    def mp = MountPoint.create('/a_b/c*d/e#f')

    storage.storeState(mp, state)

    GluGroovyTestUtils.assertEqualsIgnoreType(this,
                                              "state differ",
                                              state,
                                              JsonUtils.fromJSON(c.getStringData("_a%5Fb_c*d_e%23f")))

    GluGroovyTestUtils.assertEqualsIgnoreType(this,
                                              "mountpoints",
                                              [mp],
                                              storage.mountPoints)
  }
}
