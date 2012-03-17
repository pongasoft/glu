/*
 * Copyright (c) 2012 Yan Pujante
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

package test.utils.state

// this will force the loading of the class thus already executing the static block right away
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.groovy.util.ant.AntUtils
import org.linkedin.util.reflect.ReflectUtils
import java.util.concurrent.Callable

/**
 * Implementation note: since the initialization of the class happens in the static block which
 * happens only once when the class gets loaded, we never load the class directly using this
 * class loader. Instead we parse the class file in another class loader so that it can be
 * reinstantiated for each test method
 *
 * @author yan@pongasoft.com */
public class TestDefaultStateMachine extends GroovyTestCase
{
  def static DEFAULT_TRANSITIONS =
  [
    NONE: [[to: 'installed', action: 'install']],
    installed: [[to: 'stopped', action: 'configure'], [to: 'NONE', action: 'uninstall']],
    stopped: [[to: 'running', action: 'start'], [to: 'installed', action: 'unconfigure']],
    running: [[to: 'stopped', action: 'stop']]
  ]

  def static FQCN = "org.linkedin.glu.groovy.util.state.DefaultStateMachine"

  @Override
  protected void setUp()
  {
    super.setUp()

    System.clearProperty(FQCN)
  }

  /**
   * No default provided => default values
   */
  public void testNoDefaultProvided()
  {
    newStateMachine { sm ->
      assertEquals(DEFAULT_TRANSITIONS, sm.INSTANCE._transitions)
      assertEquals("running", sm.DEFAULT_ENTRY_STATE)
    }
  }

  /**
   * Test with system property
   */
  public void testSystemProperty()
  {
    def definition = """
defaultTransitions =
[
  NONE: [[to: 's1', action: '+a1']],
  s1: [[to: 'NONE', action: '-a1']]
]

defaultEntryState = 's1'
    """

    System.setProperty(FQCN, definition)

    newStateMachine { sm ->
      assertEquals(
        [
        NONE: [[to: 's1', action: '+a1']],
        s1: [[to: 'NONE', action: '-a1']]
        ]
        , sm.INSTANCE._transitions)
      assertEquals("s1", sm.DEFAULT_ENTRY_STATE)
    }
  }

  /**
   * Test with system property (no defaultEntryState provided)
   */
  public void testSystemProperty2()
  {
    def definition = """
defaultTransitions =
[
  NONE: [[to: 'running', action: '+a1']],
  running: [[to: 'NONE', action: '-a1']]
]
    """

    System.setProperty(FQCN, definition)

    newStateMachine { sm ->
      assertEquals(
        [
        NONE: [[to: 'running', action: '+a1']],
        running: [[to: 'NONE', action: '-a1']]
        ]
        , sm.INSTANCE._transitions)
      assertEquals("running", sm.DEFAULT_ENTRY_STATE)
    }
  }

  /**
   * Test with file in classpath
   */
  public void testClasspathFile()
  {
    FileSystemImpl.createTempFileSystem { FileSystem fs ->

      def definition = """
  defaultTransitions =
  [
    NONE: [[to: 's2', action: '+a2']],
    s2: [[to: 'NONE', action: '-a2']]
  ]

  defaultEntryState = 's2'
      """

      fs.saveContent("/files/glu/DefaultStateMachine.groovy", definition)

      def jarFile = fs.toResource('/jars/dsm.jar')

      AntUtils.withBuilder { ant ->
        ant.jar(destfile: jarFile.file, basedir: fs.toResource('/files').file)
      }

      def parentLoader = new URLClassLoader([jarFile.toURI().toURL()] as URL[],
                                            this.getClass().getClassLoader())

      newStateMachine(parentLoader) { sm ->
        assertEquals(
          [
          NONE: [[to: 's2', action: '+a2']],
          s2: [[to: 'NONE', action: '-a2']]
          ]
          , sm.INSTANCE._transitions)
        assertEquals("s2", sm.DEFAULT_ENTRY_STATE)
      }
    }
  }

  void newStateMachine(Closure cl)
  {
    newStateMachine(this.getClass().getClassLoader(), cl)
  }

  void newStateMachine(ClassLoader parentClassLoader, Closure cl)
  {
    def file =
      new File('./src/main/groovy/org/linkedin/glu/groovy/util/state/DefaultStateMachine.groovy')

    def loader = new GroovyClassLoader(parentClassLoader)

    ReflectUtils.executeWithClassLoader(loader, {
      cl(loader.parseClass(file).newInstance())
    } as Callable)
  }
}