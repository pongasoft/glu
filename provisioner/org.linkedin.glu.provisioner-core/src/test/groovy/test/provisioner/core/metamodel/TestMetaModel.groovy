/*
 * Copyright (c) 2013 Yan Pujante
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

package test.provisioner.core.metamodel

import org.linkedin.util.io.ram.RAMDirectory
import org.pongasoft.glu.provisioner.core.metamodel.AgentMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ConsoleMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperClusterMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.impl.builder.JsonMetaModelSerializerImpl

/**
 * @author yan@pongasoft.com  */
public class TestMetaModel extends GroovyTestCase
{
  /**
   * Empty model
   */
  public void testEmptyGluMetaModel()
  {
    def model = """{
}"""

    def expectedModel = """
{
  "metaModelVersion": "1.0.0"
}
"""

    checkJson(model, expectedModel)
  }

  /**
   * This represents a model similar to the tutorial
   */
  public void testTutorialGluMetaModel()
  {
    def model = new File("../../packaging/org.linkedin.glu.packaging-all/src/cmdline/resources/conf/tutorial/glu-meta-model.json.groovy").text

    def expectedModel = """
{
  "agents": [
    {
      "fabric": "glu-dev-1",
      "host": "localhost",
      "name": "agent-1",
      "version": "@glu.version@"
    }
  ],
  "consoles": [
    {
      "host": "localhost",
      "name": "tutorialConsole",
      "version": "@glu.version@"
    }
  ],
  "fabrics": {
    "glu-dev-1": {
      "console": "tutorialConsole",
      "keys": {
        "agentKeyStore": {
          "checksum": "JSHZAn5IQfBVp1sy0PgA36fT_fD",
          "keyPassword": "nWVxpMg6Tkv",
          "storePassword": "nacEn92x8-1",
          "uri": "keys/agent.keystore"
        },
        "agentTrustStore": {
          "checksum": "JSHZAn5IQfBVp1sy0PgA36fT_fD",
          "keyPassword": "nWVxpMg6Tkv",
          "storePassword": "nacEn92x8-1",
          "uri": "keys/agent.keystore"
        },
        "consoleKeyStore": {
          "checksum": "JSHZAn5IQfBVp1sy0PgA36fT_fD",
          "keyPassword": "nWVxpMg6Tkv",
          "storePassword": "nacEn92x8-1",
          "uri": "keys/console.keystore"
        },
        "consoleTrustStore": {
          "checksum": "qUFMIePiJhz8i7Ow9lZmN5pyZjl",
          "storePassword": "nacEn92x8-1",
          "uri": "keys/console.truststore"
        }
      },
      "zooKeeperCluster": "tutorialZooKeeperCluster"
    }
  },
  "metaModelVersion": "1.0.0",
  "zooKeeperClusters": [
    {
      "fabrics": [
        "glu-dev-1"
      ],
      "name": "tutorialZooKeeperCluster",
      "zooKeepers": [
        {
          "host": "127.0.0.1",
          "version": "@zooKeeper.version@"
        }
      ]
    }
  ]
}"""

    def metaModel = checkJson(model, expectedModel)

    assertEquals(['glu-dev-1'], metaModel.fabrics.keySet() as List)

    // agents
    assertEquals(1, metaModel.agents.size())
    AgentMetaModel agent = metaModel.findAgent('glu-dev-1', 'agent-1')
    assertEquals(metaModel.agents.iterator().next(), agent)
    assertEquals(12906, agent.mainPort)
    assertEquals('localhost', agent.host.resolveHostAddress())

    // consoles
    assertEquals(1, metaModel.consoles.size())
    ConsoleMetaModel console = metaModel.findConsole('tutorialConsole')
    assertEquals(console, metaModel.consoles.values().iterator().next())
    assertEquals(8080, console.mainPort)
    assertEquals('localhost', console.host.resolveHostAddress())

    // zookeeper clusters
    assertEquals(1, metaModel.zooKeeperClusters.size())
    ZooKeeperClusterMetaModel zkCluster = metaModel.findZooKeeperCluster('tutorialZooKeeperCluster')
    assertEquals(zkCluster, metaModel.zooKeeperClusters.values().iterator().next())
    assertEquals('127.0.0.1:2181', zkCluster.zooKeeperConnectionString)
    assertEquals(1, zkCluster.zooKeepers.size())
    ZooKeeperMetaModel zk = zkCluster.zooKeepers[0]
    assertEquals(2181, zk.mainPort)
    assertEquals(2888, zk.quorumPort)
    assertEquals(3888, zk.leaderElectionPort)
    assertEquals('127.0.0.1', zk.host.resolveHostAddress())

  }

  private GluMetaModel checkJson(String model, String expectedModel)
  {
    RAMDirectory ram = new RAMDirectory()
    ram.add('model', model)
    JsonMetaModelSerializerImpl serializer = new JsonMetaModelSerializerImpl()
    def metaModel = serializer.deserialize([ram.toResource().createRelative('/model')])
    assertEquals((expectedModel ?: model).trim(), serializer.serialize(metaModel, true))
    return metaModel
  }
}