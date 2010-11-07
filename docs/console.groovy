/////////////////////////////////////////////////////////////////////////////////////
// WARNING: this file is NOT a properties file
// WARNING: it is actually a real groovy file so it needs to be properly formatted!
/////////////////////////////////////////////////////////////////////////////////////

console.sslEnabled=true

// location of the console keystore + passwords
console.keystorePath='/Users/ypujante/github/org.linkedin/glu/agent/org.linkedin.glu.agent-server/src/zk-config/keys/console.dev.keystore'
console.keystorePassword='nacEn92x8-1'
console.keyPassword='nWVxpMg6Tkv'

// location of agent trustore + passwords
console.truststorePath='/Users/ypujante/github/org.linkedin/glu/agent/org.linkedin.glu.agent-server/src/zk-config//agent.dev.truststore'
console.truststorePassword='nacEn92x8-1'

// how to commnect to the database (by default we use hsqldb)
console.db.url='jdbc:hsqldb:file:/tmp/consoledb/prodDb;shutdown=true'

// The full URL of the console (used when computing absolute links)
grails.serverURL='http://ypujante-md.linkedin.biz:8080/console'

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

// This section is optional if you do not want to use ldap
ldap.server.url="ldaps://ldap01.linkedin.biz:3269"
ldap.search.base="dc=linkedin,dc=biz"
ldap.search.user="cn=glu,ou=glu,dc=linkedin,dc=biz"
ldap.search.pass="helloworld"
ldap.username.attribute="sAMAccountName"

// this section configures how the console looks like
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
