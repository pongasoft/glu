package test.setup

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.glu.groovy.utils.shell.ShellImpl
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.packaging.setup.ZooKeeperPackager

/**
 * @author yan@pongasoft.com  */
public class TestZooKeeperPackager extends GroovyTestCase
{
  public static Object DIRECTORY = new Object()

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


      def packager = new ZooKeeperPackager(shell: shell,
                                           templatesRoot: createZooKeeperTemplates(shell),
                                           zookeperServers: [[host: 'h2']],
                                           inputPackage: zkDistPackage,
                                           outputFolder: shell.mkdirs('/out'))

      Collection<Resource> zkps = packager.createPackages()

      assertEquals(1, zkps.size())
      Resource zkp = zkps.iterator().next()

      assertEquals("/out/org.linkedin.zookeeper-server-x.y.z-h2-2181", zkp.path)

      def expectedResources =
        [
          '/README.md': 'this is the readme',
          '/lib': DIRECTORY,
          '/lib/acme.jar': 'this is the jar',
          '/conf': DIRECTORY,
          '/conf/zoo.cfg': '[clientPort:2181];[host:h2];[]'
        ]

      checkPackageContent(expectedResources, zkp)
    }
  }

  public void testCreatePackage3()
  {
    ShellImpl.createTempShell { Shell shell ->
      def dist = shell.mkdirs('/dist')

      // building a package with a readme at the root and a another file in a subdirectory..
      // we will make sure that the packaging process does not affect anything else
      def zkDistPackage = dist.createRelative('org.linkedin.zookeeper-server-x.y.z')
      shell.saveContent(zkDistPackage.createRelative('README.md'), "this is the readme")
      shell.saveContent(zkDistPackage.createRelative('lib/acme.jar'), "this is the jar")


      def packager = new ZooKeeperPackager(shell: shell,
                                           templatesRoot: createZooKeeperTemplates(shell),
                                           zookeperServers: [[host: 'h2'], [clientPort: 2183, quorumPort: 2889, leaderElectionPort: 3889]],
                                           inputPackage: zkDistPackage,
                                           outputFolder: shell.mkdirs('/out'))

      List<Resource> zkps = packager.createPackages() as List
      assertEquals(2, zkps.size())

      // server 1 -> h2-2181
      Resource zkp = zkps[0]

      assertEquals("/out/org.linkedin.zookeeper-server-x.y.z-h2-2181", zkp.path)

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

      checkPackageContent(expectedResources, zkp)

      // server 2 -> localhost-2183
      zkp = zkps[1]

      assertEquals("/out/org.linkedin.zookeeper-server-x.y.z-localhost-2183", zkp.path)

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

      checkPackageContent(expectedResources, zkp)
    }
  }

  private Resource createZooKeeperTemplates(Shell shell)
  {
    def templates = shell.mkdirs('/templates')

    // YP Note: those are groovy templates hence the single quote!

    shell.saveContent(templates.createRelative("/myid.gtmpl"), '${id}')
    shell.saveContent(templates.createRelative("/zoo.cfg.gtmpl"), '${opts};${zk};${allServers}')

    return templates
  }

  private void checkPackageContent(def expectedResources, Resource pkgRoot)
  {
    GroovyIOUtils.eachChildRecurse(pkgRoot.chroot('.')) { Resource r ->
      def expectedValue = expectedResources.remove(r.path)
      if(expectedValue == null)
        fail("unexpected resource ${r}")

      if(expectedValue.is(DIRECTORY))
        assertTrue("${r} is directory", r.isDirectory())
      else
        assertEquals(expectedValue, r.file.text)
    }

    assertTrue("${expectedResources} is not empty", expectedResources.isEmpty())
  }
}