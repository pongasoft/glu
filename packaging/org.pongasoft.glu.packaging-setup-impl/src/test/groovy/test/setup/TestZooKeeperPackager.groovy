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

      Object directory = new Object()

      assertEquals("/out/org.linkedin.zookeeper-server-x.y.z-h2-2181", zkp.path)

      def expectedResources =
        [
          '/README.md': 'this is the readme',
          '/lib': directory,
          '/lib/acme.jar': 'this is the jar',
          '/conf': directory,
          '/conf/zoo.cfg': '[clientPort:2181];[host:h2];[]'
        ]

      GroovyIOUtils.eachChildRecurse(zkp.chroot('.')) { Resource r ->
        println r.path

        def expectedValue = expectedResources.remove(r.path)
        if(expectedValue == null)
          fail("unexpected resource ${r}")

        if(expectedValue.is(directory))
          assertTrue("${r} is directory", r.isDirectory())
        else
          assertEquals(expectedValue, r.file.text)
      }

      assertTrue("${expectedResources} is not empty", expectedResources.isEmpty())
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
}