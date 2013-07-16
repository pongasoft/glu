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
import org.pongasoft.glu.packaging.setup.GluPackager

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
                                     configTemplatesRoots: copyConfigs(shell.toResource('/configs')),
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

  public void testCompress()
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
                                     configTemplatesRoots: copyConfigs(shell.toResource('/configs')),
                                     packagesRoot: packagesRoot,
                                     outputFolder: shell.mkdirs('/out'),
                                     keysRoot: keysRootResource,
                                     gluMetaModel: testModel,
                                     dryMode: false,
                                     compress: true)

      packager.packageAll()

      def agent = "org.linkedin.glu.agent-server-agent-1-tutorialZooKeeperCluster-${GLU_VERSION}"
      def agentUpgrade = "org.linkedin.glu.agent-server-agent-1-tutorialZooKeeperCluster-upgrade-${GLU_VERSION}"
      def agentCli = "org.linkedin.glu.agent-cli-${GLU_VERSION}"
      def consoleCli = "org.linkedin.glu.console-cli-${GLU_VERSION}"
      def console = "org.linkedin.glu.console-server-tutorialConsole-${GLU_VERSION}"
      def zkc = 'zookeeper-clusters/zookeeper-cluster-tutorialZooKeeperCluster'
      def zks = "org.linkedin.zookeeper-server-${ZOOKEEPER_VERSION}"

      boolean bulkFailure = true

      // make sure
      def expectedResources =
        [
          "/agent-cli": DIRECTORY,
          "/agent-cli/${agentCli}.tgz": tarContent(shell, [
            "/${agentCli}": DIRECTORY,
            "/${agentCli}/README.md": "this is the readme",
            "/${agentCli}/conf": DIRECTORY,
            "/${agentCli}/conf/clientConfig.properties": FILE,
            "/${agentCli}/conf/keys": DIRECTORY,
            "/${agentCli}/conf/keys/agent.truststore": FILE,
            "/${agentCli}/conf/keys/console.keystore": FILE,
          ], bulkFailure),
          "/agents": DIRECTORY,
          "/agents/${agent}.tgz": tarContent(shell, [
            "/${agent}": DIRECTORY,
            "/${agent}/README.md": "this is the readme",
            "/${agent}/version.txt": GLU_VERSION,
            "/${agent}/${GLU_VERSION}": DIRECTORY,
            "/${agent}/${GLU_VERSION}/conf": DIRECTORY,
            "/${agent}/${GLU_VERSION}/conf/agentConfig.properties": FILE,
            "/${agent}/${GLU_VERSION}/conf/pre_master_conf.sh": FILE,
          ], bulkFailure),
          "/agents/${agentUpgrade}.tgz": tarContent(shell, [
            "/conf": DIRECTORY,
            "/conf/agentConfig.properties": FILE,
            "/conf/pre_master_conf.sh": FILE,
          ], bulkFailure),
          "/console-cli": DIRECTORY,
          "/console-cli/${consoleCli}.tgz": tarContent(shell, [
            "/${consoleCli}": DIRECTORY,
            "/${consoleCli}/README.md": "this is the readme",
          ], bulkFailure),
          "/consoles": DIRECTORY,
          "/consoles/${console}.tgz": tarContent(shell, [
            "/${console}": DIRECTORY,
            "/${console}/README.md": "this is the readme",
            "/${console}/conf": DIRECTORY,
            "/${console}/conf/glu-console-webapp.groovy": FILE,
            "/${console}/conf/pre_master_conf.sh": FILE,
            "/${console}/keys": DIRECTORY,
            "/${console}/keys/agent.truststore": FILE,
            "/${console}/keys/console.keystore": FILE,
            "/${console}/${jettyDistribution}": DIRECTORY,
            "/${console}/${jettyDistribution}/contexts": DIRECTORY,
            "/${console}/${jettyDistribution}/contexts/console-jetty-context.xml": FILE,
            "/${console}/${jettyDistribution}/contexts/glu-jetty-context.xml": FILE,
          ], bulkFailure),
          '/zookeeper-clusters': DIRECTORY,
          "/${zkc}": DIRECTORY,
          "/${zkc}/${zks}.tgz": tarContent(shell, [
            "/${zks}": DIRECTORY,
            "/${zks}/README.md": "this is the readme",
            "/${zks}/conf": DIRECTORY,
            "/${zks}/conf/zoo.cfg": FILE,
            "/${zks}/bin": DIRECTORY,
            "/${zks}/bin/zookeeperctl.sh": FILE,
          ], bulkFailure),
          "/${zkc}/conf": DIRECTORY,
          "/${zkc}/conf/org": DIRECTORY,
          "/${zkc}/conf/org/glu": DIRECTORY,
          "/${zkc}/conf/org/glu/agents": DIRECTORY,
          "/${zkc}/conf/org/glu/agents/names": DIRECTORY,
          "/${zkc}/conf/org/glu/agents/names/agent-1": DIRECTORY,
          "/${zkc}/conf/org/glu/agents/names/agent-1/fabric": 'glu-dev-1',
          "/${zkc}/conf/org/glu/agents/fabrics": DIRECTORY,
          "/${zkc}/conf/org/glu/agents/fabrics/glu-dev-1": DIRECTORY,
          "/${zkc}/conf/org/glu/agents/fabrics/glu-dev-1/config": DIRECTORY,
          "/${zkc}/conf/org/glu/agents/fabrics/glu-dev-1/config/config.properties": FILE,
          "/${zkc}/conf/org/glu/agents/fabrics/glu-dev-1/config/agent.keystore": FILE,
          "/${zkc}/conf/org/glu/agents/fabrics/glu-dev-1/config/console.truststore": FILE,
        ]

      checkPackageContent(expectedResources, shell.toResource('/out'), bulkFailure)
    }
  }
}