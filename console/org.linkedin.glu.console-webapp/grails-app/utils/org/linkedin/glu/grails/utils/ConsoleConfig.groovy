/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.grails.utils

import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.commons.ApplicationHolder

/**
 * @author ypujante@linkedin.com */
class ConsoleConfig
{
  static ConsoleConfig getInstance()
  {
    ApplicationHolder.application.mainContext.servletContext.consoleConfig
  }

  /**
   * Map containing defaults for the console 
   */
  def defaults

  /**
   * Map (<code>ConfigObject</code>) configuration parameters
   */
  def config

  ConsoleConfig()
  {
    config = ConfigurationHolder.config
    defaults = config.console.defaults
  }

  def getDefault(String name)
  {
    return defaults[name]
  }
}
