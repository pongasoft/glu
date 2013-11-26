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

import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerAware
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.cache.ehcache.EhCacheFactoryBean
import org.linkedin.util.clock.Timespan
import java.util.concurrent.Executors

import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.glu.grails.utils.ConsoleConfig
import org.linkedin.glu.orchestration.engine.commands.CommandsServiceImpl
import org.linkedin.util.reflect.ObjectProxyBuilder
import org.linkedin.glu.utils.core.DisabledFeatureProxy
import org.linkedin.glu.orchestration.engine.commands.CommandsService
import org.linkedin.glu.orchestration.engine.commands.CommandExecutionStorageImpl
import org.linkedin.glu.commands.impl.FileSystemCommandExecutionIOStorage
import org.linkedin.glu.commands.impl.MemoryCommandExecutionIOStorage

class GrailsPluginLoadOrderDebugger implements PluginManagerAware
{
  public static final String MODULE = "org.linkedin.glu.spring.resources.GrailsPluginLoadOrderDebugger";
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  @Override
  void setPluginManager(GrailsPluginManager pluginManager)
  {
    log.debug pluginManager.allPlugins.collect { "$it" }.join(',')
  }
}

// Place your Spring DSL code here
beans = {
  userSessionCache(EhCacheFactoryBean) {
    timeToIdle = Timespan.parse('30m').durationInSeconds
    timeToLive = 0 // use only the timeToIdle parameter...
  }

  grailsPluginLoadOrderDebugger(GrailsPluginLoadOrderDebugger)

  def consoleConfig = new ConsoleConfig()
  consoleConfig.setGrailsApplication(grailsApplication)

  def fixedThreadPoolSize =
    consoleConfig.console.deploymentService.deployer.planExecutor.leafExecutorService.fixedThreadPoolSize ?: 0

  if(fixedThreadPoolSize ?: 0 > 0)
  {
    log.info "Setting leafExecutorService thread pool size to [${fixedThreadPoolSize}]"
    leafExecutorService(Executors, fixedThreadPoolSize) { bean ->
      bean.factoryMethod = "newFixedThreadPool"
      bean.destroyMethod = "shutdown"
    }
  }
  else
  {
    leafExecutorService(Executors) { bean ->
      bean.factoryMethod = "newCachedThreadPool"
      bean.destroyMethod = "shutdown"
    }
  }

  /**
   * CommandsService
   */
  if(consoleConfig.isFeatureEnabled("commands"))
  {
    // storage (DB)
    commandExecutionStorage(CommandExecutionStorageImpl)

    // IO Storage (command)
    switch(consoleConfig.console.commandsService.storageType)
    {
      case 'filesystem':
        def rootDir = new File(consoleConfig.console.commandsService.commandExecutionIOStorage.filesystem.rootDir)
        commandExecutionFileSystem(FileSystemImpl, rootDir)

        commandExecutionIOStorage(FileSystemCommandExecutionIOStorage) {
          commandExecutionFileSystem = ref("commandExecutionFileSystem")
          pluginService = ref('pluginService')
        }
        break

      case 'memory':
        commandExecutionIOStorage(MemoryCommandExecutionIOStorage) {
          pluginService = ref('pluginService')
          maxNumberOfElements = consoleConfig.console.commandsService.commandExecutionIOStorage.memory.maxNumberOfElements ?: 25
        }
        break

      default:
        throw new IllegalArgumentException("unsupported storageType [${consoleConfig.console.commandsService.storageType}]")
    }

    commandsService(CommandsServiceImpl) {
      agentsService = ref("agentsService")
      authorizationService = ref("authorizationService")
      submitter = ref("submitter")
      commandExecutionStorage = ref("commandExecutionStorage")
      commandExecutionIOStorage = ref("commandExecutionIOStorage")
    }
  }
  else
  {
    // in the disabled case, return a proxy that will throw exceptions
    commandsService(ObjectProxyBuilder, new DisabledFeatureProxy("commands"), CommandsService) { bean ->
      bean.factoryMethod = "createProxy"
    }
  }
}