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
    assertNull(new FabricManager(null, null, null).fabric)
    assertNull(new FabricManager(null, null, new File('/do/not/exists')).fabric)

    def envFile = fs.saveContent('/env', 'myEnvFromFile')
    assertEquals('myEnvFromFile', new FabricManager(null, null, envFile.file).fabric)

    ThreadControl tc = new ThreadControl()

    Thread.start {
      tc.unblock('b0')
      tc.unblock('b1')
      client.createWithParents('/org/glu/agents/names/myHost/fabric',
                               'myEnvFromZooKeeper',
                               Ids.OPEN_ACL_UNSAFE,
                               CreateMode.PERSISTENT)
    }

    tc.block('b0')
    def mgr = new FabricManager(client, "/org/glu/agents/names/myHost", null)
    shouldFail(TimeoutException) {mgr.getFabric(200)}

    tc.block('b1')
    assertEquals('myEnvFromZooKeeper', mgr.fabric)
  }
}
