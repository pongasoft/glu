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

import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.provisioner.core.metamodel.AgentMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.MetaModel

/**
 * @author yan@pongasoft.com  */
public class AgentServerPackager extends BasePackager
{
  public static final String DEFAULT_AGENT_HOST = '*' // any host

  public static final def ENV_PROPERTY_NAMES = [
    "GLU_CONFIG_PREFIX",
    "GLU_ZOOKEEPER",
    "GLU_AGENT_NAME",
    "GLU_AGENT_TAGS",
    "GLU_AGENT_HOSTNAME_FACTORY",
    "GLU_AGENT_PORT",
    "GLU_AGENT_ADDRESS",
    "GLU_AGENT_FABRIC",
    "GLU_AGENT_APPS",
    "GLU_AGENT_ZOOKEEPER_ROOT",
    "APP_NAME",
    "APP_VERSION",
    "JAVA_HOME",
    "JAVA_CMD",
    "JAVA_CMD_TYPE",
    "JVM_CLASSPATH",
    "JVM_SIZE",
    "JVM_SIZE_NEW",
    "JVM_SIZE_PERM",
    "JVM_GC_TYPE",
    "JVM_GC_OPTS",
    "JVM_GC_LOG",
    "JVM_LOG4J",
    "JVM_TMP_DIR",
    "JVM_XTRA_ARGS",
    "JVM_DEBUG",
    "JVM_APP_INFO",
    "MAIN_CLASS",
    "MAIN_CLASS_ARGS",
  ]

  AgentMetaModel metaModel

  private Map<MetaModel, PackagedArtifact> _packagedArtifacts = null
  private Map<String, Object> _tokens = null

  Map<String, Object> getConfigTokens()
  {
    metaModel.configTokens
  }

  Map<MetaModel, PackagedArtifact> computePackagedArtifacts()
  {
    if(!_packagedArtifacts)
    {
      String packageName = ensureVersion(metaModel.version)

      _tokens = [*:configTokens]

      String agentName = metaModel.name
      int agentPort = metaModel.agentPort

      def agentHost

      switch(configTokens.GLU_AGENT_HOSTNAME_FACTORY)
      {
        case ':ip':
        case ':canonical':
        case null:
          agentHost = '*'
          break;

        default:
          agentHost = configTokens.GLU_AGENT_HOSTNAME_FACTORY
      }

      def parts = [packageName]
      if(agentName)
      {
        parts << agentName
        _tokens.GLU_AGENT_NAME = agentName
      }
      if(agentHost != DEFAULT_AGENT_HOST)
        parts << agentHost
      if(agentPort != AgentMetaModel.DEFAULT_PORT)
      {
        parts << agentPort
        _tokens.GLU_AGENT_PORT = agentPort
      }
      if(configTokens.GLU_AGENT_FABRIC)
        parts << configTokens.GLU_AGENT_FABRIC

      if(metaModel.gluMetaModel.zooKeeperRoot != GluMetaModel.DEFAULT_ZOOKEEPER_ROOT)
        _tokens.GLU_AGENT_ZOOKEEPER_ROOT = metaModel.gluMetaModel.zooKeeperRoot

      // Define which zookeeper to use
      _tokens.GLU_ZOOKEEPER =
        metaModel.fabric.zooKeeperCluster.zooKeeperConnectionString

      parts << metaModel.fabric.zooKeeperCluster.name

      def upgradeParts = [*parts]

      upgradeParts << 'upgrade'


      parts << metaModel.version
      upgradeParts << metaModel.version

      Resource packagePath = outputFolder.createRelative(parts.join('-'))
      Resource upgradePackagePath = outputFolder.createRelative(upgradeParts.join('-'))

      _packagedArtifacts = [:]

      _packagedArtifacts[metaModel] =
        new PackagedArtifact(location: packagePath,
                             host: metaModel.host.resolveHostAddress(),
                             port: agentPort,
                             tokens: _tokens)

      _packagedArtifacts[metaModel.agentUpgrade] =
        new PackagedArtifact(location: upgradePackagePath,
                             host: metaModel.host.resolveHostAddress(),
                             port: agentPort,
                             tokens: _tokens)
    }

    return _packagedArtifacts
  }

  @Override
  Map<MetaModel, PackagedArtifact> createPackages()
  {
    def packagedArtifacts = computePackagedArtifacts()

    def tokens = [
      agentMetaModel: metaModel,
      envPropertyNames: ENV_PROPERTY_NAMES,
    ]

    tokens[PACKAGER_CONTEXT_KEY] = packagerContext
    tokens[CONFIG_TOKENS_KEY] = [*:_tokens]

    if(!dryMode)
    {
      copyInputPackage(packagedArtifacts[metaModel].location)
      Resource packagePath = configure(packagedArtifacts[metaModel].location, tokens)
      Resource serverRoot = packagePath.createRelative(metaModel.version)

      if(metaModel.gluMetaModel.stateMachine)
        generateStateMachineJarFile(metaModel.gluMetaModel.stateMachine,
                                    serverRoot.createRelative('lib'))

      shell.delete(packagedArtifacts[metaModel.agentUpgrade].location)
      shell.cp(serverRoot, packagedArtifacts[metaModel.agentUpgrade].location)
    }

    return packagedArtifacts
  }

  Resource configure(Resource packagePath, Map tokens)
  {
    processConfigs('agent-server', tokens, packagePath)

    return packagePath
  }
}