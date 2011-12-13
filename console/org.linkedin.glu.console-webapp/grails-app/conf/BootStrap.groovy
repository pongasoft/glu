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

import org.linkedin.glu.console.domain.Fabric
import grails.util.Environment
import org.linkedin.glu.console.domain.User
import org.linkedin.glu.console.domain.RoleName
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.groovy.util.net.SingletonURLStreamHandlerFactory
import org.linkedin.groovy.util.ivy.IvyURLHandler
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.groovy.util.log.JulToSLF4jBridge
import org.linkedin.glu.console.domain.DbUserCredentials
import org.linkedin.glu.orchestration.engine.system.SystemService
import org.linkedin.glu.orchestration.engine.delta.DeltaService
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinition
import org.linkedin.glu.orchestration.engine.plugins.PluginServiceImpl
import org.codehaus.groovy.grails.commons.ApplicationHolder

class BootStrap {

  ConsoleConfig consoleConfig
  SystemService systemService
  DeltaService deltaService
  PluginServiceImpl pluginService

  def init = { servletContext ->
    log.info "Starting up... [${Environment.current} mode]"

    JulToSLF4jBridge.installBridge()
    
    if(!consoleConfig.defaults)
      throw new IllegalStateException("could not find console config defaults. Did you properly set console.defaults property ?")

    servletContext.consoleConfig = consoleConfig

    // setup ivy url handler
    def ivySettings = ConfigurationHolder.config.console.ivySettingsURL
    if (ivySettings) {
      SingletonURLStreamHandlerFactory.INSTANCE.registerHandler('ivy') {
        return new IvyURLHandler(ivySettings)
      }
    } else {
      log.warn "console.ivySettingsURL config parameter not specified, ivy:/ URLhandler is not enabled."
    }

    // initializing the plugin if one is provided
    if(consoleConfig.defaults.plugins?.engine)
    {
      pluginService.initializePlugin(consoleConfig.defaults.plugins.engine,
                                     [
                                       applicationContext: ApplicationHolder.application.mainContext,
                                       defaults: consoleConfig.defaults
                                     ])
    }

    // setting up data when development...
    if(Environment.current == Environment.DEVELOPMENT)
    {
      def hostname = InetAddress.getLocalHost().canonicalHostName

      // dev fabrics
      [
          'glu-dev-1': '#005a87',
          'glu-dev-2': '#5a0087',
      ].each { fabric, color ->
        new Fabric(name: fabric,
                   zkConnectString: "${hostname}:2181",
                   zkSessionTimeout: '5s',
                   color: color).save()

        // create an empty system
        def emptySystem = new SystemModel(fabric: fabric)
        emptySystem.metadata.name = "Empty System Model"
        systemService.saveCurrentSystem(emptySystem)
      }

      // creating users (glu, glur, glua)
      [
          glu:  [RoleName.USER],
          glur: [RoleName.USER, RoleName.RELEASE],
          glua: [RoleName.USER, RoleName.RELEASE, RoleName.ADMIN],
      ].each { username, roles ->
        User.withTransaction { status ->
          User user = new User(username: username)
          user.setRoles(roles)
          if(!user.save())
            throw new RuntimeException("could not create ${username} user")

          DbUserCredentials credentials = new DbUserCredentials(username: username,
                                                                password: "password")
          if(!credentials.save())
          {
            status.setRollbackOnly() // rollback the transaction
            throw new RuntimeException("could not create ${username} user")
          }
        }
      }
    }

    // if no user in the database then simply create an admin user
    if(!User.count())
    {
      log.info "No user detected. Creating admin account..."

      User.withTransaction { status ->
        def userInstance = new User(username: "admin")
        userInstance.setRoles([RoleName.USER, RoleName.RELEASE, RoleName.ADMIN])
        if(!userInstance.save())
          throw new RuntimeException("could not create admin user")

        DbUserCredentials credentials = new DbUserCredentials(username: "admin",
                                                              password: "admin")
        if(!credentials.save())
        {
          status.setRollbackOnly() // rollback the transaction
          throw new RuntimeException("could not create admin user")
        }
      }

      log.info "Successfully created (original) admin user. MAKE SURE YOU LOG IN AND CHANGE THE PASSWORD!"
    }

    // initialzing default custom delta definition
    CustomDeltaDefinition defaultCustomDeltaDefinition =
      CustomDeltaDefinition.fromDashboard(consoleConfig.defaults.dashboard)
    if(defaultCustomDeltaDefinition)
      deltaService.saveDefaultCustomDeltaDefinition(defaultCustomDeltaDefinition)

    log.info "Console started."
  }

  def destroy = {
    log.info "Shutting down..."

    log.info "Shutdown."
  }
} 