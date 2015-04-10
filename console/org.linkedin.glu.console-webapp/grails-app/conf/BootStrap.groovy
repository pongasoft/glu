/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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


import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.mgt.SecurityManager
import org.apache.shiro.realm.Realm
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.subject.SimplePrincipalCollection
import org.apache.shiro.subject.Subject
import org.linkedin.glu.console.domain.AuditLog
import org.linkedin.glu.console.domain.Fabric
import grails.util.Environment
import org.linkedin.glu.console.domain.User
import org.linkedin.glu.console.domain.RoleName
import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.glu.groovy.utils.jvm.JVMInfo
import org.linkedin.groovy.util.net.SingletonURLStreamHandlerFactory
import org.linkedin.groovy.util.ivy.IvyURLHandler
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.groovy.util.log.JulToSLF4jBridge
import org.linkedin.glu.console.domain.DbUserCredentials
import org.linkedin.glu.orchestration.engine.system.SystemService
import org.linkedin.glu.orchestration.engine.delta.DeltaService
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinition
import org.linkedin.glu.groovy.utils.plugins.PluginServiceImpl

class BootStrap {

  def grailsApplication
  def shiroSecurityManager
  ConsoleConfig consoleConfig
  SystemService systemService
  DeltaService deltaService
  PluginServiceImpl pluginService

  def init = { servletContext ->
    log.info "Starting up... [${Environment.current} mode]"

    JulToSLF4jBridge.installBridge()

    log.info JVMInfo.getJVMInfoAsStringCollection().join(" | ")

    def config = grailsApplication.config

    servletContext.consoleConfig = consoleConfig

    // setup ivy url handler
    def ivySettings = config.console.ivySettingsURL
    if (ivySettings) {
      SingletonURLStreamHandlerFactory.INSTANCE.registerHandler('ivy') {
        return new IvyURLHandler(ivySettings)
      }
    } else {
      log.warn "console.ivySettingsURL config parameter not specified, ivy:/ URLhandler is not enabled."
    }

    // initializing the plugin if one is provided
    if(config.orchestration.engine.plugins)
    {
      pluginService.initializePlugin(config.orchestration.engine.plugins,
                                     [
                                       applicationContext: grailsApplication.mainContext,
                                       config: config
                                     ])
    }

    // setting up data when development...
    if(Environment.current == Environment.DEVELOPMENT)
    {
      // creating users (glu, glur, glua)
      [
          glu:  [RoleName.USER],
          glur: [RoleName.USER, RoleName.RELEASE],
          glua: [RoleName.USER, RoleName.RELEASE, RoleName.ADMIN],
      ].each { username, roles ->
        User.withTransaction { status ->
          User user = new User(username: username)
          user.updateRoles(roles)
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
        userInstance.updateRoles([RoleName.USER, RoleName.RELEASE, RoleName.ADMIN])
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

    executeWithSubject {
      // handle fabrics defined in bootstrap
      Fabric.withTransaction { status ->
        Map<String, Fabric> allFabrics = Fabric.list().collectEntries { Fabric f -> [f.name, f] }
        config.console?.bootstrap?.fabrics?.each { params ->
          if(!allFabrics.containsKey(params.name))
          {
            def fabricInstance = new Fabric(params)
            if(!fabricInstance.hasErrors() && fabricInstance.save())
            {
              AuditLog.audit('fabric.created', fabricInstance.id.toString(), params.toString())
              def emptySystem = new SystemModel(fabric: fabricInstance.name)
              emptySystem.metadata.name = "Empty System Model"
              systemService.saveCurrentSystem(emptySystem)
              log.info "Successfully added fabric ${fabricInstance.name}"
            }
            else
            {
              status.setRollbackOnly() // rollback the transaction
              throw new RuntimeException("Could not create fabric ${params}")
            }
          }
          else
          {
            log.info "Fabric ${params.name} already exists"

            Fabric currentFabric = allFabrics[params.name]
            ['zkConnectString', 'zkSessionTimeout', 'color'].each { String propName ->
              def currentValue = currentFabric."${propName}"
              def configValue = params[propName]
              if(currentValue != configValue)
                log.warn("for fabric ${currentFabric.name} defined in " +
                         "[console.bootstrap.fabrics]: mismatch ${propName}: ${currentValue} != ${configValue}")
            }
          }
        }
      }
    }

    // initializing default custom delta definition
    CustomDeltaDefinition defaultCustomDeltaDefinition =
      CustomDeltaDefinition.fromDashboard(consoleConfig.defaults.dashboard)
    if(defaultCustomDeltaDefinition)
      deltaService.saveDefaultCustomDeltaDefinition(defaultCustomDeltaDefinition)

    def features = consoleConfig.defaults.features

    features?.each { feature, enabled ->
      log.info "Feature [${feature}] => [${enabled ? 'enabled' : 'disabled'}]"
    }

    log.info "Console started."
  }

  /**
   * Make sure the call runs with a subject so that there is no warning..
   * @return whatever the closure returns
   */
  private <T> T executeWithSubject(Closure<T> closure)
  {
    Realm localizedRealm = shiroSecurityManager.realms[0]

    SecurityManager bootstrapSecurityManager = new DefaultSecurityManager(localizedRealm);
    PrincipalCollection principals =
      new SimplePrincipalCollection("<bootstrap>", localizedRealm.getName());
    Subject subject =
      new Subject.Builder(bootstrapSecurityManager).principals(principals).buildSubject();

    T res = null
    subject.execute {
      res = closure()
    }

    return res
  }

  def destroy = {
    log.info "Shutting down..."

    log.info "Shutdown."
  }
} 