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
import org.pongasoft.glu.packaging.setup.ConsoleServerPackager
import org.pongasoft.glu.packaging.setup.PackagedArtifact
import org.pongasoft.glu.provisioner.core.metamodel.ConsoleMetaModel

/**
 * @author yan@pongasoft.com  */
public class TestConsoleServerPackager extends BasePackagerTest
{
  public void testTutorialModel()
  {
    ShellImpl.createTempShell { Shell shell ->

      def inputPackage = shell.mkdirs("/dist/org.linkedin.glu.console-server-${GLU_VERSION}")

      def jettyDistribution = "jetty-distribution-${JETTY_VERSION}"
      def jettyPackage = shell.mkdirs(inputPackage.createRelative(jettyDistribution))
      shell.saveContent(jettyPackage.'/lib/acme.jar', 'this is the jar')
      shell.saveContent(jettyPackage.'/contexts/dummy-context.xml', 'will be deleted')
      shell.saveContent(jettyPackage.'/webapps/dummy-webapp.war', 'will be deleted')

      def packager = new ConsoleServerPackager(packagerContext: createPackagerContext(shell),
                                               outputFolder: shell.mkdirs('/out'),
                                               inputPackage: inputPackage,
                                               configTemplatesRoots: copyConfigs(shell.toResource('/configs')),
                                               metaModel: testModel.consoles['tutorialConsole'])

      PackagedArtifact artifact = packager.createPackage()

      assertEquals(shell.toResource("/out/org.linkedin.glu.console-server-tutorialConsole-${GLU_VERSION}"), artifact.location)
      assertEquals('localhost', artifact.host)
      assertEquals(ConsoleMetaModel.DEFAULT_PORT, artifact.port)

      def expectedResources =
        [
          "/conf": DIRECTORY,
          "/conf/glu-console-webapp.groovy": TUTORIAL_GLU_CONSOLE_WEBAPP,
          "/conf/pre_master_conf.sh": """#!/bin/bash

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


JETTY_PORT="8080"

""",
          "/keys": DIRECTORY,
          "/keys/agent.truststore": toBinaryResource(keysRootResource.'agent.truststore'),
          "/keys/console.keystore": toBinaryResource(keysRootResource.'console.keystore'),
          "/${jettyDistribution}": DIRECTORY,
          "/${jettyDistribution}/contexts": DIRECTORY,
          "/${jettyDistribution}/contexts/console-jetty-context.xml": TUTORIAL_CONSOLE_JETTY_CONTEXT,
          "/${jettyDistribution}/contexts/glu-jetty-context.xml": DEFAULT_GLU_JETTY_CONTEXT,
          "/${jettyDistribution}/lib": DIRECTORY,
          "/${jettyDistribution}/lib/acme.jar": 'this is the jar',
          "/glu": DIRECTORY,
          "/glu/repository": DIRECTORY,
          "/glu/repository/exploded-wars": DIRECTORY,
          "/glu/repository/exploded-wars/org.linkedin.glu.console-webapp-${GLU_VERSION}": DIRECTORY,
          "/glu/repository/exploded-wars/org.linkedin.glu.console-webapp-${GLU_VERSION}/WEB-INF": DIRECTORY,
          "/glu/repository/exploded-wars/org.linkedin.glu.console-webapp-${GLU_VERSION}/WEB-INF/web.xml": { r ->
            assertTrue(r.file.text.contains('<session-timeout>30</session-timeout>'))
          },
          "/glu/repository/tgzs": DIRECTORY,
          "/glu/repository/tgzs/${jettyDistribution}.tar.gz": tarContent(shell, [
            "/${jettyDistribution}": DIRECTORY,
            "/${jettyDistribution}/contexts": DIRECTORY,
            "/${jettyDistribution}/contexts/console-jetty-context.xml": TUTORIAL_CONSOLE_JETTY_CONTEXT,
            "/${jettyDistribution}/contexts/glu-jetty-context.xml": DEFAULT_GLU_JETTY_CONTEXT,
            "/${jettyDistribution}/lib": DIRECTORY,
            "/${jettyDistribution}/lib/acme.jar": 'this is the jar',
          ]),
        ]

      checkPackageContent(expectedResources, artifact.location)


    }
  }

  public void testDifferentConfigs()
  {
    ShellImpl.createTempShell { Shell shell ->

      def driverResource = shell.saveContent('/drivers/db-driver.jar', 'this is the driver')
      def plugin1Resource = shell.saveContent('/plugins/plugin1.jar', 'this is the plugin1')
      def plugin2Resource = shell.saveContent('/plugins/plugin2.jar', 'this is the plugin2')

      def metaModel = """
fabrics['f1'] = [
  zooKeeperCluster: 'zkc',
  console: 'default',
  color: 'black'
]

consoles << [
  host: 'h1',
  ports: [
    mainPort: 9090,
    externalPort: 19090
  ],
  internalPath: '/ic',
  externalHost: 'h2',
  externalPath: '/ec',
  plugins: [
    [fqcn: 'p1', classPath: ['${plugin1Resource.toURI()}']],
    [fqcn: 'p2', classPath: ['${plugin2Resource.toURI()}']],
  ],
  version: '${GLU_VERSION}',
  dataSourceDriverUri: '${driverResource.toURI()}',
  configTokens: [
    includeJettyDistribution: false,
    dataSource: '<datasource>',
    JVM_SIZE: '-Xmx555m',
    plugins: '<extra plugin config>',
    ldap: '<ldap config>',
    log4j: '<log4j config>',
    'console.defaults': '<console.defaults config>',
    'console.defaults.override': '<console.defaults.override config>',
    security: '<security config>',
    tuning: '<tuning config>',
    commands: '<commands config>',
    misc: '<misc config>',
    maxFormContentSize: '500k',
    sessionTimeoutInMinutes: 15
  ]
]

zooKeeperClusters << [
  name: 'zkc',
  zooKeeperSessionTimeout: '1m',
  zooKeepers: [
    [
      version: '${ZOOKEEPER_VERSION}',
      host: 'hz',
      port: 23456
    ]
  ],
]

"""

      def inputPackage = shell.mkdirs("/dist/org.linkedin.glu.console-server-${GLU_VERSION}")

      def jettyDistribution = "jetty-distribution-${JETTY_VERSION}"
      def consoleWar = "org.linkedin.glu.console-webapp-${GLU_VERSION}"
      def jettyPackage = shell.mkdirs(inputPackage.createRelative(jettyDistribution))
      shell.saveContent(jettyPackage.'/lib/acme.jar', 'this is the jar')
      shell.saveContent(jettyPackage.'/contexts/dummy-context.xml', 'will be deleted')
      shell.saveContent(jettyPackage.'/webapps/dummy-webapp.war', 'will be deleted')

      def packager = new ConsoleServerPackager(packagerContext: createPackagerContext(shell),
                                               outputFolder: shell.mkdirs('/out'),
                                               inputPackage: inputPackage,
                                               configTemplatesRoots: copyConfigs(shell.toResource('/configs')),
                                               metaModel: toGluMetaModel(metaModel).consoles['default'])

      PackagedArtifact artifact = packager.createPackage()

      assertEquals(shell.toResource("/out/org.linkedin.glu.console-server-${GLU_VERSION}"), artifact.location)
      assertEquals('h1', artifact.host)
      assertEquals(9090, artifact.port)


      def expectedResources =
        [
          "/conf": DIRECTORY,
          "/conf/glu-console-webapp.groovy": '''/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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



console.sslEnabled=false


console.trackerService.zookeeperRoot = '/org/glu'

// dataSource configuration
<datasource>


// specify the fabrics used by this console (at bootstrap)
console.bootstrap.fabrics = [

  [ name: 'f1', zkConnectString: 'hz:23456', zkSessionTimeout: '1m', color: 'black' ],

]

grails.serverURL = "http://h2:19090/ec"

// path to the root of the unrestricted location (empty means no restriction)
// this property is used by StreamFileContentPlugin
plugins.StreamFileContentPlugin.unrestrictedLocation = ''

// role for unrestricted
plugins.StreamFileContentPlugin.unrestrictedRole = 'ADMIN'

// to mask the content of the file (remove passwords when reading log files)
plugins.StreamFileContentPlugin.maskFileContent = true

// define the plugins as a Map, or a class name or an array of class names
orchestration.engine.plugins = [
  'p1',
'p2'
]

// extra plugin configuration
<extra plugin config>

// commands
def commandsDir =
  System.properties['org.linkedin.glu.console.commands.dir'] ?: "${System.properties['user.dir']}/commands"

// storage type supported right now are 'filesystem' and 'memory'
console.commandsService.storageType = 'filesystem'

// when storage is filesystem => where the commands are stored
console.commandsService.commandExecutionIOStorage.filesystem.rootDir = commandsDir

// when storage is memory => how many elements maximum to store (then start evicting...)
console.commandsService.commandExecutionIOStorage.memory.maxNumberOfElements = 25

// The following property limits how many (leaf) steps get executed in parallel during a deployment
// By default (undefined), it is unlimited
// console.deploymentService.deployer.planExecutor.leafExecutorService.fixedThreadPoolSize = 100

log4j = <log4j config>

// ldap configuration
<ldap config>

// console defaults
console.defaults = <console.defaults config>

// console.defaults.override
<console.defaults.override config>

// security
<security config>

// tuning
<tuning config>

// commands
<commands config>

// extra/misc configuration
<misc config>

''',
          "/conf/pre_master_conf.sh": """#!/bin/bash

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


JVM_SIZE="-Xmx555m"

JETTY_PORT="9090"

""",
          "/${jettyDistribution}": DIRECTORY,
          "/${jettyDistribution}/contexts": DIRECTORY,
          "/${jettyDistribution}/contexts/console-jetty-context.xml":
            TUTORIAL_CONSOLE_JETTY_CONTEXT.replace('/console', '/ic').replace('204800', '512000'),
          "/${jettyDistribution}/contexts/glu-jetty-context.xml": DEFAULT_GLU_JETTY_CONTEXT,
          "/${jettyDistribution}/lib": DIRECTORY,
          "/${jettyDistribution}/lib/acme.jar": 'this is the jar',
          "/glu": DIRECTORY,
          "/glu/repository": DIRECTORY,
          "/glu/repository/exploded-wars": DIRECTORY,
          "/glu/repository/exploded-wars/${consoleWar}": DIRECTORY,
          "/glu/repository/exploded-wars/${consoleWar}/WEB-INF": DIRECTORY,
          "/glu/repository/exploded-wars/${consoleWar}/WEB-INF/lib": DIRECTORY,
          "/glu/repository/exploded-wars/${consoleWar}/WEB-INF/lib/db-driver.jar": "this is the driver",
          "/glu/repository/exploded-wars/${consoleWar}/WEB-INF/lib/plugin1.jar": "this is the plugin1",
          "/glu/repository/exploded-wars/${consoleWar}/WEB-INF/lib/plugin2.jar": "this is the plugin2",
          "/glu/repository/exploded-wars/org.linkedin.glu.console-webapp-${GLU_VERSION}/WEB-INF/web.xml": { r ->
            assertTrue(r.file.text.contains('<session-timeout>15</session-timeout>'))
          },
        ]

      checkPackageContent(expectedResources, artifact.location)
    }

  }

  public static final String DEFAULT_GLU_JETTY_CONTEXT = '''<?xml version="1.0"  encoding="ISO-8859-1"?>
<!DOCTYPE Configure PUBLIC "-//Mort Bay Consulting//DTD Configure//EN" "http://jetty.eclipse.org/configure.dtd">

<!--
  ~ Copyright (c) 2010-2010 LinkedIn, Inc
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License"); you may not
  ~ use this file except in compliance with the License. You may obtain a copy of
  ~ the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  ~ WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  ~ License for the specific language governing permissions and limitations under
  ~ the License.
  -->

<Configure class="org.eclipse.jetty.server.handler.ContextHandler">
  <Call class="org.eclipse.jetty.util.log.Log" name="debug"><Arg>Configure glu-jetty-context.xml</Arg></Call>
  <Set name="contextPath">/glu</Set>
  <Set name="resourceBase"><SystemProperty name="org.linkedin.glu.console.root"/>/glu/</Set>
  <Set name="handler">
    <New class="org.eclipse.jetty.server.handler.ResourceHandler">
      <Set name="cacheControl">max-age=3600,public</Set>
    </New>
  </Set>

</Configure>

'''

  public static final String TUTORIAL_CONSOLE_JETTY_CONTEXT = """<?xml version="1.0"  encoding="ISO-8859-1"?>
<!DOCTYPE Configure PUBLIC "-//Mort Bay Consulting//DTD Configure//EN" "http://jetty.eclipse.org/configure.dtd">

<!--
  ~ Copyright (c) 2010-2010 LinkedIn, Inc
  ~ Portions Copyright (c) 2011 Yan Pujante
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License"); you may not
  ~ use this file except in compliance with the License. You may obtain a copy of
  ~ the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
  ~ WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
  ~ License for the specific language governing permissions and limitations under
  ~ the License.
  -->

<Configure class="org.eclipse.jetty.webapp.WebAppContext">
  <Call class="org.eclipse.jetty.util.log.Log" name="debug"><Arg>Configure console-jetty-context.xml</Arg></Call>
  <Set name="contextPath">/console</Set>
  <Set name="war"><SystemProperty name="org.linkedin.glu.console.root"/>/glu/repository/exploded-wars/org.linkedin.glu.console-webapp-${GLU_VERSION}</Set>
  <Set name="tempDirectory"><SystemProperty name="org.linkedin.glu.console.root"/>/tmp</Set>
  <Set name="extraClasspath"><SystemProperty name="org.linkedin.glu.console.plugins.classpath"/></Set>
  <Set name="maxFormContentSize">204800</Set>
</Configure>

"""

  public static final String TUTORIAL_GLU_CONSOLE_WEBAPP = '''/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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



console.sslEnabled=true

def keysDir = System.properties['org.linkedin.glu.console.keys.dir'] ?: "${System.properties['user.dir']}/keys"

console.keystorePath="${keysDir}/console.keystore"
console.keystorePassword = 'nacEn92x8-1'
console.keyPassword = 'nWVxpMg6Tkv'

console.secretkeystorePath="${keysDir}/console.secretkeystore"

console.truststorePath="${keysDir}/agent.truststore"
console.truststorePassword = 'nacEn92x8-1'


console.trackerService.zookeeperRoot = '/org/glu'

// dataSource configuration

def dataSourceUrl =
  System.properties['org.linkedin.glu.console.dataSource.url'] ?:
  "jdbc:hsqldb:file:${System.properties['user.dir']}/database/prod;shutdown=true"
// specify the database connection string
dataSource.dbCreate = "update"
dataSource.url = dataSourceUrl



// specify the fabrics used by this console (at bootstrap)
console.bootstrap.fabrics = [

  [ name: 'glu-dev-1', zkConnectString: '127.0.0.1:2181', zkSessionTimeout: '30s', color: '#005a87' ],

]

grails.serverURL = "http://localhost:8080/console"

// path to the root of the unrestricted location (empty means no restriction)
// this property is used by StreamFileContentPlugin
plugins.StreamFileContentPlugin.unrestrictedLocation = ''

// role for unrestricted
plugins.StreamFileContentPlugin.unrestrictedRole = 'ADMIN'

// to mask the content of the file (remove passwords when reading log files)
plugins.StreamFileContentPlugin.maskFileContent = true

// define the plugins as a Map, or a class name or an array of class names
orchestration.engine.plugins = [
  'org.linkedin.glu.orchestration.engine.plugins.builtin.StreamFileContentPlugin'
]

// extra plugin configuration


// commands
def commandsDir =
  System.properties['org.linkedin.glu.console.commands.dir'] ?: "${System.properties['user.dir']}/commands"

// storage type supported right now are 'filesystem' and 'memory'
console.commandsService.storageType = 'filesystem'

// when storage is filesystem => where the commands are stored
console.commandsService.commandExecutionIOStorage.filesystem.rootDir = commandsDir

// when storage is memory => how many elements maximum to store (then start evicting...)
console.commandsService.commandExecutionIOStorage.memory.maxNumberOfElements = 25

// The following property limits how many (leaf) steps get executed in parallel during a deployment
// By default (undefined), it is unlimited
// console.deploymentService.deployer.planExecutor.leafExecutorService.fixedThreadPoolSize = 100

log4j = {
    appenders {
    file name:'file',
      file:'logs/console.log',
      layout:pattern(conversionPattern: '%d{yyyy/MM/dd HH:mm:ss.SSS} %p [%c{1}] %m%n')
    }

    root {
      info 'file'
      additivity = false
    }

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate'

    info 'grails',
         'org.linkedin'

    //debug 'com.linkedin.glu.agent.tracker', 'com.linkedin.glu.zookeeper.client'

    //trace 'org.hibernate.SQL', 'org.hibernate.type'

    warn   'org.mortbay.log', 'org.restlet.Component.LogService', 'org.restlet'
}


// ldap configuration


// console defaults
console.defaults = [
  // customCss can be a String (use triple quote notation in order to make it easy) or a URI
  customCss: null,

  // set to true if you want the agent links on the dashboard to go to the individual agent
  // page rather than adding a filter
  dashboardAgentLinksToAgent: false,

  dashboard:
    [
      [ name: "mountPoint", source: "mountPoint" ],
      [ name: "agent",      source: "agent"],
      [ name: "tags",       source: "tags",       groupBy: "uniqueVals"],
      [ name: "container",  source: "metadata.container.name"],
      [ name: "version",    source: "metadata.version"],
      [ name: "product",    source: "metadata.product"],
      [ name: "cluster",    source: "metadata.cluster"],
      [ name: "status",     source: "status" ],
      [ name: "statusInfo", source: "statusInfo", groupBy: "vals", visible: false],
      [ name: "state",      source: "state",                       visible: false]
    ],

  tags:
    [
      'webapp': [background: '#ec7000', color: '#fff0e1'],
      'frontend': [background: '#006633', color: '#f1f5ec'],
      'backend': [background: '#5229a3', color: '#e0d5f9'],
    ],

  model:
    [
      agent: [name: 'agent'],
      'tags.webapp': [name: 'webapp'],
      'metadata.container.name': [name: 'container'],
      'metadata.product': [name: 'product'],
      'metadata.version': [name: 'version'],
      'metadata.cluster': [name: 'cluster']
    ],

  shortcutFilters:
    [
      [
        name: 'product',
        source: 'metadata.product',
        header: ['version']
      ]
    ],

// set this value to true if you don't want to allow for model update (the text area)
// meaning you can only load a new model
  disableModelUpdate: false,


  header:
    [
      metadata: ['drMode']
    ],

// map which defines the actions available for a given mountPoint (agents page)
//
// - key is "state of the mountPoint" (meaning, if the state of the mountPoint is "<key>" then
//   display the actions defined by the value)
//   * The key "-" is special and is reserved for the actions to display when the state does
//     not have an entry (in this example, everything besides running).
//   * The key "*" is special and is reserved for the actions to display all the time.
//
// - value is a map defining what to do (ex: bounce, undeploy) as well as extra informations
//
// This example represents the default values used if not defined
/*
mountPointActions: [
  running: [
    [planType: "transition", displayName: "Stop", state: "stopped"],
    [planType: "bounce", displayName: "Bounce"],
  ],

  // all other states
  "-": [
    [planType: "transition", displayName: "Start", state: "running"],
  ],

  // actions to include for all states
  "*": [
    [planType: "undeploy", displayName: "Undeploy"],
    [planType: "redeploy", displayName: "Redeploy"],
  ]
],
*/

// array which defines the actions available (as well as the order) on the 'Plans' subtab
//
// each item in the array is of the same type as the value in the mountPointActions definition
//
// This example represents the default values used if not defined
/*
plans: [
  [planType: "deploy"],
  [planType: "bounce"],
  [planType: "redeploy"],
  [planType: "undeploy"],
  [planType: "transition", displayName: "Stop", state: "stopped"],
],
*/

  tail: [
    size: '10k', // size to use when tailing a file by default (MemorySize)
    refreshRate: '5s' // how long between polls (Timespan)
  ],

// features that can be turned on and off
  features:
    [
      commands: true
    ],
]


// console.defaults.override


// security


// tuning


// commands


// extra/misc configuration


'''
}