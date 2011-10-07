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

package test.agent.server

import java.util.concurrent.TimeoutException
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.ZooDefs.Ids
import org.linkedin.glu.agent.server.FabricManager
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.util.clock.Timespan
import org.linkedin.util.concurrent.ThreadControl
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.zookeeper.client.ZKClient
import org.linkedin.zookeeper.server.StandaloneZooKeeperServer

/**
 * @author ypujante@linkedin.com */
class TestFabricManager extends GroovyTestCase
{
  FileSystemImpl fs
  StandaloneZooKeeperServer zookeeperServer
  IZKClient client

  protected void setUp()
  {
    super.setUp();
    fs = FileSystemImpl.createTempFileSystem()
    
    zookeeperServer = new StandaloneZooKeeperServer(tickTime: 2000,
                                                    clientPort: 2121,
                                                    dataDir: fs.root.file.canonicalPath)
    zookeeperServer.start()
    client = new ZKClient('localhost:2121', Timespan.parse('100'), null)

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

  void testFabricManager()
  {
    assertNull(new FabricManager(null, null, null, null, null).fabric)
    assertNull(new FabricManager(null, null, null, null, new File('/do/not/exists')).fabric)

    def fabricFile = fs.saveContent('/fabric', 'myFabricFromFile')
    assertEquals('myFabricFromFile', new FabricManager(null, null, null, null, fabricFile.file).fabric)

    // nothing in ZooKeeper prior to the call
    assertNull(client.exists("/org/glu/agents/names/myHost1/fabric"))
    // no value in ZooKeeper => previous value wins
    assertEquals('myFabricFromPrevious',
                 new FabricManager(client, "/org/glu/agents/names/myHost1", null, "myFabricFromPrevious", null).fabric)
    assertEquals('myFabricFromPrevious', client.getStringData("/org/glu/agents/names/myHost1/fabric"))

    client.createWithParents('/org/glu/agents/names/myHost2/fabric',
                             'myFabricFromZooKeeper2',
                             Ids.OPEN_ACL_UNSAFE,
                             CreateMode.PERSISTENT)

    // value in ZooKeeper => it wins
    assertEquals('myFabricFromZooKeeper2',
                 new FabricManager(client, "/org/glu/agents/names/myHost2", null, "myFabricFromPrevious", null).fabric)
    assertEquals('myFabricFromZooKeeper2', client.getStringData("/org/glu/agents/names/myHost2/fabric"))

    // no previous value => wait for value in ZooKeeper
    ThreadControl tc = new ThreadControl()

    Thread.start {
      tc.unblock('b0')
      tc.unblock('b1')
      client.createWithParents('/org/glu/agents/names/myHost3/fabric',
                               'myFabricFromZooKeeper3',
                               Ids.OPEN_ACL_UNSAFE,
                               CreateMode.PERSISTENT)
    }

    tc.block('b0')
    def mgr = new FabricManager(client, "/org/glu/agents/names/myHost3", null, null, null)
    shouldFail(TimeoutException) {mgr.getFabric(200)}

    tc.block('b1')
    assertEquals('myFabricFromZooKeeper3', mgr.fabric)

    // nothing in ZooKeeper prior to the call
    assertNull(client.exists("/org/glu/agents/names/myHost4/fabric"))
    // new value: wins over previous value!
    assertEquals('myFabricFromNewFabric4',
                 new FabricManager(client, "/org/glu/agents/names/myHost4", "myFabricFromNewFabric4", "myFabricFromPrevious4", null).fabric)
    assertEquals('myFabricFromNewFabric4', client.getStringData("/org/glu/agents/names/myHost4/fabric"))

    // even when something in zookeeper it gets replaced!
    assertEquals("myFabricFromPrevious5",
                 new FabricManager(client, "/org/glu/agents/names/myHost5", null, "myFabricFromPrevious5", null).fabric)
    assertEquals('myFabricFromPrevious5', client.getStringData("/org/glu/agents/names/myHost5/fabric"))
    assertEquals("myFabricFromNewFabric5",
                 new FabricManager(client, "/org/glu/agents/names/myHost5", "myFabricFromNewFabric5", "myFabricFromPrevious5", null).fabric)
    assertEquals('myFabricFromNewFabric5', client.getStringData("/org/glu/agents/names/myHost5/fabric"))
  }
}
