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

package test.setup

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.glu.groovy.utils.shell.ShellImpl
import org.pongasoft.glu.packaging.setup.ConsoleCliPackager
import org.pongasoft.glu.packaging.setup.GluPackager
import org.pongasoft.glu.packaging.setup.PackagedArtifact

import java.nio.file.Files

/**
 * @author yan@pongasoft.com   */
public class TestGluPackager extends BasePackagerTest
{
  public void testInstallScripts()
  {
    ShellImpl.createTempShell { Shell shell ->

      def packagesRoot = shell.mkdirs("/packages")

      [
        "org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}",
        "org.linkedin.glu.agent-server-${GLU_VERSION}",
        "org.linkedin.glu.console-server-${GLU_VERSION}",
        "org.linkedin.glu.agent-cli-${GLU_VERSION}",
        "org.linkedin.glu.console-cli-${GLU_VERSION}"
      ].each { String pkgName ->
        def inputPackage = shell.mkdirs("/packages/${pkgName}")
        shell.saveContent(inputPackage.createRelative('README.md'), "this is the readme")
      }

      // agent-server
      shell.saveContent("/packages/org.linkedin.glu.agent-server-${GLU_VERSION}/version.txt",
                        GLU_VERSION)

      // console-server
      def jettyDistribution = "jetty-distribution-${JETTY_VERSION}"
      shell.mkdirs("/packages/org.linkedin.glu.console-server-${GLU_VERSION}/${jettyDistribution}")

      def packager = new GluPackager(shell: shell,
                                     configsRoots: copyConfigs(shell.toResource('/configs')),
                                     packagesRoot: packagesRoot,
                                     outputFolder: shell.mkdirs('/out'),
                                     keysRoot: keysRootResource,
                                     gluMetaModel: testModel,
                                     dryMode: false)

      packager.packageAll()

      packager.generateInstallScripts(true)

      shell.ls('/out/bin').each { def r ->
        assertTrue(Files.isExecutable(r.file.toPath()))
      }

      // make sure
      def expectedResources =
        [
          "/install-agents.sh": FILE,
          "/install-consoles.sh": FILE,
          "/install-zookeepers.sh": FILE,
          "/install-agent-cli.sh": FILE,
          "/install-console-cli.sh": FILE,
          "/install-all.sh": FILE,
        ]

      checkPackageContent(expectedResources, shell.toResource('/out/bin'))
    }
  }
}