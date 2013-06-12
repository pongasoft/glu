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

fabric = 'glu-dev-1'

def gluVersion = '@glu.version@'
def jettyVersion = '@jetty.version@'
def zooKeeperVersion = '@zooKeeper.version@'
def zkRoot = '/org/glu'

def keys = [
  agentKeystore: [
    path: 'keys/agent.keystore',
    checksum: 'JSHZAn5IQfBVp1sy0PgA36fT_fD',
    keystorePassword: 'nacEn92x8-1',
    keyPassword: 'nWVxpMg6Tkv'
  ],

  agentTruststore: [
    path: 'keys/agent.keystore',
    checksum: 'JSHZAn5IQfBVp1sy0PgA36fT_fD',
    truststorePassword: 'nacEn92x8-1',
    keyPassword: 'nWVxpMg6Tkv'
  ],

  consoleKeystore: [
    path: 'keys/console.keystore',
    checksum: 'JSHZAn5IQfBVp1sy0PgA36fT_fD',
    keystorePassword: 'nacEn92x8-1',
    keyPassword: 'nWVxpMg6Tkv'
  ],

  consoleTruststore: [
    path: 'keys/console.truststore',
    checksum: 'qUFMIePiJhz8i7Ow9lZmN5pyZjl',
    truststorePassword: 'nacEn92x8-1',
  ],
]

agents << [
  name: 'agent-1',
  host: 'localhost',
  version: gluVersion,
  config: "agent-local-config"
]

configs << [
  name: "agent-local-config",
  from: [
    template: 'templates/agent/agentConfig.properties.gtmpl',
    tokens: [
      'glu.agent.configURL': 'zookeeper:${glu.agent.zookeeper.root}/agents/fabrics/${glu.agent.fabric}/config/config.properties'
    ]
  ],
  to: "${gluVersion}/conf/",
]

consoles << [
  host: 'localhost',
  version: gluVersion,
  configs: ['console-config', 'console-console-keystore', 'console-agent-truststore']
]

// console configuration (keys)
configs << [
  name:'console-config',
  from: [
    template: 'templates/console/glu-console-webapp.groovy.xtmpl',
    tokens: [
      'console.keystorePath': '"${keysDir}/console.keystore"',
      'console.keystorePassword': keys.consoleKeystore.keystorePassword,
      'console.keyPassword': keys.consoleKeystore.keyPassword,

      'console.truststorePath' : '"${keysDir}/agent.truststore"',
      'console.truststorePassword': keys.agentTruststore.truststorePassword
    ]
  ],
  to: 'conf/',
]

// console.keystore stored in keys folder
configs << [
  name: 'console-console-keystore',
  from: keys.consoleKeystore.path,
  to: "keys/console.keystore"
]

// agent.truststore stored in keys folder
configs << [
  name: 'console-agent-truststore',
  from: keys.agentTruststore.path,
  to: "keys/agent.truststore"
]

zooKeeperClusters << [
  zooKeepers: [
    [
      version: zooKeeperVersion,
      host: '127.0.0.1'
    ]
  ],
  configs: ['zookeeper-agent-config', 'zookeeper-agent-keystore', 'zookeeper-console-truststore']
]

// extra agent configuration stored in ZooKeeper (keys)
configs << [
  name: 'zookeeper-agent-config',
  from: [
    template: 'templates/agent/zookeeper-config.properties.gtmpl',
    tokens: [
      'glu.agent.keystorePath': 'zookeeper:${glu.agent.zookeeper.root}/agents/fabrics/${glu.agent.fabric}/config/agent.keystore',
      'glu.agent.keystoreChecksum': keys.agentKeystore.checksum,
      'glu.agent.keystorePassword': keys.agentKeystore.keystorePassword,
      'glu.agent.keyPassword': keys.agentKeystore.keyPassword,

      'glu.agent.truststorePath': 'zookeeper:${glu.agent.zookeeper.root}/agents/fabrics/${glu.agent.fabric}/config/console.truststore',
      'glu.agent.truststoreChecksum': keys.consoleTruststore.checksum,
      'glu.agent.truststorePassword': keys.consoleTruststore.truststorePassword
    ]
  ],
  to: "zookeeper:${zkRoot}/agents/fabrics/${fabric}/config/config.properties"
]

// agent.keystore stored in ZooKeeper
configs << [
  name: 'zookeeper-agent-keystore',
  from: keys.agentKeystore.path,
  to: "zookeeper:${zkRoot}/agents/fabrics/${fabric}/config/agent.keystore"
]

// console.truststore stored in ZooKeeper
configs << [
  name: 'zookeeper-console-truststore',
  from: keys.consoleTruststore.path,
  to: "zookeeper:${zkRoot}/agents/fabrics/${fabric}/config/console.truststore"
]

