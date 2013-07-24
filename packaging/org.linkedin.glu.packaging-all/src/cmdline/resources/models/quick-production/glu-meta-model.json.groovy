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
 * The purpose of this model is to be a quick and easy way to test glu in a more 'real' setup
 * than the tutorial: each component goes on a different host: replace localhost with the names
 * of your hosts
 */

////////////////////////////////////////////////////////////////////////////////
// START QUICK SETUP
////////////////////////////////////////////////////////////////////////////////

/**
 * Define where each components should be installed on their target host.
 * Note that a trailing / indicates it will be considered a directory in which to install
 * the package.
 */
def installPath = '/opt/glu/'

/**
 * Run $GLU_HOME/bin/setup.sh -K -o /xxx to generate the keys and copy/paste the section
 * from the command output
 */
////////////////////////////////////////
def keys = [:]
////////////////////////////////////////

/**
 * Define the name of your fabric
 */
def fabric = "fabric-1"

/**
 * Defines the hosts on which the ZooKeeper cluster should be installed (recommended 3 or 5).
 */
def zooKeeperHost1 = "localhost"
def zooKeeperHost2 = null
def zooKeeperHost3 = null
def zooKeeperHost4 = null
def zooKeeperHost5 = null

/**
 * Defines the host on which the console should be installed
 */
def consoleHost = "localhost"

/**
 * Defines the host where mysql is running. If you want to use the default database (not
 * recommended!) then set to null
 */
def mysqlHost = "localhost"
def mysqlUsername = "glua"
def mysqlPassword = "password"
//def mysqlHost = null

/**
 * Defines the host(s) on which the agent should be installed
 */
def agentHosts = ["localhost", ]

////////////////////////////////////////////////////////////////////////////////
// END QUICK SETUP
////////////////////////////////////////////////////////////////////////////////














////////////////////////////////////////////////////////////////////////////////
// What is below is to build a model based on the information provided above
// Feel free to skip for now!
////////////////////////////////////////////////////////////////////////////////

/**
 * Pre defined global versions
 */
metaModelVersion = '1.0.0' // the version of the format of this file
gluVersion = '@glu.version@' // the glu version
def zooKeeperVersion = '@zookeeper.version@' // the version for ZooKeeper distribution

fabrics[fabric] = [
  keys: keys,
  console: 'default',
  zooKeeperCluster: 'zkc1'
]

def zooKeeperHosts =
  [zooKeeperHost1, zooKeeperHost2, zooKeeperHost3, zooKeeperHost4, zooKeeperHost5].findAll { it }

zooKeeperClusters << [
  name: 'zkc1',
  zooKeepers: zooKeeperHosts.collect { zkHost ->
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

def consolePlugins = [
  [
    fqcn: 'org.linkedin.glu.orchestration.engine.plugins.builtin.StreamFileContentPlugin'
  ],
]

def dbs = [
  hsqldb: [
    dataSource: null // use built-in hsqldb
  ],
  mysql: [
    dataSourceDriverUri: 'http://jcenter.bintray.com/mysql/mysql-connector-java/5.1.25/mysql-connector-java-5.1.25.jar',
    dataSource: """
def dataSourceUrl = "jdbc:mysql://${mysqlHost}/glu"
dataSource.dbCreate = "update"
dataSource.url = dataSourceUrl
dataSource.logSql=false // set to true for details (+ open trace logging level)
dataSource.dialect = "org.hibernate.dialect.MySQL5InnoDBDialect"
dataSource.driverClassName = "com.mysql.jdbc.Driver"
dataSource.username= "${mysqlUsername}"
dataSource.password = "${mysqlPassword}"
""",
  ]
]

// pick the right dataSource definition depending on mysqlHost
def db = mysqlHost ? dbs.mysql : dbs.hsqldb

consoles << [
  name: 'default',
  host: consoleHost,
  install: [
    path: installPath,
  ],
  plugins: consolePlugins,
  dataSourceDriverUri: db.dataSourceDriverUri,
  configTokens: [
    dataSource: db.dataSource,
  ]
]

agentHosts.each { agentHost ->
  agents << [
    host: agentHost,
    install: [
      path: installPath,
    ],
    fabric: fabric,
  ]
}


