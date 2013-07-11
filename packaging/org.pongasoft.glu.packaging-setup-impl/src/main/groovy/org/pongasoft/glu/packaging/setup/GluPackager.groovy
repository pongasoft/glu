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

package org.pongasoft.glu.packaging.setup

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.codec.CodecUtils
import org.linkedin.util.codec.HexaCodec
import org.linkedin.util.codec.OneWayCodec
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.linkedin.util.io.resource.Resource
import org.linkedin.util.reflect.ReflectUtils
import org.pongasoft.glu.provisioner.core.metamodel.AgentCliMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.AgentMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.CliMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ConsoleCliMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ConsoleMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.MetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperClusterMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperMetaModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author yan@pongasoft.com  */
public class GluPackager
{
  public static final String MODULE = GluPackager.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static final OneWayCodec SHA1 =
    OneWayMessageDigestCodec.createSHA1Instance('', HexaCodec.INSTANCE)

  private static final JACKSON_CANONICAL_MAPPER = JsonUtils.newJacksonMapper(true)

  GluMetaModel gluMetaModel

  Shell shell

  Collection<Resource> configsRoots
  Resource packagesRoot
  Resource outputFolder
  Resource keysRoot

  Map<MetaModel, PackagedArtifact> packagedArtifacts = [:]

  boolean dryMode = false

  Map<MetaModel, PackagedArtifact> packageAll()
  {
    packageAgents()
    packageConsoles()
    packageZooKeeperClusters()
    packageAgentCli()
    packageConsoleCli()

    return packagedArtifacts
  }

  void generateInstallScripts(boolean generateInstallAllScript)
  {
    def installAllScript = []

    [
      (ZooKeeperMetaModel.class): "zookeepers",
      (AgentMetaModel.class): "agents",
      (ConsoleMetaModel.class): "consoles",
      (AgentCliMetaModel.class): "agent-cli",
      (ConsoleCliMetaModel.class): "console-cli",
    ].each { Class<? extends CliMetaModel> metaModelClass, String name ->

      def installScript = []

      installScript << ("#" * 20) + " [${name}] " + ("#" * 20)
      buildInstallCommands(metaModelClass, installScript)
      installScript << "#" * 20

      generateInstallScript("install-${name}.sh", installScript)

      installAllScript.addAll(installScript)
    }

    if(generateInstallAllScript)
      generateInstallScript("install-all.sh", installAllScript)
  }

  protected Resource generateInstallScript(String name, def lines)
  {
    def content = """#!/bin/bash

if [ -z "\$SCP_CMD" ]; then
  SCP_CMD="scp"
fi

if [ -z "\$SCP_OPTIONS" ]; then
  SCP_OPTIONS="-r"
fi

if [ -z "\$SCP_USER" ]; then
  SCP_USER=\$USER
fi

${lines.join('\n')}

"""
    def installScript = shell.saveContent(outputFolder.createRelative("bin/${name}"), content)
    shell.chmodPlusX(installScript)

    log.info "Generated install script ${installScript.file.canonicalPath}"

    return installScript
  }

  /**
   * Build install commands
   */
  protected <T extends CliMetaModel> void buildInstallCommands(Class<T> metaModelClass,
                                                               def installScript)
  {
    filter(packagedArtifacts, metaModelClass).each { T metaModel,  PackagedArtifact pa ->
      installScript << buildInstallCommand(metaModel, pa)
    }
  }

  /**
   * Build a single install command
   */
  protected String buildInstallCommand(CliMetaModel cli, PackagedArtifact artifact)
  {
    if(artifact.host)
    {
      if(cli.install?.path)
      {
        "\$SCP_CMD \$SCP_OPTIONS \"${artifact.location.file.canonicalPath}\" \"\$SCP_USER@${artifact.host}:${cli.install.path}\""
      }
      else
      {
        "# manually install ${artifact.location.file.canonicalPath} on host ${artifact.host}"
      }
    }
    else
    {
      "# manually install ${artifact.location.file.canonicalPath}"
    }

  }

  /**
   * Filter the map by class
   */
  protected <T extends MetaModel> Map<T, PackagedArtifact> filter(Map<MetaModel, PackagedArtifact> artifacts,
                                                                  Class<T> metaModelClass)
  {
    artifacts.findAll {k, v ->
      ReflectUtils.isSubClassOrInterfaceOf(k.getClass(), metaModelClass)
    } as Map<T, PackagedArtifact>
  }

  void packageAgents()
  {
    def checksums = [:]

    gluMetaModel.agents.each { AgentMetaModel model ->
      AgentServerPackager packager = buildPackager(model)

      Map<MetaModel, PackagedArtifact> pas = packager.computePackagedArtifacts()

      PackagedArtifact agent = pas[model]

      def checksum = computeChecksum(agent)

      def packageName = agent.location.filename
      def previousChecksum = checksums[packageName]

      if(previousChecksum)
      {
        if(previousChecksum != checksum)
          throw new IllegalStateException("Sanity check failed... same package name [${packageName}], " +
                                          "different checksums [${previousChecksum} != ${checksum}]")
        else
        {
          // no need to generate the package!
          if(!dryMode)
          {
            log.info "Skipped agent package ${agent.location} => ${agent.host}:${agent.port}"
          }
        }
      }
      else
      {
        packagedArtifacts.putAll(packager.createPackages())
        displayPackagedArtifact(model, "agent package")
        checksums[packageName] = checksum
      }
    }
  }

  void packageConsoles()
  {
    gluMetaModel.consoles.values().each { ConsoleMetaModel model ->
      Map<MetaModel, PackagedArtifact> pas = packageConsole(model)
      packagedArtifacts.putAll(pas)
      displayPackagedArtifact(model, "console package")
    }
  }

  void packageZooKeeperClusters()
  {
    gluMetaModel.zooKeeperClusters.values().each { ZooKeeperClusterMetaModel model ->
      Map<MetaModel, PackagedArtifact> pas = packageZooKeeperCluster(model)
      packagedArtifacts.putAll(pas)
      model.zooKeepers.each { ZooKeeperMetaModel zkm ->
        displayPackagedArtifact(zkm, "ZooKeeper instance [${zkm.serverIdx}]")
      }
      displayPackagedArtifact(model, "ZooKeeper cluster [${model.name}]")
    }
  }

  protected void displayPackagedArtifact(MetaModel metaModel, String displayName)
  {
    if(!dryMode)
    {
      PackagedArtifact pa = packagedArtifacts[metaModel]
      if(pa)
      {
        String str = "Generated ${displayName} ${pa.location}"
        if(pa.host)
          str = "${str} => ${pa.host}:${pa.port}"
        log.info str
      }
    }
  }

  protected AgentServerPackager buildPackager(AgentMetaModel agentMetaModel)
  {
    def out = shell.mkdirs(outputFolder.createRelative('agents'))
    def packager =
      new AgentServerPackager(packagerContext: createPackagerContext(),
                              outputFolder: out,
                              inputPackage: getInputPackage('org.linkedin.glu.agent-server',
                                                            agentMetaModel.version),
                              configsRoots: configsRoots,
                              metaModel: agentMetaModel,
                              dryMode: dryMode)
    return packager
  }

  protected Map<MetaModel, PackagedArtifact> packageConsole(ConsoleMetaModel consoleMetaModel)
  {
    def out = shell.mkdirs(outputFolder.createRelative('consoles'))
    def packager =
      new ConsoleServerPackager(packagerContext: createPackagerContext(),
                                outputFolder: out,
                                inputPackage: getInputPackage('org.linkedin.glu.console-server',
                                                              consoleMetaModel.version),
                                configsRoots: configsRoots,
                                metaModel: consoleMetaModel,
                                dryMode: dryMode)
    packager.createPackages()
  }

  protected Map<MetaModel, PackagedArtifact> packageZooKeeperCluster(ZooKeeperClusterMetaModel zooKeeperClusterMetaModel)
  {
    def out = shell.mkdirs(outputFolder.createRelative('zookeeper-clusters'))
    def packager =
      new ZooKeeperClusterPackager(packagerContext: createPackagerContext(),
                                   outputFolder: out,
                                   inputPackage: getInputPackage('org.linkedin.zookeeper-server',
                                                                 zooKeeperClusterMetaModel.zooKeepers[0].version),
                                   configsRoots: configsRoots,
                                   metaModel: zooKeeperClusterMetaModel,
                                   dryMode: dryMode)
    packager.createPackages()
  }

  protected void packageAgentCli()
  {
    AgentCliMetaModel agentCliMetaModel = gluMetaModel.agentCli

    def out = shell.mkdirs(outputFolder.createRelative('agent-cli'))
    def packager =
      new AgentCliPackager(packagerContext: createPackagerContext(),
                           outputFolder: out,
                           inputPackage: getInputPackage('org.linkedin.glu.agent-cli',
                                                         gluMetaModel.gluVersion),
                           configsRoots: configsRoots,
                           metaModel: agentCliMetaModel,
                           dryMode: dryMode)

    packagedArtifacts.putAll(packager.createPackages())

    displayPackagedArtifact(agentCliMetaModel, "agent cli package")
  }

  protected void packageConsoleCli()
  {
    ConsoleCliMetaModel consoleCliMetaModel = gluMetaModel.consoleCli

    def out = shell.mkdirs(outputFolder.createRelative('console-cli'))
    def packager =
      new ConsoleCliPackager(packagerContext: createPackagerContext(),
                             outputFolder: out,
                             inputPackage: getInputPackage('org.linkedin.glu.console-cli',
                                                           gluMetaModel.gluVersion),
                             configsRoots: configsRoots,
                             metaModel: consoleCliMetaModel,
                             dryMode: dryMode)

    packagedArtifacts.putAll(packager.createPackages())

    displayPackagedArtifact(consoleCliMetaModel, "console cli package")
  }

  protected PackagerContext createPackagerContext()
  {
    new PackagerContext(shell: shell,
                        keysRoot: keysRoot ?: outputFolder.createRelative('keys'))
  }

  protected Resource getInputPackage(String name, String version)
  {
    def inputPackage = packagesRoot.createRelative("${name}-${version}")

    if(!inputPackage.exists())
      inputPackage = packagesRoot.createRelative("${name}-${version}.tgz")

    if(!inputPackage.exists())
      throw new FileNotFoundException("${inputPackage} does not exist")

    return inputPackage
  }

  protected String computeChecksum(PackagedArtifact packagedArtifact)
  {
    if(packagedArtifact.tokens != null)
    {
      def json = JACKSON_CANONICAL_MAPPER.writeValueAsString(packagedArtifact.tokens)
      return CodecUtils.encodeString(SHA1, json)
    }
    else
      return null
  }
}