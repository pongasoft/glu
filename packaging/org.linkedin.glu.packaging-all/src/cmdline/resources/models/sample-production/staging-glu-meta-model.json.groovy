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

/**
 * This is a json/groovy DSL: you can use the power of groovy to 'dynamically' generate the output,
 * for example using if/then conditions, looping for repeated values etc...
 */

/**
 * Defining global versions
 */
metaModelVersion = '1.0.0' // the version of the format of this file
gluVersion = '@glu.version@' // the glu version
def zooKeeperVersion = '@zookeeper.version@' // the version for ZooKeeper distribution

/**
 * Step 1 (generate keys) produce a block of code that needs to be copied right below
 * (note that if you do not use keys, simply replace the following section by:
 *
 * def keys = null
 *
 */
////////////////////////////////////////
def keys = [:]
////////////////////////////////////////

/**
 * Define your fabrics here
 */
def stagingAlphaFabric = "staging-alpha"
def stagingBetaFabric = "staging-beta"

[stagingAlphaFabric, stagingBetaFabric].each { fabric ->
  fabrics[fabric] = [
    keys: keys,
    console: 'stgConsole',
    zooKeeperCluster: 'stgZkCluster'
  ]
}

/**
 * Define where each components should be installed on their target host.
 * Note that a trailing / indicates it will be considered a directory in which to install
 * the package.
 *
 * Otherwise provide the actual name you want (which you need to define for each type):
 * '/opt/glu/agent-server'
 */
def installPath = '/opt/glu/'

/**
 * Define your ZooKeeperClusters here.
 * In general you need one ZooKeeper cluster per data center.
 * In general a ZooKeeper cluster is 3 or 5 ZooKeeper instances.
 */
zooKeeperClusters << [
  name: 'stgZkCluster',
  zooKeepers: ['zk-host1', 'zk-host2', 'zk-host3'].collect { zkHost ->
    [
      version: zooKeeperVersion,
      host: zkHost,
      install: [
        path: installPath,
      ],
      configTokens: [:] // map of config tokens if necessary
    ]
  }
]

/**
 * Define your consoles here.
 * You may need more than 1 console if you have multiple data centers (each console should be
 * hosted in the same data center as the ZooKeeper cluster it is talking to).
 *
 * This section uses a more "real" database than the tutorial (MySql). You still need to install/
 * configure the database outside of glu (creating the user needed below for example...)
 *
 */

def consolePlugins = [
  [
    fqcn: 'org.linkedin.glu.orchestration.engine.plugins.builtin.StreamFileContentPlugin'
  ],
  /*
    add your own console plugins here
     [  fqcn: 'com.acme.glu.MyPlugin',
        classPath: ['http://xxx/glu-plugin.jar', ...]
     ]
   */
]

consoles << [
  name: 'stgConsole',
  host: 'console-host1',
  install: [
    path: installPath,
  ],
  plugins: consolePlugins,
  dataSourceDriverUri: 'http://jcenter.bintray.com/mysql/mysql-connector-java/5.1.25/mysql-connector-java-5.1.25.jar',
  configTokens: [
    dataSource: """
def dataSourceUrl = "jdbc:mysql://mysql-host1/glu"
dataSource.dbCreate = "update"
dataSource.url = dataSourceUrl
dataSource.logSql=false // set to true for details (+ open trace logging level)
dataSource.dialect = "org.hibernate.dialect.MySQL5InnoDBDialect"
dataSource.driverClassName = "com.mysql.jdbc.Driver"
dataSource.username= "xxx"
dataSource.password = "yyy"
""",
  ]
]

/**
 * Define your agents here...
 */

[
  'agent-host-1': stagingAlphaFabric,
  'agent-host-2': stagingAlphaFabric,
  'agent-host-3': stagingBetaFabric].each { agentHost, fabric ->

  agents << [
    host: agentHost,
    install: [
      path: installPath,
    ],
    fabric: fabric,
    configTokens: [:] // map of config tokens if necessary
  ]
}


