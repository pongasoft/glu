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

package test.setup

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.glu.groovy.utils.shell.ShellImpl
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.packaging.setup.PackagerContext
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.impl.builder.GluMetaModelBuilder

/**
 * @author yan@pongasoft.com  */
public abstract class BasePackagerTest extends GroovyTestCase
{
  public static final Object DIRECTORY = new Object()

  public static class BinaryResource
  {
    Resource resource
  }

  /**
   * Convenient call from subclasses
   */
  public static BinaryResource toBinaryResource(Resource resource)
  {
    new BinaryResource(resource: resource)
  }

  public static final int CONFIG_TEMPLATES_COUNT = 8

  public static final String GLU_VERSION = 'g.v.0'
  public static final String ZOOKEEPER_VERSION = 'z.v.1'

  Shell rootShell = ShellImpl.createRootShell()
  GluMetaModel testModel

  @Override
  protected void setUp() throws Exception
  {
    super.setUp()
    GluMetaModelBuilder builder = new GluMetaModelBuilder()
    builder.deserializeFromJson(rootShell.replaceTokens(testModelFile.text,
                                                        [
                                                          'glu.version': GLU_VERSION,
                                                          'zooKeeper.version': ZOOKEEPER_VERSION
                                                        ]))
    testModel = builder.toGluMetaModel()
  }

  protected GluMetaModel toGluMetaModel(String gluMetaModelString)
  {
    GluMetaModelBuilder builder = new GluMetaModelBuilder()

    builder.deserializeFromJson(gluMetaModelString)

    return builder.toGluMetaModel()
  }

  protected File getTestModelFile()
  {
    new File('../org.linkedin.glu.packaging-all/src/cmdline/resources/conf/tutorial/glu-meta-model.json.groovy').canonicalFile
  }

  protected File getConfigsRoot()
  {
    new File('../org.linkedin.glu.packaging-setup/src/cmdline/resources/configs').canonicalFile
  }

  protected Resource getConfigsRootResource()
  {
    rootShell.toResource(configsRoot)
  }

  protected File getKeysRootDir()
  {
    new File('../../dev-keys').canonicalFile
  }

  protected Resource getKeysRootResource()
  {
    rootShell.toResource(keysRootDir)
  }

  protected PackagerContext createPackagerContext(Shell shell)
  {
    new PackagerContext(shell: shell,
                        keysRootDir: keysRootResource)
  }

  /**
   * Copy all the configs from the config root
   */
  protected Resource copyConfigs(Resource toConfigRoot)
  {
    Resource dir = rootShell.cp(configsRootResource, toConfigRoot)
    int configFilesCount = 0
    rootShell.eachChildRecurse(dir) { Resource r ->
      if(!r.isDirectory())
        configFilesCount++
    }
    assertEquals(CONFIG_TEMPLATES_COUNT, configFilesCount)
    return toConfigRoot
  }

  protected void checkContent(String expectedContent, Shell shell, String templateName, def tokens)
  {
    def processed = shell.processTemplate("/templates/${templateName}", '/out', tokens)
    assertEquals(expectedContent.trim(), shell.cat(processed).trim())
  }


  protected void checkPackageContent(def expectedResources, Resource pkgRoot)
  {
    // GString -> String conversion for proper map key handling
    expectedResources = expectedResources.collectEntries {k,v -> [k.toString(), v]}

    GroovyIOUtils.eachChildRecurse(pkgRoot.chroot('.')) { Resource r ->
      def expectedValue = expectedResources.remove(r.path)
      if(expectedValue == null)
        fail("unexpected resource ${r}")

      if(expectedValue.is(DIRECTORY))
        assertTrue("${r} is directory", r.isDirectory())
      else
      {
        if(expectedValue instanceof BinaryResource)
          assertEquals("binary content differ for ${r}",
                       rootShell.sha1(expectedValue.resource), rootShell.sha1(r))
        else
          assertEquals(expectedValue, r.file.text)
      }
    }

    assertTrue("${expectedResources.keySet()} is not empty", expectedResources.isEmpty())
  }

}