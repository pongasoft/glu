/*
 * Copyright (c) 2013-2014 Yan Pujante
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

grails.servlet.version = "2.5" // Change depending on target container compliance (2.5 or 3.0)
if(System.properties['grails.project.work.dir'])
{
  grails.project.work.dir = System.properties['grails.project.work.dir']
  grails.project.test.reports.dir = "${grails.project.work.dir}/test-reports"
}
grails.project.target.level = 1.7
grails.project.source.level = 1.7
//grails.project.war.file = "target/${appName}-${appVersion}.war"

// uncomment (and adjust settings) to fork the JVM to isolate classpaths
//grails.project.fork = [
//   run: [maxMemory:1024, minMemory:64, debug:false, maxPerm:256]
//]

//def externalDomainClassesInPlacePluginPath = new File("../external-domain-classes")
//grails.plugin.location.'external-domain-classes' =
//  externalDomainClassesInPlacePluginPath.canonicalPath

// in place plugin
grails.plugin.location.'decorate-grails-methods-plugin' = 'decorate-grails-methods-plugin'
grails.plugin.location.'external-domain-classes-grails-plugin' = 'external-domain-classes-grails-plugin'

grails.project.dependency.resolution = {
  // inherit Grails' default dependencies
  inherits("global") {
    // specify dependency exclusions here; for example, uncomment this to disable ehcache:
    // excludes 'ehcache'
  }
  log "error" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
  checksums true // Whether to verify checksums on resolve
  legacyResolve false // whether to do a secondary resolve on plugin installation, not advised and here for backwards compatibility

  repositories {
      inherits true // Whether to inherit repository definitions from plugins

      grailsPlugins()
      grailsHome()
      mavenLocal()
      grailsCentral()
      mavenCentral()
      // uncomment these (or add new ones) to enable remote dependency resolution from public Maven repositories
      //mavenRepo "http://repository.codehaus.org"
      //mavenRepo "http://download.java.net/maven/2/"
      //mavenRepo "http://repository.jboss.com/maven2/"
  }

  dependencies {
    // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes e.g.

    ///// HACK HACK HACK somehow with grails 2.5.0 those 2 lines are required :(
    compile 'org.springframework:spring-aop:4.1.5.RELEASE'
    compile 'org.springframework:spring-expression:4.1.5.RELEASE'
    ///// HACK HACK HACK somehow with grails 2.5.0 those 2 lines are required :(
  }

  plugins {

    // plugins for the build system only
    build ":tomcat:7.0.55.2" // or ":tomcat:8.0.20"

    // plugins for the compile step
    compile ":scaffolding:2.1.2"
    compile ':cache:1.1.8'
    compile ":asset-pipeline:2.1.5"

    // plugins needed at runtime but not for compilation
    runtime ":hibernate4:4.3.8.1" // or ":hibernate:3.6.10.18"
    runtime ":database-migration:1.4.0"
    runtime ":jquery:1.8.3"

    compile ":shiro:1.1.4"
  }
}
