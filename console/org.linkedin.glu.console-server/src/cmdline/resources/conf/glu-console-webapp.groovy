/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Copyright (c) 2011 Yan Pujante
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
      dashboard:
      [
          mountPoint: [checked: true, name: 'mountPoint', groupBy: true, linkFilter: true],
          agent: [checked: true, name: 'agent', groupBy: true],
          'tag': [checked: false, name: 'tag', groupBy: true, linkFilter: true],
          'tags': [checked: true, name: 'tags', linkFilter: true],
          'metadata.container.name': [checked: true, name: 'container', groupBy: true, linkFilter: true],
          'metadata.version': [checked: true, name: 'version', groupBy: true],
          'metadata.product': [checked: true, name: 'product', groupBy: true, linkFilter: true],
          'metadata.cluster': [checked: true, name: 'cluster', groupBy: true, linkFilter: true],
          'initParameters.skeleton': [checked: false, name: 'skeleton', groupBy: true],
          script: [checked: false, name: 'script', groupBy: true],
          'metadata.modifiedTime': [checked: false, name: 'Last Modified', groupBy: false],
          status: [checked: true, name: 'status', groupBy: true]
      ],

    tags:
    [
      'webapp': [background: '#ec7000', color: '#fff0e1'],
      'frontend': [background: '#006633', color: '#f1f5ec'],
      'backend': [background: '#5229a3', color: '#e0d5f9'],
    ],

      system:
      [
          agent: [name: 'agent'],
        'tags.webapp': [name: 'webapp'],
          'metadata.container.name': [name: 'container'],
          'metadata.product': [name: 'product'],
          'metadata.version': [name: 'version'],
          'metadata.cluster': [name: 'cluster']
      ],

      model:
      [
          [
              name: 'product',
              header: ['version']
          ]
      ],

      header:
      [
          metadata: ['drMode']
      ],

      fabrics:
      [
          'glu-dev-1': ['drMode': 'primary'],
          'glu-dev-2': ['drMode': 'secondary']
      ],
  ]
