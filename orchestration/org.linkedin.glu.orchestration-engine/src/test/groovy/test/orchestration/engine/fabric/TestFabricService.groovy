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

package test.orchestration.engine.fabric

import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.zookeeper.server.StandaloneZooKeeperServer
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.orchestration.engine.fabric.FabricServiceImpl
import org.linkedin.glu.orchestration.engine.fabric.FabricStorage
import org.linkedin.util.clock.Timespan
import org.linkedin.zookeeper.client.IZKClient
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.ZooDefs.Ids

/**
 * @author yan@pongasoft.com */
public class TestFabricService extends GroovyTestCase
{
  FileSystemImpl fs = FileSystemImpl.createTempFileSystem()
  StandaloneZooKeeperServer zookeeperServer
  FabricServiceImpl fabricService
  Collection<Fabric> fabrics = []

  def port = 2121

  protected void setUp()
  {
    super.setUp();

    zkStart()
  }

  private def zkStart()
  {
    zookeeperServer = new StandaloneZooKeeperServer(tickTime: 2000,
                                                    clientPort: port,
                                                    dataDir: fs.root.file.canonicalPath)
    zookeeperServer.start()

    fabricService = new FabricServiceImpl()
    fabricService.fabricStorage = [
      loadFabrics: { return fabrics }
    ] as FabricStorage
  }

  protected void tearDown()
  {
    try
    {
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

  public void testGetAgents()
  {
    fabrics.addAll(['f1', 'f2', 'f3'].collect { name ->
      new Fabric(name: name,
                 zkConnectString: "localhost:${port}".toString(),
                 zkSessionTimeout: Timespan.parse('5s'),
                 color: "#ffff${name}".toString())
    })

    fabricService.withZkClient('f1') { IZKClient client ->
      // agent a1 is not assigned any fabric
      client.createWithParents("${fabricService.zookeeperAgentsFabricRoot}/a1",
                               null,
                               Ids.OPEN_ACL_UNSAFE,
                               CreateMode.PERSISTENT)

      // agent a2 is part of fabric f2
      client.createWithParents("${fabricService.zookeeperAgentsFabricRoot}/a2/fabric",
                               'f2',
                               Ids.OPEN_ACL_UNSAFE,
                               CreateMode.PERSISTENT)

      // agent a3 is part of fabric f2
      client.createWithParents("${fabricService.zookeeperAgentsFabricRoot}/a3/fabric",
                               'f2',
                               Ids.OPEN_ACL_UNSAFE,
                               CreateMode.PERSISTENT)

      // agent a4 is part of fabric f3
      client.createWithParents("${fabricService.zookeeperAgentsFabricRoot}/a4/fabric",
                               'f3',
                               Ids.OPEN_ACL_UNSAFE,
                               CreateMode.PERSISTENT)
    }

    def agents = fabricService.agents

    assertEquals(4, agents.size())
    assertTrue(agents.containsKey('a1'))
    assertNull(agents['a1'])
    assertEquals('f2', agents['a2'])
    assertEquals('f2', agents['a3'])
    assertEquals('f3', agents['a4'])
  }

  public void testSetAndClearAgents()
  {
    fabrics.addAll(['f1', 'f2', 'f3'].collect { name ->
      new Fabric(name: name,
                 zkConnectString: "localhost:${port}".toString(),
                 zkSessionTimeout: Timespan.parse('5s'),
                 color: "#ffff${name}".toString())
    })

    def agents = fabricService.agents
    assertEquals(0, agents.size())

    fabricService.withZkClient('f1') { IZKClient client ->
      assertNull(client.exists("${fabricService.zookeeperAgentsFabricRoot}/a1"))
    }

    // assigning f1 to a1
    fabricService.setAgentFabric('a1', 'f1')

    fabricService.withZkClient('f1') { IZKClient client ->
      assertEquals('f1', client.getStringData("${fabricService.zookeeperAgentsFabricRoot}/a1/fabric"))
    }
    agents = fabricService.agents
    assertEquals(1, agents.size())
    assertEquals('f1', agents['a1'])

    // calling a second time doesn't fail!
    fabricService.setAgentFabric('a1', 'f1')

    fabricService.withZkClient('f1') { IZKClient client ->
      assertEquals('f1', client.getStringData("${fabricService.zookeeperAgentsFabricRoot}/a1/fabric"))
    }
    agents = fabricService.agents
    assertEquals(1, agents.size())
    assertEquals('f1', agents['a1'])


    // clearing it
    assertTrue(fabricService.clearAgentFabric('a1', 'f1'))

    agents = fabricService.agents
    assertEquals(0, agents.size())
    fabricService.withZkClient('f1') { IZKClient client ->
      assertNull(client.exists("${fabricService.zookeeperAgentsFabricRoot}/a1"))
    }

    // this time it returns false because it was already missing
    assertFalse(fabricService.clearAgentFabric('a1', 'f1'))

  }
}