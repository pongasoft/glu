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
import org.pongasoft.glu.provisioner.core.metamodel.AgentMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ConsoleMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperClusterMetaModel
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

  def packagedArtifacts = [:]

  boolean dryMode = false

  def packageAll()
  {
    packageAgents()
    packageConsoles()
    packageZooKeeperClusters()
    packageAgentCli()
    packageConsoleCli()

    return packagedArtifacts
  }

  void packageAgents()
  {
    def checksums = [:]

    gluMetaModel.agents.each { AgentMetaModel model ->
      AgentServerPackager packager = buildPackager(model)

      def pas = packager.computePackagedArtifacts()

      def checksum = computeChecksum(pas.agentServer)

      def packageName = pas.agentServer.location.filename
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
            log.info "Skipped agent package ${pas.agentServer.location} => ${pas.agentServer.host}:${pas.agentServer.port}"
          }
        }
      }
      else
      {
        packager.createPackage()
        if(!dryMode)
          log.info "Generated agent package ${pas.agentServer.location} => ${pas.agentServer.host}:${pas.agentServer.port}"
        checksums[packageName] = checksum
      }

      packagedArtifacts[model] = pas
    }
  }

  void packageConsoles()
  {
    gluMetaModel.consoles.values().each { ConsoleMetaModel model ->
      PackagedArtifact pa = packageConsole(model)
      packagedArtifacts[model] = pa
      if(!dryMode)
        log.info "Generated console package ${pa.location} => ${pa.host}:${pa.port}"
    }
  }

  void packageZooKeeperClusters()
  {
    gluMetaModel.zooKeeperClusters.values().each { ZooKeeperClusterMetaModel model ->
      def pas = packageZooKeeperCluster(model)
      packagedArtifacts[model] = pas
      if(!dryMode)
      {
        pas.zooKeepers.each { zki ->
          log.info "Generated ZooKeeper instance ${zki.location}  => ${zki.host}:${zki.port}"
        }
        log.info "Generated ZooKeeper cluster ${pas.zooKeeperCluster.location} => ${model.zooKeeperConnectionString}"
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

  protected PackagedArtifact packageConsole(ConsoleMetaModel consoleMetaModel)
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
    packager.createPackage()
  }

  protected def packageZooKeeperCluster(ZooKeeperClusterMetaModel zooKeeperClusterMetaModel)
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
    packager.createPackage()
  }

  protected PackagedArtifact packageAgentCli()
  {
    def out = shell.mkdirs(outputFolder.createRelative('agent-cli'))
    def packager =
      new AgentCliPackager(packagerContext: createPackagerContext(),
                           outputFolder: out,
                           inputPackage: getInputPackage('org.linkedin.glu.agent-cli',
                                                         gluMetaModel.gluVersion),
                           configsRoots: configsRoots,
                           metaModel: gluMetaModel,
                           dryMode: dryMode)

    PackagedArtifact pa = packager.createPackage()

    if(!packagedArtifacts[gluMetaModel])
      packagedArtifacts[gluMetaModel] = [:]

    packagedArtifacts[gluMetaModel].agentCli = pa

    if(!dryMode)
      println "Generated agent-cli package ${pa.location}"

    return pa
  }

  protected PackagedArtifact packageConsoleCli()
  {
    def out = shell.mkdirs(outputFolder.createRelative('console-cli'))
    def packager =
      new ConsoleCliPackager(packagerContext: createPackagerContext(),
                             outputFolder: out,
                             inputPackage: getInputPackage('org.linkedin.glu.console-cli',
                                                           gluMetaModel.gluVersion),
                             configsRoots: configsRoots,
                             metaModel: gluMetaModel,
                             dryMode: dryMode)

    PackagedArtifact pa = packager.createPackage()

    if(!packagedArtifacts[gluMetaModel])
      packagedArtifacts[gluMetaModel] = [:]

    packagedArtifacts[gluMetaModel].consoleCli = pa

    if(!dryMode)
      println "Generated console-cli package ${pa.location}"

    return pa
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