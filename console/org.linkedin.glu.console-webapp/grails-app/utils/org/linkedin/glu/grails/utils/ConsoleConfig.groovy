/*
 * Copyright 2010-2010 LinkedIn, Inc
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
//      dashboard: [:],
//          'metadata.container.name': [checked: true, name: 'container', groupBy: true, linkFilter: true],
//          agent: [checked: true, name: 'agent', groupBy: true],
//          'metadata.product': [checked: true, name: 'product', groupBy: true, linkFilter: true],
//          'metadata.version': [checked: true, name: 'version', groupBy: true, linkFilter: true],
//          'initParameters.wars': [checked: false, name: 'wars', groupBy: true],
//          mountPoint: [checked: false, name: 'mountPoint', groupBy: true, linkFilter: true],
//          'metadata.cluster': [checked: true, name: 'cluster', groupBy: true, linkFilter: true],
//          'initParameters.skeleton': [checked: false, name: 'skeleton', groupBy: true],
//          script: [checked: false, name: 'script', groupBy: true],
//          'metadata.modifiedTime': [checked: false, name: 'Last Modified', groupBy: false],
//          status: [checked: true, name: 'status', groupBy: true]

//      system: [:],
//          'metadata.container.name': [name: 'container'],
//          agent: [name: 'agent'],
//          'metadata.product': [name: 'product'],
//          'metadata.version': [name: 'version'],
//          'metadata.cluster': [name: 'cluster']

//      model:
//      [
//          [
//              name: 'product',
//              header: ['version']
//          ]
//      ]

  ConsoleConfig()
  {
    defaults = ConfigurationHolder.config.console.defaults
  }

}
