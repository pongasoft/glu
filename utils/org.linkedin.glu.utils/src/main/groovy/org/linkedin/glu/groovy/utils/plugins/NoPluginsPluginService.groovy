/*
 * Copyright (c) 2012 Yan Pujante
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

package org.linkedin.glu.groovy.utils.plugins

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author yan@pongasoft.com */
public class NoPluginsPluginService implements PluginService
{
  public static final String MODULE = NoPluginsPluginService.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static final NoPluginsPluginService INSTANCE = new NoPluginsPluginService();

  @Override
  void initializePlugin(String pluginClassName, Map initParameters)
  {
    log.warn("Ignoring plugin [${pluginClassName}]")
  }

  @Override
  void initializePlugin(Collection<String> pluginClassNames, Map initParameters)
  {
    log.warn("Ignoring plugins [${pluginClassNames?.join(', ')}]")
  }

  @Override
  void initializePlugin(Map plugin, Map initParameters)
  {
    log.warn("Ignoring map plugin")
  }

  @Override
  def executeMethod(Class targetService, String pluginMethod, Map pluginArgs)
  {
    // does nothing
    return null
  }

  @Override
  def executePrePostMethods(Class targetService, String pluginMethod, Map pluginArgs, Closure serviceClosure)
  {
    // simply execute the service closure
    serviceClosure(pluginArgs)
  }
}