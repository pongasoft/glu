/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2013 Yan Pujante
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

import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.plugins.support.aware.GrailsApplicationAware

/**
 * @author ypujante@linkedin.com */
class ConsoleConfig implements GrailsApplicationAware
{
  static ConsoleConfig INSTANCE = null

  static ConsoleConfig getInstance()
  {
    if(INSTANCE == null)
      throw new IllegalStateException("ConsoleConfig not initialized")

    return INSTANCE
  }

  static void setInstance(ConsoleConfig config)
  {
    INSTANCE = config
  }

  /**
   * Map containing defaults for the console 
   */
  def defaults

  /**
   * Map (<code>ConfigObject</code>) configuration parameters
   */
  def config

  @Override
  void setGrailsApplication(GrailsApplication grailsApplication)
  {
    config = grailsApplication.config
    defaults = config.console.defaults ?: [:]
  }

  def getDefault(String name)
  {
    return defaults[name]
  }

  def propertyMissing(String name)
  {
    config."${name}"
  }

  boolean isFeatureEnabled(feature)
  {
    return org.linkedin.groovy.util.config.Config.getOptionalBoolean(getDefault('features'),
                                                                     feature,
                                                                     false)
  }

  void disableFeature(feature)
  {
    def features = defaults.features
    if(!features)
    {
      features = [:]
      defaults.features = features
    }
    features[feature] = false
  }
}
