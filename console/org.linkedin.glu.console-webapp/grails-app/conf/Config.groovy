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

/////////////////////////////////////////////////////////
// Default values (can be overriden in any of the config file (see below))
/////////////////////////////////////////////////////////
// security (keystore/trustore defining security between console and agent)
console.sslEnabled = false
console.keystorePath = ""
console.keystorePassword = ""
console.keyPassword = ""
console.truststorePath = ""
console.truststorePassword = ""
console.secretkeystorePath = "/dev/null"

// if you want to restrict access to some subdirectory between agent and console
console.authorizationService.unrestrictedLocation = "/"

// set to '0' if you don't want deployments to be automatically archived
console.deploymentService.autoArchiveTimeout = "30m"

// connection timeout when the console tries to talk to the agent (rest)
console.to.agent.connectionTimeout = "30s"

/////////////////////////////////////////////////////////
// End Default values
/////////////////////////////////////////////////////////

// locations to search for config files that get merged into the main config
// config files can either be Java properties files or ConfigSlurper scripts

// grails.config.locations = [ "classpath:${appName}-config.properties",
//                             "classpath:${appName}-config.groovy",
//                             "file:${userHome}/.grails/${appName}-config.properties",
//                             "file:${userHome}/.grails/${appName}-config.groovy"]

grails.config.locations = []

[ "${System.properties['user.dir']}/conf/glu-console-webapp.groovy",
  "${userHome}/.org.linkedin.glu/glu-console-webapp.groovy"].each { String filename ->
  def file = new File(filename)
  if(file.exists())
  {
    grails.config.locations << file.toURI().toURL().toString()
    println "Detected config file ${file}."
  }
}

if(System.properties["org.linkedin.glu.${appName}.config.location"]) {
  grails.config.locations << "file:" + System.properties["org.linkedin.glu.${appName}.config.location"]
}

grails.mime.file.extensions = true // enables the parsing of file extensions from URLs into the request format
grails.mime.use.accept.header = false
grails.mime.types = [ html: ['text/html','application/xhtml+xml'],
                      xml: ['text/xml', 'application/xml'],
                      text: 'text/plain',
                      js: 'text/javascript',
                      rss: 'application/rss+xml',
                      atom: 'application/atom+xml',
                      css: 'text/css',
                      csv: 'text/csv',
                      all: '*/*',
                      json: ['application/json','text/json'],
                      form: 'application/x-www-form-urlencoded',
                      multipartForm: 'multipart/form-data'
                    ]
// The default codec used to encode data with ${}
grails.views.default.codec="none" // none, html, base64
grails.views.gsp.encoding="UTF-8"
grails.converters.encoding="UTF-8"

grails.views.javascript.library="jquery"

// enabled native2ascii conversion of i18n properties files
grails.enable.native2ascii = true

// extra packages with domain classes
grails.external.domain.packages = ['org.linkedin.glu.orchestration.engine.delta']

// this parameter disables auto flushing in grails which happens when an object is dirty
// and not saved... when setting this to manual you need to explicitely save the object
hibernate.flush.mode="manual"

// see ConsoleConfig for explanation
console.dev.defaults =
  [
    customCss: null,

//    dashboardAgentLinksToAgent: false,
    
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
      'a:tag1': [background: '#5a0087', color: '#ffffff'],
      'e:tag1': [background: '#00875a', color: '#ffffff'],
    ],

      system:
      [
        agent: [name: 'agent'],
        'tags.a:tag1': [name: 'a:tag1'],
        'tags.e:tag1': [name: 'e:tag1'],
        'metadata.container.name': [name: 'container'],
        'metadata.product': [name: 'product'],
        'metadata.version': [name: 'version'],
        'metadata.cluster': [name: 'cluster']
      ],

    disableModelUpdate: false,

      shortcutFilters:
      [
          [
              name: 'product',
              source: 'metadata.product',
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

// set per-environment serverURL stem for creating absolute links
environments {
  production {
    // configuration parameters should be read from the console.config.location file
  }
  development {

    // read config properties coming from the build
    grails.config.locations << "file:./lib/build.properties.groovy"

    // configuration parameters
    console.sslEnabled = true

    console.keystorePassword = 'nacEn92x8-1'
    console.keyPassword = 'nWVxpMg6Tkv'

    console.truststorePassword = 'nacEn92x8-1'

    console.defaults = console.dev.defaults

    console.authorizationService.unrestrictedLocation = '/export/content/glu'

    grails.serverURL = "http://${InetAddress.getLocalHost().canonicalHostName}:8080/${appName}"

    // log4j configuration
    log4j = {

        appenders {
            console name:'stdout', layout:pattern(conversionPattern: '%d{yyyy/MM/dd HH:mm:ss.SSS} %p [%c{1}] %m%n')
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

        //debug 'org.linkedin.zookeeper.tracker', 'org.linkedin.glu.agent.tracker'

//        trace 'org.hibernate.SQL', 'org.hibernate.type'
//        trace 'org.codehaus.groovy.grails.orm'
//        trace 'org.codehaus.groovy.grails.orm.hibernate'


        warn   'org.mortbay.log'
    }
  }
  test {

    // read config properties coming from the build
    grails.config.locations << "file:./lib/build.properties.groovy"

    // configuration parameters
    console.sslEnabled = true

    console.keystorePassword = 'nacEn92x8-1'
    console.keyPassword = 'nWVxpMg6Tkv'

    console.truststorePassword = 'nacEn92x8-1'

    console.defaults = console.dev.defaults

    console.authorizationService.unrestrictedLocation = '/export/content/glu'

    grails.serverURL = "http://localhost:8080/${appName}"

    // log4j configuration
    log4j = {

        appenders {
            console name:'stdout', layout:pattern(conversionPattern: '%d{yyyy/MM/dd HH:mm:ss.SSS} %p [%c{1}] %m%n')
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

        //debug 'org.linkedin.glu.agent.tracker', 'org.linkedin.glu.zookeeper.client'

        //trace 'org.hibernate.SQL', 'org.hibernate.type'


        warn   'org.mortbay.log'
    }
  }
}



     