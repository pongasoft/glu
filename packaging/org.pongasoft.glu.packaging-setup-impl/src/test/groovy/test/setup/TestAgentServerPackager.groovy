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
import org.pongasoft.glu.packaging.setup.AgentServerPackager
import org.pongasoft.glu.packaging.setup.PackagedArtifact
import org.pongasoft.glu.provisioner.core.metamodel.AgentMetaModel

/**
 * @author yan@pongasoft.com  */
public class TestAgentServerPackager extends BasePackagerTest
{
  public void testTutorialModel()
  {
    ShellImpl.createTempShell { Shell shell ->

      def inputPackage = shell.mkdirs("/dist/org.linkedin.glu.agent-server-${GLU_VERSION}")

      shell.saveContent(inputPackage.createRelative('version.txt'), GLU_VERSION)
      shell.saveContent(inputPackage.createRelative("${GLU_VERSION}/lib/acme.jar"), "this is the jar")

      def packager = new AgentServerPackager(packagerContext: createPackagerContext(shell),
                                             outputFolder: shell.mkdirs('/out'),
                                             inputPackage: inputPackage,
                                             configRoot: copyConfigs(shell.toResource('/configs')),
                                             metaModel: testModel.agents[0])

      PackagedArtifact artifact = packager.createPackage()

      assertEquals(shell.toResource("/out/org.linkedin.glu.agent-server-${GLU_VERSION}-agent-1"), artifact.location)
      assertEquals('localhost', artifact.host)
      assertEquals(AgentMetaModel.DEFAULT_PORT, artifact.port)

      def expectedResources =
        [
          '/version.txt': GLU_VERSION,
          "/${GLU_VERSION}": DIRECTORY,
          "/${GLU_VERSION}/lib": DIRECTORY,
          "/${GLU_VERSION}/lib/acme.jar": 'this is the jar',
          "/${GLU_VERSION}/conf": DIRECTORY,
          "/${GLU_VERSION}/conf/agentConfig.properties": DEFAULT_AGENT_CONFIG_PROPERTIES,
          "/${GLU_VERSION}/conf/pre_master_conf.sh": """#!/bin/bash

#
# Copyright (c) 2013 Yan Pujante
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#


GLU_ZOOKEEPER="127.0.0.1:2181"

GLU_AGENT_NAME="agent-1"

""",
        ]

      checkPackageContent(expectedResources, artifact.location)
    }
  }

  public void testPorts()
  {
    ShellImpl.createTempShell { Shell shell ->
      def inputPackage = shell.mkdirs("/dist/org.linkedin.glu.agent-server-${GLU_VERSION}")
      shell.saveContent(inputPackage.createRelative('version.txt'), GLU_VERSION)
      shell.saveContent(inputPackage.createRelative("${GLU_VERSION}/lib/acme.jar"), "this is the jar")

      def metaModel = """
fabrics['f1'] = [
  zooKeeperCluster: 'zkc'
]

agents << [
  host: 'ha',
  fabric: 'f1',
  ports: [
    mainPort: 12345,
    configPort: 54321
  ],
  version: '${GLU_VERSION}'
]

zooKeeperClusters << [
  name: 'zkc',
  zooKeepers: [
    [
      version: '${ZOOKEEPER_VERSION}',
      host: 'hz',
      port: 23456
    ]
  ],
]

"""

      def packager = new AgentServerPackager(packagerContext: createPackagerContext(shell),
                                             outputFolder: shell.mkdirs('/out'),
                                             inputPackage: inputPackage,
                                             configRoot: copyConfigs(shell.toResource('/configs')),
                                             metaModel: toGluMetaModel(metaModel).agents[0])

      PackagedArtifact artifact = packager.createPackage()

      assertEquals(shell.toResource("/out/org.linkedin.glu.agent-server-${GLU_VERSION}-12345"), artifact.location)
      assertEquals('ha', artifact.host)
      assertEquals(12345, artifact.port)

      def expectedResources =
        [
          '/version.txt': GLU_VERSION,
          "/${GLU_VERSION}": DIRECTORY,
          "/${GLU_VERSION}/lib": DIRECTORY,
          "/${GLU_VERSION}/lib/acme.jar": 'this is the jar',
          "/${GLU_VERSION}/conf": DIRECTORY,
          "/${GLU_VERSION}/conf/agentConfig.properties": DEFAULT_AGENT_CONFIG_PROPERTIES.replace('glu.agent.rest.nonSecure.port=12907', 'glu.agent.rest.nonSecure.port=54321'),
          "/${GLU_VERSION}/conf/pre_master_conf.sh": """#!/bin/bash

#
# Copyright (c) 2013 Yan Pujante
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#


GLU_ZOOKEEPER="hz:23456"

GLU_AGENT_PORT="12345"

""",
        ]

      checkPackageContent(expectedResources, artifact.location)
    }

  }

  public static final def DEFAULT_AGENT_CONFIG_PROPERTIES = """#
# Copyright (c) 2010-2010 LinkedIn, Inc
# Portions Copyright (c) 2011-2013 Yan Pujante
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License. You may obtain a copy of
# the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#

# this directory is the space where the agent should be allowed to write into
# in order to install the software
glu.agent.scriptRootDir=\${glu.agent.apps}

# the directory where the data (non version specific) is stored
glu.agent.dataDir=\${glu.agent.homeDir}/data

# the directory where the logs (non version specific) is stored
glu.agent.logDir=\${glu.agent.dataDir}/logs

# This is the temporary directory for the agent
glu.agent.tempDir=\${glu.agent.dataDir}/tmp

# This directory will contain the state of the scripts
glu.agent.scriptStateDir=\${glu.agent.dataDir}/scripts/state

# the port which exports the REST api (used for bootstrap only)
glu.agent.rest.nonSecure.port=12907

# this file will store the properties used so that they become 'default' values for the next run
glu.agent.persistent.properties=\${glu.agent.dataDir}/config/agent.properties

################################
# Commands configuration

# is commands feature enabled?
glu.agent.features.commands.enabled=true

# currently 1 value: filesystem
glu.agent.commands.storageType=filesystem

# the directory where the commands (non version specific) are stored (if filesystem) (if undefined => use temp folder)
glu.agent.commands.filesystem.dir=\${glu.agent.dataDir}/commands

################################
# ZooKeeper configuration:

# session timeout (ex: 2s)
glu.agent.zkSessionTimeout=5s

# The file which contains the location of zookeeper
glu.agent.zkProperties=\${glu.agent.dataDir}/config/zk.properties

################################
# The version
glu.agent.version=\${org.linkedin.app.version}

################################
# The rest of the config
glu.agent.configURL=zookeeper:\${glu.agent.zookeeper.root}/agents/fabrics/\${glu.agent.fabric}/config/config.properties"""
}