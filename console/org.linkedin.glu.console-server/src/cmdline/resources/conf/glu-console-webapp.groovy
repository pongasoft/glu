console.sslEnabled=true

console.keystorePath="${System.properties['user.dir']}/conf/console.dev.keystore"
console.keystorePassword = 'nacEn92x8-1'
console.keyPassword = 'nWVxpMg6Tkv'

console.secretkeystorePath="${System.properties['user.dir']}/conf/console.dev.secretkeystore"

console.truststorePath="${System.properties['user.dir']}/conf/agent.dev.truststore"
console.truststorePassword = 'nacEn92x8-1'

// specify the database connection string
dataSource.dbCreate = "update"
dataSource.url = "jdbc:hsqldb:file:${System.properties['user.dir']}/database/tutorial;shutdown=true"

grails.serverURL = "http://${InetAddress.getLocalHost().canonicalHostName}:8080/console"

log4j = {
    appenders {
    	file name:'file', file:'logs/console.log', layout:pattern(conversionPattern: '%d{yyyy/MM/dd HH:mm:ss.SSS} %p [%c{1}] %m%n')
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
         'com.linkedin'

    //debug 'com.linkedin.glu.agent.tracker', 'com.linkedin.glu.zookeeper.client'

    //trace 'org.hibernate.SQL', 'org.hibernate.type'

    warn   'org.mortbay.log', 'org.restlet.Context'
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
          'metadata.container.name': [checked: true, name: 'container', groupBy: true, linkFilter: true],
          'metadata.version': [checked: true, name: 'version', groupBy: true],
          'metadata.product': [checked: true, name: 'product', groupBy: true, linkFilter: true],
          'metadata.cluster': [checked: true, name: 'cluster', groupBy: true, linkFilter: true],
          'initParameters.skeleton': [checked: false, name: 'skeleton', groupBy: true],
          script: [checked: false, name: 'script', groupBy: true],
          'metadata.modifiedTime': [checked: false, name: 'Last Modified', groupBy: false],
          status: [checked: true, name: 'status', groupBy: true]
      ],

      system:
      [
          agent: [name: 'agent'],
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
