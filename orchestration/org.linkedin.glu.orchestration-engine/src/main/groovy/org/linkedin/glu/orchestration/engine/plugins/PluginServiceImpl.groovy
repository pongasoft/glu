/*
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

package org.linkedin.glu.orchestration.engine.plugins

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.util.reflect.ReflectUtils

/**
 * @author yan@pongasoft.com */
public class PluginServiceImpl implements PluginService
{
  public static final String MODULE =  PluginService.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  def plugin

  @Override
  void initializePlugin(String pluginClassname, Map initParameters)
  {
    plugin = createPluginFromClassname(pluginClassname, initParameters)
    executeMethod(PluginService, 'initialize', initParameters)
  }

  private static def createPluginFromClassname(String pluginClassname, Map initParameters)
  {
    if(pluginClassname)
    {
      log.info("Initializing plugin [${pluginClassname}]")
      return ReflectUtils.forName(pluginClassname).newInstance()
    }
    else
      return null
  }

  @Override
  void initializePlugin(Collection<String> pluginClassnames, Map initParameters)
  {
    plugin = pluginClassnames?.collect { createPluginFromClassname(it, initParameters) }
    executeMethod(PluginService, 'initialize', initParameters)
  }

  @Override
  void initializePlugin(Map plugin, Map initParameters)
  {
    log.info("Initializing plugin from map")
    this.plugin = plugin
    executeMethod(PluginService, 'initialize', initParameters)
  }

  @Override
  def executeMethod(Class targetService, String pluginMethod, Map pluginArgs)
  {
    executePluginClosure("${targetService.simpleName}_${pluginMethod}", pluginArgs)
  }

  @Override
  def executePrePostMethods(Class targetService,
                            String pluginMethod,
                            Map pluginArgs,
                            Closure serviceClosure)
  {
    executePluginClosure("${targetService.simpleName}_pre_${pluginMethod}", pluginArgs)

    try
    {
      def res = serviceClosure()

      def args = [:]
      if(pluginArgs)
        args.putAll(pluginArgs)
      args.serviceResult = res
      executePluginClosure("${targetService.simpleName}_post_${pluginMethod}",
                           args)
      return res
    }
    catch(Throwable th)
    {
      def args = [:]
      if(pluginArgs)
        args.putAll(pluginArgs)
      args.serviceException = th
      executePluginClosure("${targetService.simpleName}_post_${pluginMethod}",
                           args)
      throw th
    }
  }

  private def executePluginClosure(def plugin, String closureName, Map pluginArgs)
  {
    def res = null
    def closure = null

    if(plugin instanceof Collection)
    {
      plugin.each { res = executePluginClosure(it, closureName, pluginArgs) }
      return res
    }

    if(plugin instanceof Map)
    {
      closure = plugin[closureName]
    }
    else
    {
      if(plugin?.hasProperty(closureName))
        closure = plugin."${closureName}"
    }

    if(closure)
      res = closure(pluginArgs)

    return res
  }

  private def executePluginClosure(String closureName, Map pluginArgs)
  {
    executePluginClosure(plugin, closureName, pluginArgs)
  }
}