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
package test.agent.impl

import org.linkedin.glu.agent.impl.script.NoSharedClassLoaderScriptLoader
import org.linkedin.glu.groovy.utils.io.GluGroovyIOUtils
import org.linkedin.glu.groovy.utils.shell.ShellImpl
import org.linkedin.util.io.resource.Resource

/**
 * @author yan@pongasoft.com  */
public class TestNoSharedClassLoaderScriptLoader extends GroovyTestCase
{
  public void testLoadSingleFile()
  {
    def singleFile = new File('./src/test/resources/MyScriptTestAgentImpl4.groovy').canonicalFile

    NoSharedClassLoaderScriptLoader loader = new NoSharedClassLoaderScriptLoader()

    def sl1 = loader.loadScript(singleFile)

    Class<?> sl1Class = sl1.script.getClass()
    assertEquals('MyScriptTestAgentImpl4', sl1Class.name)

    def sl2 = loader.loadScript(singleFile)

    Class<?> sl2Class = sl2.script.getClass()
    assertEquals('MyScriptTestAgentImpl4', sl2Class.name)
    assertNotSame(sl1Class, sl2Class)

    loader.unloadScript(sl2)
    loader.unloadScript(sl1)

    // try again
    def sl3 = loader.loadScript(singleFile)

    Class<?> sl3Class = sl3.script.getClass()
    assertEquals('MyScriptTestAgentImpl4', sl3Class.name)
    assertNotSame(sl1Class, sl3Class)

    loader.unloadScript(sl3)
  }

  private static String dependentClass = """
package test.agent.depend

class MyDependClass {}

"""

  private static String scriptClass1 = """
package test.agent.script

class MyScript1
{
  def install = { args ->
    log.info new test.agent.depend.MyDependClass()
  }
}
"""

  private static String scriptClass2 = """
package test.agent.script

class MyScript2
{
  def install = { args ->
    log.info new test.agent.depend.MyDependClass()
  }
}
"""

  public void testClassPath()
  {
    ShellImpl.createTempShell { ShellImpl shell ->
          // creating jar file with dependent class
    Resource dependenciesJarFile =
      GluGroovyIOUtils.compileAndJar(shell.fileSystem,
                                     [shell.saveContent("/src/classes/dependencies.groovy",
                                                             dependentClass).file],
                                     shell.toResource('/out/jars/dependencies.jar'))

    // creating jar file containing glu scripts
    Resource scriptJarFile =
      GluGroovyIOUtils.compileAndJar(shell.fileSystem,
                                     [shell.saveContent("/src/classes/script1.groovy",
                                                        scriptClass1).file,
                                       shell.saveContent("/src/classes/script2.groovy",
                                                         scriptClass2).file],
                                     shell.toResource('/out/jars/script.jar'),
                                     [dependenciesJarFile])

      NoSharedClassLoaderScriptLoader loader = new NoSharedClassLoaderScriptLoader()


      def sl1 = loader.loadScript('test.agent.script.MyScript1',
                                  [scriptJarFile.file, dependenciesJarFile.file])

      Class<?> sl1Class = sl1.script.getClass()
      assertEquals('test.agent.script.MyScript1', sl1Class.name)

      def sl2 = loader.loadScript('test.agent.script.MyScript1',
                                  [scriptJarFile.file, dependenciesJarFile.file])

      Class<?> sl2Class = sl2.script.getClass()
      assertEquals('test.agent.script.MyScript1', sl2Class.name)
      assertNotSame(sl1Class, sl2Class)

      // different classes but same class path => loaded with SAME class loader
      def sl3 = loader.loadScript('test.agent.script.MyScript2',
                                  [scriptJarFile.file, dependenciesJarFile.file])

      Class<?> sl3Class = sl3.script.getClass()
      assertEquals('test.agent.script.MyScript2', sl3Class.name)
      assertNotSame(sl1Class, sl3Class)
      assertNotSame(sl3Class.classLoader, sl1Class.classLoader)

      loader.unloadScript(sl2)
      loader.unloadScript(sl1)
      loader.unloadScript(sl3)
    }
  }

}