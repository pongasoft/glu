/*
 * Copyright (c) 2012-2013 Yan Pujante
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

package test.agent.server

import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.glu.agent.server.AgentMain
import org.linkedin.util.lifecycle.Destroyable
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.zookeeper.server.StandaloneZooKeeperServer
import org.linkedin.glu.agent.impl.capabilities.ShellImpl
import org.linkedin.glu.agent.api.Shell
import org.linkedin.util.io.PathUtils

/**
 * @author yan@pongasoft.com */
public class AgentForTest implements Destroyable
{
  FileSystemImpl fileSystem
  StandaloneZooKeeperServer zookeeperServer
  AgentMain agentMain
  def urlFactory
  def args = []
  def agentProperties

  def zkClientPort = 2121
  def agentPort = 13906

  def shutdownSequence = []

  void start()
  {
    start(null, null)
  }

  void start(String higherPriorityProperties, String lowerPriorityProperties)
  {
    initFileSystem()
    args << saveProperties(higherPriorityProperties, lowerPriorityProperties)
    agentProperties = AgentMain.staticReadConfig(args[0], new Properties())
    initZooKeeper()
    initAgent()
  }

  def getDefaultAgentProperties()
  {
"""
# base properties on which everything else is built
glu.agent.apps=${fileSystem.toResource("/agent/server/apps").file.canonicalPath }
glu.agent.homeDir=${fileSystem.toResource("/agent/server/home").file.canonicalPath}

glu.agent.scriptRootDir=\${glu.agent.apps}
glu.agent.dataDir=\${glu.agent.homeDir}/data
glu.agent.commandsDir=\${glu.agent.homeDir}/commands
glu.agent.logDir=\${glu.agent.dataDir}/logs
glu.agent.tempDir=\${glu.agent.dataDir}/tmp
glu.agent.scriptStateDir=\${glu.agent.dataDir}/scripts/state
glu.agent.rest.nonSecure.port=12907
glu.agent.persistent.properties=\${glu.agent.dataDir}/config/agent.properties
glu.agent.zkSessionTimeout=5s
glu.agent.version=test
org.linkedin.app.version=test
glu.agent.name=agent-1
glu.agent.fabric=test-fabric
glu.agent.port=${agentPort}
glu.agent.zkConnectString=127.0.0.1:${zkClientPort}
glu.agent.zookeeper.root=/org/glu

glu.agent.features.commands.enabled=true
glu.agent.commands.storageType=filesystem
glu.agent.commands.filesystem.dir=\${glu.agent.dataDir}/commands

# security
glu.agent.sslEnabled=true
glu.agent.keystorePath=${devKeysDir.canonicalPath}/agent.keystore
glu.agent.keystoreChecksum=JSHZAn5IQfBVp1sy0PgA36fT_fD
glu.agent.keystorePassword=nacEn92x8-1
glu.agent.keyPassword=nWVxpMg6Tkv
glu.agent.truststorePath=${devKeysDir.canonicalPath}/console.truststore
glu.agent.truststoreChecksum=qUFMIePiJhz8i7Ow9lZmN5pyZjl
glu.agent.truststorePassword=nacEn92x8-1
""".toString()
  }

  File getDevKeysDir()
  {
    new File("../../dev-keys").canonicalFile
  }

  void initFileSystem()
  {
    fileSystem = FileSystemImpl.createTempFileSystem()

    shutdownSequence << { fileSystem.destroy() }
  }

  void initZooKeeper()
  {
    zookeeperServer = new StandaloneZooKeeperServer(tickTime: 2000,
                                                    clientPort: zkClientPort,
                                                    dataDir: fileSystem.toResource("/zookeeper/server/data").file.canonicalPath)

    zookeeperServer.start()

    shutdownSequence << {
      zookeeperServer.shutdown()
      zookeeperServer.waitForShutdown(100)
    }
  }

  void initAgent()
  {
    agentMain = new AgentMain()
    agentMain.init(args)
    urlFactory = agentMain.urlFactory
    agentMain.start(false)

    shutdownSequence << { agentMain?.stop() }
  }

  void stop()
  {
    agentMain?.stop()
    agentMain = null
  }

  void restart()
  {
    stop()
    urlFactory.reInit()
    agentMain = new AgentMain(urlFactory: urlFactory)
    agentMain.init(args)
    agentMain.start(false)
  }

  @Override
  void destroy()
  {
    GluGroovyLangUtils.onlyOneException(shutdownSequence.reverse())
  }

  def saveProperties(String higherPriorityProperties, String lowerPriorityProperties)
  {
    def resource = fileSystem.toResource("/agent/server/conf/agentConfig.properties")
    fileSystem.withOutputStream(resource) { OutputStream os ->
      if(higherPriorityProperties)
        os << higherPriorityProperties
      os << "\n"
      os << defaultAgentProperties
      os << "\n"
      if(lowerPriorityProperties)
        os << lowerPriorityProperties
    }


    return resource.toURI().toString()
  }

  Shell getShellForScripts()
  {
    agentMain._agent.shellForScripts
  }

  Shell getShellForCommands()
  {
    agentMain._agent.shellForCommands
  }

  Shell getRootShell()
  {
    agentMain._agent._rootShell
  }

  /**
   * Executes a rest call on the agent
   */
  def execRestCall(String path)
  {
    path = PathUtils.addLeadingSlash(path)
    rootShell.exec("curl -k https://localhost:${agentPort}${path} -E ${devKeysDir.canonicalPath}/console.pem")
  }
}