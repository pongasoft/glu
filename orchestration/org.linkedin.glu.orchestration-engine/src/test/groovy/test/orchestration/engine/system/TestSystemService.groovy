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

package test.orchestration.engine.system

import org.linkedin.glu.groovy.utils.plugins.PluginServiceImpl
import org.linkedin.glu.orchestration.engine.system.SystemServiceImpl
import org.linkedin.glu.provisioner.core.model.SystemModel

/**
 * @author yan@pongasoft.com */
public class TestSystemService extends GroovyTestCase
{
  PluginServiceImpl pluginService = new PluginServiceImpl()
  SystemServiceImpl systemService = new SystemServiceImpl(pluginService: pluginService)

  public void testParseSystemModel()
  {
    // no plugin
    SystemModel model = systemService.parseSystemModel("""{"fabric": "f1"}""")
    assertNull(model.metadata.plugin)

    // post parse plugin
    pluginService.initializePlugin([SystemService_post_parseSystemModel: { args ->
      // modifying the model directly (args.serviceResult is a SystemModel)
      args.serviceResult.metadata.plugin = "post"
      return null
    }], [:])

    model = systemService.parseSystemModel("""{"fabric": "f1"}""")
    assertEquals("post", model.metadata.plugin)

    // pre parse plugin
    pluginService.initializePlugin([SystemService_pre_parseSystemModel: { args ->
      // modifying the source directly (in this case completely changing it!)
      args.source = """{"fabric": "f1", "metadata": {"plugin": "pre"}}"""
      return null
    }], [:])

    model = systemService.parseSystemModel("""{"fabric": "f1"}""")
    assertEquals("pre", model.metadata.plugin)
  }
}