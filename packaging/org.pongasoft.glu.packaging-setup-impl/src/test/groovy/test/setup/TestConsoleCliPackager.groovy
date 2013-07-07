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
import org.pongasoft.glu.packaging.setup.ConsoleCliPackager
import org.pongasoft.glu.packaging.setup.PackagedArtifact

/**
 * @author yan@pongasoft.com   */
public class TestConsoleCliPackager extends BasePackagerTest
{
  public void testTutorialModel()
  {
    ShellImpl.createTempShell { Shell shell ->

      def inputPackage = shell.mkdirs("/dist/org.linkedin.glu.console-cli-${GLU_VERSION}")
      shell.saveContent(inputPackage.createRelative('README.md'), "this is the readme")
      shell.saveContent(inputPackage.createRelative('lib/acme.py'), "this is the python")

      def packager = new ConsoleCliPackager(packagerContext: createPackagerContext(shell),
                                            outputFolder: shell.mkdirs('/out'),
                                            inputPackage: inputPackage,
                                            configsRoots: copyConfigs(shell.toResource('/configs')),
                                            metaModel: testModel)

      PackagedArtifact artifact = packager.createPackage()

      assertEquals(shell.toResource("/out/org.linkedin.glu.console-cli-${GLU_VERSION}"), artifact.location)
      assertNull(artifact.host)
      assertEquals(0, artifact.port)

      def expectedResources =
        [
          "/README.md": 'this is the readme',
          "/lib": DIRECTORY,
          "/lib/acme.py": 'this is the python',
        ]

      checkPackageContent(expectedResources, artifact.location)
    }
  }
}