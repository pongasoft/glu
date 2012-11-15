/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

def dataSourceUrl =
  System.properties['org.linkedin.glu.console.dataSource.url'] ?:
  "jdbc:hsqldb:file:${System.properties['user.dir']}/database/prod;shutdown=true"

// specify the database connection string
dataSource.dbCreate = "update"
dataSource.url = dataSourceUrl

grails.serverURL = "http://${InetAddress.getLocalHost().canonicalHostName}:8080/console"

// path to the root of the unrestricted location (empty means no restriction)
// this property is used by StreamFileContentPlugin
plugins.StreamFileContentPlugin.unrestrictedLocation = ''

// role for unrestricted
plugins.StreamFileContentPlugin.unrestrictedRole = 'ADMIN'

// define the plugins as a Map, or a class name or an array of class names
orchestration.engine.plugins = [
  'org.linkedin.glu.orchestration.engine.plugins.builtin.StreamFileContentPlugin'
]

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

/******************************************************
 * This is how to configure ldap
 ******************************************************/
//ldap.server.url="ldaps://ldap.host:3269"
//ldap.search.base="dc=gluos"
//ldap.search.user="cn=Artifactoryr,ou=Pseudo-Users,dc=gluos"
//ldap.search.pass="*****"
//ldap.username.attribute="sAMAccountName"


console.defaults =
  [
      // customCss can be a String (use """ notation in order to make it easy) or a URI
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
    // features that can be turned on and off
    features:
    [
      commands: true
    ],

  ]
