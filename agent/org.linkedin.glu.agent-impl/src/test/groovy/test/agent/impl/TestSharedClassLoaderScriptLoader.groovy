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

import org.linkedin.glu.agent.impl.script.SharedClassLoaderScriptLoader
import org.linkedin.glu.groovy.utils.io.GluGroovyIOUtils
import org.linkedin.glu.groovy.utils.shell.ShellImpl
import org.linkedin.util.io.resource.Resource

/**
 * @author yan@pongasoft.com  */
public class TestSharedClassLoaderScriptLoader extends GroovyTestCase
{
  public void testLoadSingleFile()
  {
    def singleFile = new File('./src/test/resources/MyScriptTestAgentImpl4.groovy').canonicalFile

    SharedClassLoaderScriptLoader loader = new SharedClassLoaderScriptLoader()

    // should start empty
    assertEquals(0, loader.classLoaders.size())

    def sl1 = loader.loadScript(singleFile)

    Class<?> sl1Class = sl1.script.getClass()
    assertEquals(1, loader.classLoaders.size())
    assertEquals('MyScriptTestAgentImpl4', sl1Class.name)
    // somehow sl1Class class loader is some inner class of groovy class loader with a delegate of
    // the actual one...
    assertEquals(sl1Class,
                 loader.classLoaders[sl1.key].groovyClassLoader.loadedClasses.find { it == sl1Class })

    def sl2 = loader.loadScript(singleFile)

    Class<?> sl2Class = sl2.script.getClass()
    assertEquals(1, loader.classLoaders.size())
    assertEquals('MyScriptTestAgentImpl4', sl2Class.name)
    assertEquals(sl1Class, sl2Class)

    loader.unloadScript(sl2)
    // no change!
    assertEquals(1, loader.classLoaders.size())

    loader.unloadScript(sl1)
    // back down to 0
    assertEquals(0, loader.classLoaders.size())

    // now it will be a brand new class loader => sl1 and sl3 are different classes!
    def sl3 = loader.loadScript(singleFile)

    Class<?> sl3Class = sl3.script.getClass()
    assertEquals(1, loader.classLoaders.size())
    assertEquals('MyScriptTestAgentImpl4', sl3Class.name)
    assertNotSame(sl1Class, sl3Class)

    // loading another different class
    def singleFile2 = new File('./src/test/resources/MyScriptTestFailure.groovy').canonicalFile

    def sl4 = loader.loadScript(singleFile2)
    Class<?> sl4Class = sl4.script.getClass()
    assertEquals(2, loader.classLoaders.size())
    assertEquals('MyScriptTestFailure', sl4Class.name)
    assertNotSame(sl1Class, sl4Class)
    assertNotSame(sl3Class, sl4Class)
    assertEquals(sl3Class,
                 loader.classLoaders[sl3.key].groovyClassLoader.loadedClasses.find { it == sl3Class })
    assertEquals(sl4Class,
                 loader.classLoaders[sl4.key].groovyClassLoader.loadedClasses.find { it == sl4Class })

    loader.unloadScript(sl3)
    assertEquals(1, loader.classLoaders.size())
    loader.unloadScript(sl4)
    assertEquals(0, loader.classLoaders.size())
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

      SharedClassLoaderScriptLoader loader = new SharedClassLoaderScriptLoader()


      // should start empty
      assertEquals(0, loader.classLoaders.size())

      def sl1 = loader.loadScript('test.agent.script.MyScript1',
                                  [scriptJarFile.file, dependenciesJarFile.file])

      Class<?> sl1Class = sl1.script.getClass()
      assertEquals(1, loader.classLoaders.size())
      assertEquals('test.agent.script.MyScript1', sl1Class.name)
      assertEquals(sl1Class.classLoader, loader.classLoaders[sl1.key].groovyClassLoader)

      def sl2 = loader.loadScript('test.agent.script.MyScript1',
                                  [scriptJarFile.file, dependenciesJarFile.file])

      Class<?> sl2Class = sl2.script.getClass()
      assertEquals(1, loader.classLoaders.size())
      assertEquals('test.agent.script.MyScript1', sl2Class.name)
      assertEquals(sl1Class, sl2Class)
      assertEquals(sl2Class.classLoader, loader.classLoaders[sl2.key].groovyClassLoader)

      // different classes but same class path => loaded with SAME class loader
      def sl3 = loader.loadScript('test.agent.script.MyScript2',
                                  [scriptJarFile.file, dependenciesJarFile.file])

      Class<?> sl3Class = sl3.script.getClass()
      assertEquals(1, loader.classLoaders.size())
      assertEquals('test.agent.script.MyScript2', sl3Class.name)
      assertNotSame(sl1Class, sl3Class)
      assertEquals(sl3Class.classLoader, loader.classLoaders[sl3.key].groovyClassLoader)

      loader.unloadScript(sl2)
      assertEquals(1, loader.classLoaders.size())

      loader.unloadScript(sl1)
      assertEquals(1, loader.classLoaders.size())

      loader.unloadScript(sl3)
      assertEquals(0, loader.classLoaders.size())

      // now it should be a brand new class loader
      def sl4 = loader.loadScript('test.agent.script.MyScript1',
                                  [scriptJarFile.file, dependenciesJarFile.file])

      Class<?> sl4Class = sl4.script.getClass()
      assertEquals(1, loader.classLoaders.size())
      assertEquals('test.agent.script.MyScript1', sl4Class.name)
      assertNotSame(sl1Class, sl4Class)
      assertEquals(sl4Class.classLoader, loader.classLoaders[sl4.key].groovyClassLoader)

      // if using a different class path => new class loader!
      def sl5 = loader.loadScript('test.agent.script.MyScript1',
                                  [dependenciesJarFile.file, scriptJarFile.file])

      Class<?> sl5Class = sl5.script.getClass()
      assertEquals(2, loader.classLoaders.size())
      assertEquals('test.agent.script.MyScript1', sl5Class.name)
      assertNotSame(sl4Class, sl5Class)
      assertEquals(sl5Class.classLoader, loader.classLoaders[sl5.key].groovyClassLoader)

      loader.unloadScript(sl4)
      assertEquals(1, loader.classLoaders.size())
      assertEquals(sl5.key, (loader.classLoaders.values() as List)[0].key)

      loader.unloadScript(sl5)
      assertEquals(0, loader.classLoaders.size())
    }
  }

}