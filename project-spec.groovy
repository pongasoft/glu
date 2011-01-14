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

spec = [
    name: 'glu',
    group: 'org.linkedin',
    version: '1.6.0',

    versions: [
      grails: '1.3.5',
      groovy: '1.7.5',
      jetty: '7.2.2.v20101205',
      linkedinUtils: '1.2.1',
      linkedinZookeeper: '1.2.1',
      restlet: '2.0.1',
      sigar: '1.6.4',
      slf4j: '1.5.8' // to be compatible with grails 1.3.5
    ],

    // information about the build framework itself
    build: [
        type: "gradle",
        version: "0.9",
        uri: "http://gradle.artifactoryonline.com/gradle/distributions/gradle-0.9-all.zip",
        commands: [
            "snapshot": "gradle release",
            "release": "gradle -Prelease=true release"
        ]
    ]
]

spec.scmUrl = "git@github.com:linkedin/${spec.name}.git"

/**
 * External dependencies
 */
spec.external = [
  commonsCli: 'commons-cli:commons-cli:1.2',
  groovy: "org.codehaus.groovy:groovy:${spec.versions.groovy}",
  httpClient: "org.apache.httpcomponents:httpclient:4.0",
  ivy: 'org.apache.ivy:ivy:2.2.0',
  jettyPackage: [
    group: "org.eclipse.jetty",
    name: "jetty-distribution",
    version: spec.versions.jetty,
    ext: "tar.gz"
  ],
  junit: 'junit:junit:4.4',
  linkedinUtilsCore: "org.linkedin:org.linkedin.util-core:${spec.versions.linkedinUtils}",
  linkedinUtilsGroovy: "org.linkedin:org.linkedin.util-groovy:${spec.versions.linkedinUtils}",
  linkedinZookeeperCliImpl: "org.linkedin:org.linkedin.zookeeper-cli-impl:${spec.versions.linkedinZookeeper}",
  linkedinZookeeperCliPackage: [
    group: "org.linkedin",
    name: "org.linkedin.zookeeper-cli",
    version: spec.versions.linkedinZookeeper,
    configuration: "package"
  ],
  linkedinZookeeperImpl: "org.linkedin:org.linkedin.zookeeper-impl:${spec.versions.linkedinZookeeper}",
  linkedinZookeeperServerPackage: [
    group: "org.linkedin",
    name: "org.linkedin.zookeeper-server",
    version: spec.versions.linkedinZookeeper,
    configuration: "package"
  ],
  log4j: 'log4j:log4j:1.2.16',
  mimeUtil: 'eu.medsea.mimeutil:mime-util:2.1.3',
  restlet: "org.restlet.jse:org.restlet:${spec.versions.restlet}",
  restletExtHttpClient: "org.restlet.jse:org.restlet.ext.httpclient:${spec.versions.restlet}",
  restletExtJson: "org.restlet.jse:org.restlet.ext.json:${spec.versions.restlet}",
  restletExtSimple: "org.restlet.jse:org.restlet.ext.simple:${spec.versions.restlet}",
  servletApi: 'javax.servlet:servlet-api:2.5',
  shiro: "org.apache.shiro:shiro-all:1.0.0-incubating",
  sigar: "com.hyperic:sigar:${spec.versions.sigar}",
  simpleFramework: 'org.simpleframework:simple:4.1.21',
  slf4j: "org.slf4j:slf4j-api:${spec.versions.slf4j}",
  slf4jLog4j: "org.slf4j:slf4j-log4j12:${spec.versions.slf4j}",
  slf4jJul: "org.slf4j:jul-to-slf4j:${spec.versions.slf4j}",
  zookeeper: 'org.apache.zookeeper:zookeeper:3.3.1'
]
