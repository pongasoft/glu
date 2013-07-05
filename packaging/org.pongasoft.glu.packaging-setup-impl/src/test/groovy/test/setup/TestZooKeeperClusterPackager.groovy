package test.setup

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.glu.groovy.utils.shell.ShellImpl
import org.pongasoft.glu.packaging.setup.ZooKeeperClusterPackager

/**
 * @author yan@pongasoft.com  */
public class TestZooKeeperClusterPackager extends BasePackagerTest
{
  public void testTutorialModel()
  {
    ShellImpl.createTempShell { Shell shell ->
      def inputPackage = shell.mkdirs("/dist/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}")
      shell.saveContent(inputPackage.createRelative('README.md'), "this is the readme")
      shell.saveContent(inputPackage.createRelative('lib/acme.jar'), "this is the jar")

      def packager =
        new ZooKeeperClusterPackager(packagerContext: createPackagerContext(shell),
                                     outputFolder: shell.mkdirs('/out'),
                                     inputPackage: inputPackage,
                                     configsRoots: copyConfigs(shell.toResource('/configs')),
                                     metaModel: testModel.zooKeeperClusters['tutorialZooKeeperCluster'])

      def pkg = packager.createPackage()

      def expectedResources =
        [
          // zookeeper cluster config
          '/conf': DIRECTORY,
          '/conf/org': DIRECTORY,
          '/conf/org/glu': DIRECTORY,
          '/conf/org/glu/agents': DIRECTORY,
          '/conf/org/glu/agents/names': DIRECTORY,
          '/conf/org/glu/agents/names/agent-1': DIRECTORY,
          '/conf/org/glu/agents/names/agent-1/fabric': 'glu-dev-1',
          '/conf/org/glu/agents/fabrics': DIRECTORY,
          '/conf/org/glu/agents/fabrics/glu-dev-1': DIRECTORY,
          '/conf/org/glu/agents/fabrics/glu-dev-1/config': DIRECTORY,
          '/conf/org/glu/agents/fabrics/glu-dev-1/config/config.properties': TUTORIAL_AGENT_CONFIG_PROPERTIES,
          '/conf/org/glu/agents/fabrics/glu-dev-1/config/agent.keystore': toBinaryResource(keysRootResource.createRelative('agent.keystore')),
          '/conf/org/glu/agents/fabrics/glu-dev-1/config/console.truststore': toBinaryResource(keysRootResource.createRelative('console.truststore')),

          // zookeeper server config
          "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-127.0.0.1": DIRECTORY,
          "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-127.0.0.1/README.md": 'this is the readme',
          "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-127.0.0.1/bin": DIRECTORY,
          "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-127.0.0.1/bin/zookeeperctl.sh": ZOOKEEPERCTL_SH,
          "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-127.0.0.1/lib": DIRECTORY,
          "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-127.0.0.1/lib/acme.jar": 'this is the jar',
          "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-127.0.0.1/conf": DIRECTORY,
          "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-127.0.0.1/conf/zoo.cfg": """# The number of milliseconds of each tick
tickTime=2000
# The number of ticks that the initial
# synchronization phase can take
initLimit=10
# The number of ticks that can pass between
# sending a request and getting an acknowledgement
syncLimit=5
# the directory where the snapshot is stored.
dataDir=data
# the port at which the clients will connect
clientPort=2181
""",
        ]

      assertEquals(shell.toResource("/out/zookeeper-cluster-tutorialZooKeeperCluster"),
                   pkg.zooKeeperCluster.location)

      checkPackageContent(expectedResources, pkg.zooKeeperCluster.location)

      assertEquals(1, pkg.zooKeepers.size())

      def zk = (pkg.zooKeepers as List)[0]

      assertEquals(shell.toResource("/out/zookeeper-cluster-tutorialZooKeeperCluster/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-127.0.0.1"),
                   zk.location)
      assertEquals('127.0.0.1', zk.host)
      assertEquals(2181, zk.port)
    }
  }

  /**
   * Test for more than 1 node in the cluster
   */
  public void testCreatePackage2()
  {
    ShellImpl.createTempShell { Shell shell ->
      def inputPackage = shell.mkdirs("/dist/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}")
      shell.saveContent(inputPackage.createRelative('README.md'), "this is the readme")
      shell.saveContent(inputPackage.createRelative('lib/acme.jar'), "this is the jar")

      def metaModel = """
fabrics['f1'] = [
  ${DEFAULT_KEYS},
  zooKeeperCluster: 'zkc'
]

zooKeeperClusters << [
  name: 'zkc',
  zooKeepers: [
    [
      version: '${ZOOKEEPER_VERSION}',
      host: 'h1'
    ],
    [
      version: '${ZOOKEEPER_VERSION}',
      host: 'h2'
    ]
  ],
]
"""

      def packager =
        new ZooKeeperClusterPackager(packagerContext: createPackagerContext(shell),
                                     outputFolder: shell.mkdirs('/out'),
                                     inputPackage: inputPackage,
                                     configsRoots: copyConfigs(shell.toResource('/configs')),
                                     metaModel: toGluMetaModel(metaModel).zooKeeperClusters['zkc'])

      def pkg = packager.createPackage()

      assertEquals(shell.toResource("/out/zookeeper-cluster-zkc"),
                   pkg.zooKeeperCluster.location)

      def expectedResources = [
        // zookeeper cluster config
        '/conf': DIRECTORY,
        '/conf/org': DIRECTORY,
        '/conf/org/glu': DIRECTORY,
        '/conf/org/glu/agents': DIRECTORY,
        '/conf/org/glu/agents/fabrics': DIRECTORY,
        '/conf/org/glu/agents/fabrics/f1': DIRECTORY,
        '/conf/org/glu/agents/fabrics/f1/config': DIRECTORY,
        '/conf/org/glu/agents/fabrics/f1/config/config.properties': TUTORIAL_AGENT_CONFIG_PROPERTIES,
        '/conf/org/glu/agents/fabrics/f1/config/agent.keystore': toBinaryResource(keysRootResource.createRelative('agent.keystore')),
        '/conf/org/glu/agents/fabrics/f1/config/console.truststore': toBinaryResource(keysRootResource.createRelative('console.truststore')),

        // zookeeper server config for h1
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1/README.md": 'this is the readme',
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1/bin": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1/bin/zookeeperctl.sh": ZOOKEEPERCTL_SH,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1/lib": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1/lib/acme.jar": 'this is the jar',
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1/conf": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1/conf/myid": '1',
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1/conf/zoo.cfg": """# The number of milliseconds of each tick
tickTime=2000
# The number of ticks that the initial
# synchronization phase can take
initLimit=10
# The number of ticks that can pass between
# sending a request and getting an acknowledgement
syncLimit=5
# the directory where the snapshot is stored.
dataDir=data
# the port at which the clients will connect
clientPort=2181

server.1=h1:2888:3888

server.2=h2:2888:3888
""",

        // zookeeper server config for h2
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h2": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h2/README.md": 'this is the readme',
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h2/bin": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h2/bin/zookeeperctl.sh": ZOOKEEPERCTL_SH,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h2/lib": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h2/lib/acme.jar": 'this is the jar',
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h2/conf": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h2/conf/myid": '2',
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h2/conf/zoo.cfg": """# The number of milliseconds of each tick
tickTime=2000
# The number of ticks that the initial
# synchronization phase can take
initLimit=10
# The number of ticks that can pass between
# sending a request and getting an acknowledgement
syncLimit=5
# the directory where the snapshot is stored.
dataDir=data
# the port at which the clients will connect
clientPort=2181

server.1=h1:2888:3888

server.2=h2:2888:3888
""",
      ]

      checkPackageContent(expectedResources, pkg.zooKeeperCluster.location)

      assertEquals(2, pkg.zooKeepers.size())

      def zk1 = (pkg.zooKeepers as List)[0]

      assertEquals(shell.toResource("/out/zookeeper-cluster-zkc/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1"),
                   zk1.location)
      assertEquals('h1', zk1.host)
      assertEquals(2181, zk1.port)

      def zk2 = (pkg.zooKeepers as List)[1]

      assertEquals(shell.toResource("/out/zookeeper-cluster-zkc/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h2"),
                   zk2.location)
      assertEquals('h2', zk2.host)
      assertEquals(2181, zk2.port)
    }
  }

  /**
   * No ssl
   */
  public void testDifferentConfigs()
  {
    ShellImpl.createTempShell { Shell shell ->
      def inputPackage = shell.mkdirs("/dist/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}")
      shell.saveContent(inputPackage.createRelative('README.md'), "this is the readme")
      shell.saveContent(inputPackage.createRelative('lib/acme.jar'), "this is the jar")

      def metaModel = """
fabrics['f1'] = [
  zooKeeperCluster: 'zkc'
]

zooKeeperClusters << [
  name: 'zkc',
  zooKeepers: [
    [
      version: '${ZOOKEEPER_VERSION}',
      host: 'h1',
      ports: [
        mainPort: 1234
      ],
      configTokens: [
        tickTime: '<tickTime>',
        initLimit: '<initLimit>',
        syncLimit: '<syncLimit>',
        dataDir: '<dataDir>'
      ]
    ],
  ],
  configTokens: [
    'glu.agent.ivySettings': '<ivy settings>',
  ]
]
"""

      def packager =
        new ZooKeeperClusterPackager(packagerContext: createPackagerContext(shell),
                                     outputFolder: shell.mkdirs('/out'),
                                     inputPackage: inputPackage,
                                     configsRoots: copyConfigs(shell.toResource('/configs')),
                                     metaModel: toGluMetaModel(metaModel).zooKeeperClusters['zkc'])

      def pkg = packager.createPackage()

      assertEquals(shell.toResource("/out/zookeeper-cluster-zkc"),
                   pkg.zooKeeperCluster.location)

      def expectedResources = [
        // zookeeper cluster config
        '/conf': DIRECTORY,
        '/conf/org': DIRECTORY,
        '/conf/org/glu': DIRECTORY,
        '/conf/org/glu/agents': DIRECTORY,
        '/conf/org/glu/agents/fabrics': DIRECTORY,
        '/conf/org/glu/agents/fabrics/f1': DIRECTORY,
        '/conf/org/glu/agents/fabrics/f1/config': DIRECTORY,
        '/conf/org/glu/agents/fabrics/f1/config/config.properties': '''#
# Copyright (c) 2010-2010 LinkedIn, Inc
# Portions Copyright (c) 2013 Yan Pujante
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#

################################
# Security:

glu.agent.sslEnabled=false



################################
# Binary repository location
glu.agent.ivySettings=<ivy settings>

''',

        // zookeeper server config for h1
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1-1234": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1-1234/README.md": 'this is the readme',
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1-1234/bin": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1-1234/bin/zookeeperctl.sh": ZOOKEEPERCTL_SH,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1-1234/lib": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1-1234/lib/acme.jar": 'this is the jar',
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1-1234/conf": DIRECTORY,
        "/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1-1234/conf/zoo.cfg": """# The number of milliseconds of each tick
tickTime=<tickTime>
# The number of ticks that the initial
# synchronization phase can take
initLimit=<initLimit>
# The number of ticks that can pass between
# sending a request and getting an acknowledgement
syncLimit=<syncLimit>
# the directory where the snapshot is stored.
dataDir=<dataDir>
# the port at which the clients will connect
clientPort=1234
""",

      ]

      checkPackageContent(expectedResources, pkg.zooKeeperCluster.location, false)

      assertEquals(1, pkg.zooKeepers.size())

      def zk1 = (pkg.zooKeepers as List)[0]

      assertEquals(shell.toResource("/out/zookeeper-cluster-zkc/org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}-h1-1234"),
                   zk1.location)
      assertEquals('h1', zk1.host)
      assertEquals(1234, zk1.port)

    }
  }

  public static final String TUTORIAL_AGENT_CONFIG_PROPERTIES = """#
# Copyright (c) 2010-2010 LinkedIn, Inc
# Portions Copyright (c) 2013 Yan Pujante
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#

################################
# Security:

glu.agent.sslEnabled=true

glu.agent.keystorePath=zookeeper:\${glu.agent.zookeeper.root}/agents/fabrics/\${glu.agent.fabric}/config/agent.keystore
glu.agent.keystoreChecksum=JSHZAn5IQfBVp1sy0PgA36fT_fD
glu.agent.keystorePassword=nacEn92x8-1
glu.agent.keyPassword=nWVxpMg6Tkv

glu.agent.truststorePath=zookeeper:\${glu.agent.zookeeper.root}/agents/fabrics/\${glu.agent.fabric}/config/console.truststore
glu.agent.truststoreChecksum=qUFMIePiJhz8i7Ow9lZmN5pyZjl
glu.agent.truststorePassword=nacEn92x8-1



"""

  public static final String ZOOKEEPERCTL_SH = '''#!/bin/bash

#
# Copyright (c) 2013 Yan Pujante
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#

# from http://stackoverflow.com/questions/59895/can-a-bash-script-tell-what-directory-its-stored-in
SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$( cd -P "$( dirname "$SOURCE" )" && pwd )"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
BASEDIR="$( cd -P "$( dirname "$SOURCE" )/.." && pwd )"
cd $BASEDIR
./bin/zkServer.sh "$@"
'''

}