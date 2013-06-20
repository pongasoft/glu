package test.setup

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.glu.groovy.utils.shell.ShellImpl
import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.packaging.setup.PackagedArtifact
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
                                     configRoot: copyConfigs(shell.toResource('/configs')),
                                     metaModel: testModel.zooKeeperClusters['tutorialZooKeeperCluster'])

      def pkg = packager.createPackage()

      println pkg.zooKeeperCluster.location.path

      println pkg
    }
  }

  /**
   * A single package */
  public void testCreatePackage1()
  {
    ShellImpl.createTempShell { Shell shell ->
      def dist = shell.mkdirs('/dist')

      // building a package with a readme at the root and a another file in a subdirectory..
      // we will make sure that the packaging process does not affect anything else
      def zkDistPackage = dist.createRelative('org.linkedin.zookeeper-server-x.y.z')
      shell.saveContent(zkDistPackage.createRelative('README.md'), "this is the readme")
      shell.saveContent(zkDistPackage.createRelative('lib/acme.jar'), "this is the jar")


      def packager = new ZooKeeperClusterPackager(shell: shell,
                                                  configRoot: createZooKeeperTemplates(shell),
                                                  zookeperServers: [[host: 'h2']],
                                                  inputPackage: zkDistPackage,
                                                  outputFolder: shell.mkdirs('/out'))

      Collection<PackagedArtifact> zkps = packager.createPackages()

      assertEquals(1, zkps.size())
      PackagedArtifact zkp = zkps.iterator().next()

      assertEquals("/out/org.linkedin.zookeeper-server-x.y.z-h2-2181", zkp.location.path)
      assertEquals('h2', zkp.host)
      assertEquals(2181, zkp.port)

      def expectedResources =
        [
          '/README.md': 'this is the readme',
          '/lib': DIRECTORY,
          '/lib/acme.jar': 'this is the jar',
          '/conf': DIRECTORY,
          '/conf/zoo.cfg': '[clientPort:2181];[host:h2];[]'
        ]

      checkPackageContent(expectedResources, zkp.location)
    }
  }

  public void testCreatePackage2()
  {
    ShellImpl.createTempShell { Shell shell ->
      def dist = shell.mkdirs('/dist')

      // building a package with a readme at the root and a another file in a subdirectory..
      // we will make sure that the packaging process does not affect anything else
      def zkDistPackage = dist.createRelative('org.linkedin.zookeeper-server-x.y.z')
      shell.saveContent(zkDistPackage.createRelative('README.md'), "this is the readme")
      shell.saveContent(zkDistPackage.createRelative('lib/acme.jar'), "this is the jar")


      def packager = new ZooKeeperClusterPackager(shell: shell,
                                                  configRoot: createZooKeeperTemplates(shell),
                                                  zookeperServers: [[host: 'h2'], [clientPort: 2183, quorumPort: 2889, leaderElectionPort: 3889]],
                                                  inputPackage: zkDistPackage,
                                                  outputFolder: shell.mkdirs('/out'))

      List<PackagedArtifact> zkps = packager.createPackages() as List
      assertEquals(2, zkps.size())

      // server 1 -> h2-2181
      PackagedArtifact zkp = zkps[0]

      assertEquals("/out/org.linkedin.zookeeper-server-x.y.z-h2-2181", zkp.location.path)
      assertEquals('h2', zkp.host)
      assertEquals(2181, zkp.port)

      def expectedResources =
        [
          '/README.md': 'this is the readme',
          '/data': DIRECTORY,
          '/data/myid': '1',
          '/lib': DIRECTORY,
          '/lib/acme.jar': 'this is the jar',
          '/conf': DIRECTORY,
          '/conf/zoo.cfg': '[clientPort:2181];[host:h2];[[host:h2], [clientPort:2183, quorumPort:2889, leaderElectionPort:3889]]'
        ]

      checkPackageContent(expectedResources, zkp.location)

      // server 2 -> localhost-2183
      zkp = zkps[1]

      assertEquals("/out/org.linkedin.zookeeper-server-x.y.z-localhost-2183", zkp.location.path)
      assertEquals('localhost', zkp.host)
      assertEquals(2183, zkp.port)

      expectedResources =
        [
          '/README.md': 'this is the readme',
          '/data': DIRECTORY,
          '/data/myid': '2',
          '/lib': DIRECTORY,
          '/lib/acme.jar': 'this is the jar',
          '/conf': DIRECTORY,
          '/conf/zoo.cfg': '[clientPort:2181];[clientPort:2183, quorumPort:2889, leaderElectionPort:3889];[[host:h2], [clientPort:2183, quorumPort:2889, leaderElectionPort:3889]]'
        ]

      checkPackageContent(expectedResources, zkp.location)
    }
  }

  public void testActualTemplates()
  {
    ShellImpl.createTempShell { Shell shell ->

      assertEquals(2, copyConfigs(shell, 'zookeeper'))

      def out = shell.mkdirs('/out')

      assertEquals("12", shell.cat(shell.processTemplate('/templates/myid.gtmpl', out, [id: 12])))

      def defaultContent = """
# The number of milliseconds of each tick
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
"""
      checkContent(defaultContent, shell, 'zoo.cfg.gtmpl',
                   [
                     opts: [:],
                     zk: [:],
                     // if only 1 server => no server section at the bottom
                     allServers: []
                   ]
      )

      def contentWith2Servers = """
# The number of milliseconds of each tick
tickTime=4000
# The number of ticks that the initial
# synchronization phase can take
initLimit=10
# The number of ticks that can pass between
# sending a request and getting an acknowledgement
syncLimit=5
# the directory where the snapshot is stored.
dataDir=data
# the port at which the clients will connect
clientPort=2282

server.1=h1:12888:13888

server.2=h2:12889:13889
"""

      checkContent(contentWith2Servers, shell, 'zoo.cfg.gtmpl',
                   [
                     opts: [tickTime: 4000],
                     zk: [clientPort: 2282],
                     // if only 1 server => no server section at the bottom
                     allServers: [[clientPort: 2282, host: 'h1', quorumPort: 12888, leaderElectionPort: 13888],
                       [clientPort: 2282, host: 'h2', quorumPort: 12889, leaderElectionPort: 13889]]
                   ]
      )
    }
  }

  private Resource createZooKeeperTemplates(Shell shell)
  {
    def templates = shell.mkdirs('/configs')

    // YP Note: those are groovy templates hence the single quote!

    shell.saveContent(templates.createRelative("/conf/myid.gtmpl"), '${id}')
    shell.saveContent(templates.createRelative("/conf/zoo.cfg.gtmpl"), '${opts};${zk};${allServers}')

    return templates
  }

}