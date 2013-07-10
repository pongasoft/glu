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

metaModelVersion = '1.0.0'
gluVersion = '@glu.version@'

def fabric = 'glu-dev-1'

def zooKeeperVersion = '@zookeeper.version@'

def keys = [
  agentKeyStore: [
    uri: 'agent.keystore',
    checksum: 'JSHZAn5IQfBVp1sy0PgA36fT_fD',
    storePassword: 'nacEn92x8-1',
    keyPassword: 'nWVxpMg6Tkv'
  ],

  agentTrustStore: [
    uri: 'agent.truststore',
    checksum: 'CvFUauURMt-gxbOkkInZ4CIV50y',
    storePassword: 'nacEn92x8-1',
    keyPassword: 'nWVxpMg6Tkv'
  ],

  consoleKeyStore: [
    uri: 'console.keystore',
    checksum: 'wxiKSyNAHN2sOatUG2qqIpuVYxb',
    storePassword: 'nacEn92x8-1',
    keyPassword: 'nWVxpMg6Tkv'
  ],

  consoleTrustStore: [
    uri: 'console.truststore',
    checksum: 'qUFMIePiJhz8i7Ow9lZmN5pyZjl',
    storePassword: 'nacEn92x8-1',
  ],
]

fabrics[fabric] = [
  keys: keys,
  console: 'tutorialConsole',
  zooKeeperCluster: 'tutorialZooKeeperCluster'
]

agents << [
  name: 'agent-1',
  host: 'localhost',
  fabric: fabric
]

consoles << [
  name: 'tutorialConsole',
  host: 'localhost',
  plugins: [
    [
      fqcn: 'org.linkedin.glu.orchestration.engine.plugins.builtin.StreamFileContentPlugin'
    ]
  ],
  configTokens: [
    dataSource: """
dataSource.dbCreate ='update'
dataSource.url="jdbc:hsqldb:file:\${System.properties['user.dir']}/database/prod;shutdown=true"
""",
  ]
]

zooKeeperClusters << [
  name: 'tutorialZooKeeperCluster',
  zooKeepers: [
    [
      version: zooKeeperVersion,
      host: '127.0.0.1'
    ]
  ],
]
