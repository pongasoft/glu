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
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.util.io.resource.Resource

import java.nio.file.Files

/**
 * @author yan@pongasoft.com  */
public abstract class BasePackagerTest extends GroovyTestCase
{
  public static Object DIRECTORY = new Object()

  protected File getTemplatesRoot(String name)
  {
    new File('../org.linkedin.glu.packaging-setup/src/cmdline/resources/templates', name)
  }

  protected int copyTemplates(Shell shell, String name)
  {
    Shell templateShell = new ShellImpl(fileSystem: new FileSystemImpl(getTemplatesRoot(name)))

    def templates = shell.mkdirs('/templates')

    int count = 0

    templateShell.root.list().each { template ->
      Files.copy(template.file.toPath(), templates.createRelative(template.filename).file.toPath())
      count++
    }

    return count
  }

  protected void checkContent(String expectedContent, Shell shell, String templateName, def tokens)
  {
    def processed = shell.processTemplate("/templates/${templateName}", '/out', tokens)
    assertEquals(expectedContent.trim(), shell.cat(processed).trim())
  }


  protected void checkPackageContent(def expectedResources, Resource pkgRoot)
  {
    GroovyIOUtils.eachChildRecurse(pkgRoot.chroot('.')) { Resource r ->
      def expectedValue = expectedResources.remove(r.path)
      if(expectedValue == null)
        fail("unexpected resource ${r}")

      if(expectedValue.is(DIRECTORY))
        assertTrue("${r} is directory", r.isDirectory())
      else
        assertEquals(expectedValue, r.file.text)
    }

    assertTrue("${expectedResources} is not empty", expectedResources.isEmpty())
  }

}