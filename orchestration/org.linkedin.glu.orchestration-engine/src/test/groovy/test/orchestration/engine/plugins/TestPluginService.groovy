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

package test.orchestration.engine.plugins

import org.linkedin.glu.orchestration.engine.plugins.PluginServiceImpl

/**
 * @author yan@pongasoft.com */
public class TestPluginService extends GroovyTestCase
{
  PluginServiceImpl pluginService = new PluginServiceImpl()

  public void testPluginMap()
  {
    def executions = []

    def pluginMap =
    [
      PluginService_initialize: { args ->
        executions << [PluginService_initialize: args]
      },

      S1_m1: { args ->
        executions << [S1_m1: args]
      },

      S1_pre_m2: { args ->
        executions << [S1_pre_m2: args]
      },

      S1_post_m2: { args ->
        executions << [S1_post_m2: args]
      },

      S1_pre_m3: { args ->
        executions << [S1_pre_m3: args]
      },

      S1_post_m4: { args ->
        executions << [S1_post_m4: args]
      }
    ]

    executions.clear()
    pluginService.initializePlugin(pluginMap, [i1: 'vi1'])
    assertEquals(1, executions.size())
    assertEquals([i1: 'vi1'], executions[0]['PluginService_initialize'])

    executions.clear()
    pluginService.executeMethod(S1, "m1", [p1: 'pv1'])
    assertEquals(1, executions.size())
    assertEquals([p1: 'pv1'], executions[0]['S1_m1'])

    executions.clear()
    pluginService.executePrePostMethods(S1, "m2", [p2: 'pv2']) {
      return 2
    }
    assertEquals(2, executions.size())
    assertEquals([p2: 'pv2'], executions[0]['S1_pre_m2'])
    assertEquals([p2: 'pv2', serviceResult: 2], executions[1]['S1_post_m2'])

    executions.clear()
    pluginService.executePrePostMethods(S1, "m3", [p3: 'pv3']) {
      return 3
    }
    assertEquals(1, executions.size())
    assertEquals([p3: 'pv3'], executions[0]['S1_pre_m3'])

    executions.clear()
    pluginService.executePrePostMethods(S1, "m4", [p4: 'pv4']) {
      return 4
    }
    assertEquals(1, executions.size())
    assertEquals([p4: 'pv4', serviceResult: 4], executions[0]['S1_post_m4'])

    executions.clear()
    pluginService.executeMethod(S1, "m100", [p1: 'pv1'])
    assertEquals(0, executions.size())

    executions.clear()
    pluginService.executePrePostMethods(S1, "m100", [p1: 'pv1']) {
      return 100
    }
    assertEquals(0, executions.size())
  }

  public void testPluginClass()
  {
    def executions = []

    executions.clear()
    pluginService.initializePlugin(PluginClass.name, [executions: executions, i1: 'vi1'])
    assertEquals(1, executions.size())
    assertEquals([i1: 'vi1'], executions[0]['PluginService_initialize'])

    executions.clear()
    pluginService.executeMethod(S1, "m1", [p1: 'pv1'])
    assertEquals(1, executions.size())
    assertEquals([p1: 'pv1'], executions[0]['S1_m1'])

    executions.clear()
    pluginService.executePrePostMethods(S1, "m2", [p2: 'pv2']) {
      return 2
    }
    assertEquals(2, executions.size())
    assertEquals([p2: 'pv2'], executions[0]['S1_pre_m2'])
    assertEquals([p2: 'pv2', serviceResult: 2], executions[1]['S1_post_m2'])

    executions.clear()
    pluginService.executePrePostMethods(S1, "m3", [p3: 'pv3']) {
      return 3
    }
    assertEquals(1, executions.size())
    assertEquals([p3: 'pv3'], executions[0]['S1_pre_m3'])

    executions.clear()
    pluginService.executePrePostMethods(S1, "m4", [p4: 'pv4']) {
      return 4
    }
    assertEquals(1, executions.size())
    assertEquals([p4: 'pv4', serviceResult: 4], executions[0]['S1_post_m4'])

    executions.clear()
    pluginService.executeMethod(S1, "m100", [p1: 'pv1'])
    assertEquals(0, executions.size())

    executions.clear()
    pluginService.executePrePostMethods(S1, "m100", [p1: 'pv1']) {
      return 100
    }
    assertEquals(0, executions.size())
  }
}

// fake service class for plugin api
class S1 {}

class PluginClass
{
  def executions

  def PluginService_initialize = { args ->
    executions = args.remove('executions')
    executions << [PluginService_initialize: args]
  }

  def S1_m1 = { args ->
    executions << [S1_m1: args]
  }

  def S1_pre_m2 = { args ->
    executions << [S1_pre_m2: args]
  }

  def S1_post_m2 = { args ->
    executions << [S1_post_m2: args]
  }

  def S1_pre_m3 = { args ->
    executions << [S1_pre_m3: args]
  }

  def S1_post_m4 = { args ->
    executions << [S1_post_m4: args]
  }
}

