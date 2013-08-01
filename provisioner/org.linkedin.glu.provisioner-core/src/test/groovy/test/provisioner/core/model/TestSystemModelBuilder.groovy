/*
 * Copyright (c) 2013 Yan Pujante
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

package test.provisioner.core.model

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.glu.groovy.utils.shell.ShellImpl
import org.linkedin.glu.provisioner.core.model.JsonSystemModelRenderer
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.builder.ModelBuilderParseException
import org.linkedin.glu.provisioner.core.model.builder.SystemModelBuilder
import org.linkedin.groovy.util.config.Config
import org.linkedin.util.io.resource.Resource

/**
 * @author yan@pongasoft.com  */
public class TestSystemModelBuilder extends GroovyTestCase
{
  Shell rootShell = ShellImpl.createRootShell()

  protected File getExampleSystemsRoot()
  {
    def r = Config.getOptionalString(System.getProperties(),
                                     'test.provisioner.core.model.TestSystemModelBuilder.exampleSystemsRoot',
                                     '../../console/org.linkedin.glu.console-server/src/cmdline/resources/glu/repository/systems')
    return new File(r).canonicalFile
  }

  protected Resource getExampleSystemsRootResource()
  {
    rootShell.toResource(exampleSystemsRoot)
  }

  public void testJsonGroovyDSL()
  {
    ['empty-system.json', 'hello-world-system.json', 'sample-webapp-system.json'].each { json ->
      Resource jsonResource = exampleSystemsRootResource.createRelative(json)
      SystemModel fromJson =
        new SystemModelBuilder().deserializeFromJsonResource(jsonResource).toModel()
      Resource jsonGroovyResource = exampleSystemsRootResource.createRelative("${json}.groovy")
      SystemModel fromJsonGroovy =
        new SystemModelBuilder().deserializeFromJsonResource(jsonGroovyResource).toModel()

      assertEquals("same system for ${json}", fromJson, fromJsonGroovy)

      JsonSystemModelRenderer renderer = new JsonSystemModelRenderer()

      assertEquals("same id for ${json}",
                   renderer.computeSystemId(fromJson),
                   renderer.computeSystemId(fromJsonGroovy))

      assertEquals("same content for ${json}",
                   renderer.prettyPrint(fromJson),
                   renderer.prettyPrint(fromJsonGroovy))
    }
  }

  public void testParseException()
  {
    def invalid = """// line 1
// line 2
// line 3
// line 4
// line 5
// line 6
// line 7
fabric =
// line 8
// line 9
// line 10
// line 11
// line 12
// line 13
// line 14
"""
    try
    {
      new SystemModelBuilder().deserializeFromJsonGroovyDsl(invalid)
      fail("should fail")
    }
    catch(ModelBuilderParseException e)
    {
      assertEquals("""////////////////////
[1] // line 1
[2] // line 2
[3] // line 3
[4] // line 4
[5] // line 5
[6] // line 6
[7] // line 7
[8] fabric =
[9] // line 8
[10] // line 9
[11] // line 10
[12] // line 11
[13] // line 12
[14] // line 13
[15] // line 14
////////////////////""", e.excerpt)
    }

    try
    {
      new SystemModelBuilder().deserializeFromJsonString(invalid)
      fail("should fail")
    }
    catch(ModelBuilderParseException e)
    {
      assertEquals("""////////////////////
[3] // line 3
[4] // line 4
[5] // line 5
[6] // line 6
[7] // line 7
[8] fabric =
[9] // line 8
[10] // line 9
[11] // line 10
[12] // line 11
[13] // line 12
////////////////////""", e.excerpt)
    }

  }
}