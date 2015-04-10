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
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DecorateGrailsMethodsPluginGrailsPlugin
{
  public static final String MODULE = "org.pongasoft.grailsPlugins.DecorateGrailsMethodsPluginGrailsPlugin"
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  // the plugin version
  def version = "1.0.0"
  // the version or versions of Grails the plugin is designed for
  def grailsVersion = "2.2 > *"
  // resources that are excluded from plugin packaging
  def pluginExcludes = [
    "grails-app/views/error.gsp"
  ]

  /**
   * Required for {@link #onChange} to be called */
  def watchedResources = [
    "file:./grails-app/controllers/**/*Controller.groovy"]

  def title = "Decorate Grails Methods Plugin" // Headline display name of the plugin
  def author = "Yan Pujante"
  def authorEmail = "yan@pongasoft.com"
  def description = '''
The purpose of this plugin is to decorate various grails methods.
'''

  def documentation = null

  def doWithDynamicMethods = { ctx ->
    log.info("Decorating grails methods")
    application.controllerClasses.each() { controllerClass ->
      decorateRedirectMethod(controllerClass)
    }
  }

  private void decorateRedirectMethod(def controllerClass)
  {
    def oldRedirect = controllerClass.metaClass.pickMethod("redirect", [Map] as Class[])

    controllerClass.metaClass.redirect = { Map args ->

      def fabricName = delegate.request.fabric?.name

      if(fabricName && !args.params?.fabric)
      {
        def params = args.params ?: [:]
        params.fabric = fabricName
        args.params = params
      }

      oldRedirect.invoke(delegate, args)
    }
  }

  def onChange = { event ->
    if(!(event.source instanceof Class)) return

    if(application.isArtefactOfType(ControllerArtefactHandler.TYPE, event.source))
    {
      decorateRedirectMethod(application.getArtefact(ControllerArtefactHandler.TYPE,
                                                     event.source.name))
    }
  }
}
