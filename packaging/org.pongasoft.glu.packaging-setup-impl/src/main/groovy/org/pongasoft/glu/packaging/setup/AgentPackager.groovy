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

/**
 * @author yan@pongasoft.com  */
public class AgentPackager extends BasePackager
{
  public static final int DEFAULT_AGENT_PORT = 12906
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

  public static final def CONFIG_PROPERTY_NAMES = [
    // from agentConfig.properties
    "glu.agent.scriptRootDir",
    "glu.agent.dataDir",
    "glu.agent.logDir",
    "glu.agent.tempDir",
    "glu.agent.scriptStateDir",
    "glu.agent.rest.nonSecure.port",
    "glu.agent.persistent.properties",
    "glu.agent.features.commands.enabled",
    "glu.agent.commands.storageType",
    "glu.agent.commands.filesystem.dir",
    "glu.agent.zkSessionTimeout",
    "glu.agent.zkProperties",
    "glu.agent.configURL",

    // from zookeeper config (usually defined by glu.agent.configURL)
    "glu.agent.sslEnabled",
    "glu.agent.keystorePath",
    "glu.agent.keystoreChecksum",
    "glu.agent.keystorePassword",
    "glu.agent.keyPassword",
    "glu.agent.truststorePath",
    "glu.agent.truststoreChecksum",
    "glu.agent.truststorePassword",
    "glu.agent.ivySettings"
  ]

  def opts = [:]

  PackagedArtifact createPackage()
  {
    String agentName = opts.GLU_AGENT_NAME
    int agentPort = (opts.GLU_AGENT_PORT ?: DEFAULT_AGENT_PORT) as int

    def agentHost
    switch(opts.GLU_AGENT_HOSTNAME_FACTORY)
    {
      case ':ip':
      case ':canonical':
      case null:
        agentHost = '*'
        break;

      default:
        agentHost = opts.GLU_AGENT_HOSTNAME_FACTORY
    }

    def parts = [packageName]
    if(agentName)
      parts << agentName
    if(agentHost != DEFAULT_AGENT_HOST)
      parts << agentHost
    if(agentPort != DEFAULT_AGENT_PORT)
      parts << agentPort
    if(opts.GLU_AGENT_FABRIC)
      parts << opts.GLU_AGENT_FABRIC
    String newPackageName = parts.join('-')

    Resource packagePath = outputFolder.createRelative(newPackageName)
    copyInputPackage(packagePath)
    configure(packagePath)
    return new PackagedArtifact(location: packagePath,
                                host: agentHost,
                                port: agentPort)
  }

  Resource configure(Resource packagePath)
  {
    String version = packagePath.createRelative('version.txt').file.text

    Resource serverRoot = packagePath.createRelative(version)

    def tokens = [
      envPropertyNames: ENV_PROPERTY_NAMES,
      opts: opts
    ]

    Resource confDir = shell.mkdirs(serverRoot.createRelative('conf'))

    // optionally process templates
    ['agentConfig.properties', 'pre_master_conf.sh', 'post_master_conf.sh'].each { file ->
      ['.xtmpl', '.gtmpl', ''].each { extension ->
        processOptionalTemplate("/${file}${extension}", confDir, tokens)
      }
    }

    return packagePath
  }
}