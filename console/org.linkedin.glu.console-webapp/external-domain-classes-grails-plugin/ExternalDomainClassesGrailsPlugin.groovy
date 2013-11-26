/*
 * Copyright (c) 2011-2013 Yan Pujante
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

import grails.persistence.Entity
import org.codehaus.groovy.grails.commons.DomainClassArtefactHandler
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.type.filter.AnnotationTypeFilter

/**
 * This plugin was inspired by the code posted on the grails-dev support mailing list @
 * http://markmail.org/message/ct2kwmuzqow3ym2m by Juraj Misur. This solution does
 * not work properly as it happens too late and the hibernate plugin has already kicked in (the net
 * effect being that constraint validators are ignored!). By making it a plugin, it is possible
 * to happen early enough in the bootstrap process. See <code>definition</code> field for details.
 *
 * You use this plugin by doing 2 things:
 *
 * 1) annotate any class you wish to be a domain class with <code>grails.persistence.Entity</code>
 *    annotation
 * 2) define a <code>grails.external.domain.packages</code> property in your config file
 *    (<code>Config.groovy</code>). Example:
 *    <code>grails.external.domain.packages = ['com.acme.domain1', 'com.acme.domain2']</code>
 *
 * @author yan@pongasoft.com */
class ExternalDomainClassesGrailsPlugin
{
  public static final String MODULE = "org.pongasoft.grailsPlugins.ExternalDomainClassesGrailsPlugin"
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  // the plugin version
  def version = "1.1.0"
  // the version or versions of Grails the plugin is designed for
  def grailsVersion = "2.0.3 > *"
  // the other plugins this plugin depends on
  def dependsOn = [:]
  // resources that are excluded from plugin packaging
  def pluginExcludes = [
    "grails-app/views/error.gsp"
  ]

  def author = "Yan Pujante"
  def authorEmail = "yan@pongasoft.com"
  def title = "Add external domain classes to the application"
  def description = '''
By default only classes under the grails-app/domain directory are included as domain classes.
The purpose of this plugin is to add classes that are annotated with grails.persistence.Entity in the list
of packages defined in the property: grails.external.domain.packages to be automatically treated
as domain classes the same way they would if they were under grails-app/domain.
'''

  // URL to the plugin's documentation
  def documentation = "http://www.github.com/pongasoft/external-domain-classes-grails-plugin/README.md"

  def loadAfter = ['core']

  def loadBefore=  ['domainClass', 'hibernate', 'dataSource']

  def doWithWebDescriptor = { xml ->
    // nothing to do
  }

  def doWithSpring = {
    log.info("Processing external domain classes")
    BeanDefinitionRegistry simpleRegistry = scanPackages(application)
    addDomainClasses(application, simpleRegistry)
  }

  def doWithDynamicMethods = { ctx ->
    // nothing to do
  }

  def doWithApplicationContext = { applicationContext ->
    // nothing to do
  }

  def onChange = { event ->
    // nothing to do
  }

  def onConfigChange = { event ->
    // nothing to do
  }

  protected addDomainClasses(GrailsApplication application, BeanDefinitionRegistry simpleRegistry)
  {
    simpleRegistry?.beanDefinitionNames?.each { String beanName ->
      BeanDefinition bean = simpleRegistry.getBeanDefinition(beanName)
      String beanClassName = bean.beanClassName
      log.debug("Adding domain artefact ${beanClassName}")
      application.addArtefact(DomainClassArtefactHandler.TYPE,
                              Class.forName(beanClassName,
                                            true,
                                            Thread.currentThread().contextClassLoader))
    }
  }

  protected BeanDefinitionRegistry scanPackages(GrailsApplication application)
  {
    def packages = application.config.grails.external.domain.packages

    if(packages)
    {
      log.debug("Detected external packages: ${packages}")

      BeanDefinitionRegistry simpleRegistry = new SimpleBeanDefinitionRegistry()
      ClassPathBeanDefinitionScanner scanner = configureScanner(simpleRegistry)
      scanner.scan(packages as String[])
      return simpleRegistry
    }
    else
    {
      return null
    }
  }

  protected ClassPathBeanDefinitionScanner configureScanner(BeanDefinitionRegistry registry)
  {
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry, false)
    scanner.includeAnnotationConfig = false
    scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class))
    return scanner
  }

}
