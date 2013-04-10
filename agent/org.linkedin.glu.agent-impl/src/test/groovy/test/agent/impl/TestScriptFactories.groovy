/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.impl.capabilities.ShellImpl
import org.linkedin.glu.agent.impl.AgentImpl
import org.linkedin.glu.agent.impl.script.FromClassNameScriptFactory
import org.linkedin.glu.agent.impl.script.FromLocationScriptFactory
import org.linkedin.glu.agent.impl.script.ScriptDefinition
import org.linkedin.glu.agent.impl.script.ScriptFactoryFactoryImpl
import org.linkedin.glu.agent.impl.storage.RAMStorage
import org.linkedin.glu.groovy.utils.io.GluGroovyIOUtils
import org.linkedin.glu.groovy.utils.test.GluGroovyTestUtils
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.glu.agent.impl.storage.AgentProperties
import org.linkedin.glu.agent.api.AgentException
import org.linkedin.glu.agent.api.ScriptExecutionCauseException
import org.linkedin.groovy.util.io.fs.SerializableFileResource
import org.linkedin.util.io.resource.Resource
import org.linkedin.util.url.URLBuilder

/**
 * Test for script factories.
 *
 * @author ypujante@linkedin.com
 */
class TestScriptFactories extends GroovyTestCase
{
  FileSystemImpl fileSystem
  def shell
  AgentImpl agent
  def ramStorage = [:]
  AgentProperties agentProperties = new AgentProperties()

  protected void setUp()
  {
    super.setUp();

    fileSystem = FileSystemImpl.createTempFileSystem()
    shell = new ShellImpl(fileSystem: fileSystem)
    agent = new AgentImpl()
    agent.boot(shellForScripts: shell, storage: new RAMStorage(ramStorage, agentProperties))
  }

  private void restartAgent()
  {
    agent.shutdown()
    agent.waitForShutdown(0)
    agent = new AgentImpl()
    agent.boot(shellForScripts: shell, storage: new RAMStorage(ramStorage, agentProperties))
  }

  protected void tearDown()
  {
    try
    {
      agent.shutdown()
      agent.waitForShutdown(0)
    }
    finally
    {
      fileSystem.destroy()
    }
  }

  /**
   * The purpose of this test is to make sure that it works with an external script as well
   */
  void testScriptLocation()
  {
    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        scriptLocation: new File('./src/test/resources/MyScriptTestAgentImpl4.groovy').canonicalFile.toURI())

    def file = fileSystem.tempFile(prefix: 'test', suffix: '.txt')
    fileSystem.saveContent(file, 'this is a test')

    assertTrue(file.exists())

    def res = [:]
    // then we run the 'install' action
    agent.executeAction(mountPoint: scriptMountPoint,
                        action: 'install',
                        actionArgs: [fileLocation: file.toURI(), fileContent: 'this is a test'])

    // then we wait for the action to be completed
    assertTrue agent.waitForState(mountPoint: scriptMountPoint, state: 'installed')

    // we verify that the install method really happened by checking the properties of the script
    def scriptStorage = ramStorage[scriptMountPoint]
    assertEquals(file.toURI(), scriptStorage.scriptState.script.originalLocation)

    // we make sure that the script really fetched the file
    def fetchedFile = scriptStorage.scriptState.script.file
    assertTrue(fetchedFile.exists())
    assertEquals('this is a test', fetchedFile.file.getText())

    // we uninstall the script which will delete the file previously fetched
    agent.executeAction(mountPoint: scriptMountPoint, action: 'uninstall')
    assertTrue agent.waitForState(mountPoint: scriptMountPoint, state: 'NONE')

    assertFalse(fetchedFile.exists())
  }

  /**
   * Test case for https://github.com/linkedin/glu/issues#issue/27
   */
  public void testFailingScript()
  {
    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        scriptLocation: new File('./src/test/resources/MyScriptTestFailure.groovy').canonicalFile.toURI())

    // then we run the 'install' action
    try
    {
      agent.executeActionAndWait(mountPoint: scriptMountPoint,
                                 action: 'install')
      fail("should throw exception")
    }
    catch(AgentException e)
    {
      Throwable th = e
      while(th.cause != null)
        th = th.cause

      assertTrue(th instanceof ScriptExecutionCauseException)
      assertEquals('groovy.lang.MissingPropertyException', th.originalClassname)
      assertEquals('[groovy.lang.MissingPropertyException]: No such property: args for class: MyScriptTestFailure', th.message)
    }

  }

  /**
   * We make sure that FromLocationScriptFactory "survives" agent restart (glu-207)
   */
  public void testFromLocationScriptFactorySerialization()
  {
      def script = fileSystem.saveContent("/scripts/test.groovy", """
class TestScript {
}
""")

    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        scriptLocation: script.toURI().toString())

    def node = agent.findScript(scriptMountPoint)

    assertEquals([
                   mountPoint: scriptMountPoint,
                   parent: MountPoint.ROOT,
                   scriptFactory: node.scriptDefinition.scriptFactory.toExternalRepresentation(),
                   initParameters: [:]
                 ],
                 ramStorage[scriptMountPoint].scriptDefinition)

    def savePriorToRestart1 = ramStorage[scriptMountPoint].scriptDefinition

    def localScriptFilePriorToRestart1 =
      node.scriptDefinition.scriptFactory.toExternalRepresentation().localScriptFile

    restartAgent()

    node = agent.findScript(scriptMountPoint)

    assertEquals([
                   mountPoint: scriptMountPoint,
                   parent: MountPoint.ROOT,
                   scriptFactory: node.scriptDefinition.scriptFactory.toExternalRepresentation(),
                   initParameters: [:]
                 ],
                 savePriorToRestart1)

    assertTrue(node.scriptDefinition.scriptFactory instanceof FromLocationScriptFactory)
    assertEquals(script.toURI().toString(),
                 node.scriptDefinition.scriptFactory.toExternalRepresentation().location)
    def localScriptFile =
      node.scriptDefinition.scriptFactory.toExternalRepresentation().localScriptFile
    assertEquals(localScriptFilePriorToRestart1, localScriptFile)
    assertTrue(localScriptFile instanceof SerializableFileResource)
    assertEquals(node.shell.tmpRoot, localScriptFile.rootResource)

    // we delete the local copy of the script => on reboot, the agent will refetch it
    shell.rm(localScriptFile)
    restartAgent()

    node = agent.findScript(scriptMountPoint)

    // new (local) copy of script
    localScriptFile =
      node.scriptDefinition.scriptFactory.toExternalRepresentation().localScriptFile
    assertNotSame(localScriptFilePriorToRestart1, localScriptFile)
    assertTrue(localScriptFile instanceof SerializableFileResource)
    assertEquals(node.shell.tmpRoot, localScriptFile.rootResource)
  }

  private static String dependentClass = """
package test.agent.depend

class MyDependClass {}

"""

  private static String scriptClass = """
package test.agent.script

class MyScript
{
  def install = { args ->
    log.info new test.agent.depend.MyDependClass()
  }
}
"""

  /**
   * We make sure that FromClassNameScriptFactory "survives" agent restart (glu-207)
   */
  public void testFromClassNameScriptFactorySerialization()
  {
    // creating jar file with dependent class
    Resource dependenciesJarFile =
      GluGroovyIOUtils.compileAndJar(fileSystem,
                                     [fileSystem.saveContent("/src/classes/dependencies.groovy",
                                                             dependentClass).file],
                                     fileSystem.toResource('/out/jars/dependencies.jar'))

    // creating jar file containing glu script
    Resource scriptJarFile =
      GluGroovyIOUtils.compileAndJar(fileSystem,
                                     [fileSystem.saveContent("/src/classes/script.groovy",
                                                             scriptClass).file],
                                     fileSystem.toResource('/out/jars/script.jar'),
                                     [dependenciesJarFile])

    URLBuilder scriptURL = new URLBuilder()
    scriptURL.scheme = "class"
    scriptURL.path = "/test.agent.script.MyScript"
    [dependenciesJarFile, scriptJarFile].each { cp ->
      scriptURL.addQueryParameter("cp", cp.toURI().toString())
    }

    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        scriptLocation: scriptURL.getURL())

    def node = agent.findScript(scriptMountPoint)

    assertEquals([
                   mountPoint: scriptMountPoint,
                   parent: MountPoint.ROOT,
                   scriptFactory: node.scriptDefinition.scriptFactory.toExternalRepresentation(),
                   initParameters: [:]
                 ],
                 ramStorage[scriptMountPoint].scriptDefinition)

    def savePriorToRestart1 = ramStorage[scriptMountPoint].scriptDefinition

    def localClassPathPriorToRestart1 =
      node.scriptDefinition.scriptFactory.toExternalRepresentation().localClassPath

    restartAgent()

    node = agent.findScript(scriptMountPoint)

    assertEquals([
                   mountPoint: scriptMountPoint,
                   parent: MountPoint.ROOT,
                   scriptFactory: node.scriptDefinition.scriptFactory.toExternalRepresentation(),
                   initParameters: [:]
                 ],
                 savePriorToRestart1)

    assertTrue(node.scriptDefinition.scriptFactory instanceof FromClassNameScriptFactory)
    assertEquals([dependenciesJarFile, scriptJarFile].collect { it.toURI().toString() },
                 node.scriptDefinition.scriptFactory.toExternalRepresentation().classPath)
    def localClassPath =
      node.scriptDefinition.scriptFactory.toExternalRepresentation().localClassPath
    assertEquals(localClassPathPriorToRestart1, localClassPath)
    localClassPath.each {
      assertTrue(it instanceof SerializableFileResource)
      assertEquals(node.shell.tmpRoot, it.rootResource)
    }

    // we delete a local jar file => on reboot, the agent will refetch it
    shell.rm(localClassPath[0])
    restartAgent()

    node = agent.findScript(scriptMountPoint)

    // new (local) copy of classpath
    localClassPath =
      node.scriptDefinition.scriptFactory.toExternalRepresentation().localClassPath
    assertNotSame(localClassPathPriorToRestart1, localClassPath)
    localClassPath.each {
      assertTrue(it instanceof SerializableFileResource)
      assertEquals(node.shell.tmpRoot, it.rootResource)
    }
  }

  /**
   * We make sure that CommandGluScriptFactory properly "survives" agent restart (although
   * in the agent, any remaining command will be cleaned on restart (check
   * {@link TestFileSystemStorage}).
   */
  public void testCommandGluScriptFactorySerialization()
  {
    // we install a script under /s
    def scriptMountPoint = MountPoint.fromPath('/s')
    agent.installScript(mountPoint: scriptMountPoint,
                        'class': 'CommandGluScriptFactory')

    def node = agent.findScript(scriptMountPoint)

    assertEquals([
                   mountPoint: scriptMountPoint,
                   parent: MountPoint.ROOT,
                   scriptFactory: node.scriptDefinition.scriptFactory.toExternalRepresentation(),
                   initParameters: [:]
                 ],
                 ramStorage[scriptMountPoint].scriptDefinition)

    def savePriorToRestart1 = ramStorage[scriptMountPoint].scriptDefinition

    restartAgent()

    // after restart, there should not be any command
    node = agent.findScript(scriptMountPoint)

    assertEquals([
                   mountPoint: scriptMountPoint,
                   parent: MountPoint.ROOT,
                   scriptFactory: node.scriptDefinition.scriptFactory.toExternalRepresentation(),
                   initParameters: [:]
                 ],
                 savePriorToRestart1)

  }

  /**
   * This test makes sure that previous version of state can be properly deserialized (note that
   * there is an issue from glu 4.6.0 to 4.7.0 (both included): glu-207 fixed in 4.7.1
   */
  public void testDeserializationBackwardCompatibility()
  {
    ScriptFactoryFactoryImpl sffi = new ScriptFactoryFactoryImpl()

    def state_4_5_2 =
      new File("src/test/resources/_sample_i001.glu4.5.2.ser").withObjectInputStream { ois ->
      ois.readObject()
    }

    def sd_4_5_2 = state_4_5_2.scriptDefinition

    // note that although this path may not exist, it does not matter as it is only used
    // to create a Resource which may or may not actually exist!
    def fs_4_5_2 =
      new FileSystemImpl(new File("/export/content/glu/org.linkedin.glu.packaging-all-4.5.2/agent-server/data/tmp/sample/i001"))

    def expected_4_5_2 =
      [
        mountPoint: MountPoint.create("/sample/i001"),
        parent: MountPoint.ROOT,
        scriptFactory:
          [
            class: "org.linkedin.glu.agent.impl.script.FromLocationScriptFactory",
            location: "http://localhost:8080/glu/repository/scripts/org.linkedin.glu.script-jetty-4.5.2/JettyGluScript.groovy",
            localScriptFile: fs_4_5_2.toResource('/__tmp618015841Dir/JettyGluScript.groovy')
          ],
        initParameters:
          [
            port:9000,
            tags:[ "frontend", "osx", "webapp"],
            webapps:
              [
                [
                  monitor: "/monitor",
                  contextPath: "/cp1",
                  war: "http://localhost:8080/glu/repository/wars/org.linkedin.glu.samples.sample-webapp-4.5.2.war"],
                [
                  monitor: "/monitor",
                  contextPath: "/cp2",
                  war: "http://localhost:8080/glu/repository/wars/org.linkedin.glu.samples.sample-webapp-4.5.2.war"]
              ],
            skeleton: "http://localhost:8080/glu/repository/tgzs/jetty-distribution-7.2.2.v20101205.tar.gz",
            metadata:
              [
                product: "product1",
                container: [name: "sample"], cluster:"c1", version:"1.0.0"
              ]
          ]
      ]

    GluGroovyTestUtils.assertEqualsIgnoreType(this,
                                              expected_4_5_2,
                                              sd_4_5_2.toExternalRepresentation())

    assertTrue(sd_4_5_2 instanceof ScriptDefinition)
    // recreate the object (simulate code in ScriptManagerImpl)
    sd_4_5_2 = new ScriptDefinition(sd_4_5_2.mountPoint,
                                    sd_4_5_2.parent,
                                    sffi.createScriptFactory(sd_4_5_2),
                                    sd_4_5_2.initParameters)

    assertTrue(sd_4_5_2.scriptFactory instanceof FromLocationScriptFactory)

    def state_4_6_2 =
      new File("src/test/resources/_sample_i001.glu4.6.2.ser").withObjectInputStream { ois ->
      ois.readObject()
    }

    def sd_4_6_2 = state_4_6_2.scriptDefinition

    def expected_4_6_2 =
      [
        mountPoint: MountPoint.create("/sample/i001"),
        parent: MountPoint.ROOT,
        scriptFactory:
          [
            class: "org.linkedin.glu.agent.impl.script.FromLocationScriptFactory",
            location: "http://localhost:8080/glu/repository/scripts/org.linkedin.glu.script-jetty-4.6.2/JettyGluScript.groovy"
            // localScriptFile: no script file: glu-207!!!
          ],
        initParameters:
          [
            port:9000,
            tags:[ "frontend", "osx", "webapp"],
            webapps:
              [
                [
                  monitor: "/monitor",
                  contextPath: "/cp1",
                  war: "http://localhost:8080/glu/repository/wars/org.linkedin.glu.samples.sample-webapp-4.6.2.war"],
                [
                  monitor: "/monitor",
                  contextPath: "/cp2",
                  war: "http://localhost:8080/glu/repository/wars/org.linkedin.glu.samples.sample-webapp-4.6.2.war"]
              ],
            skeleton: "http://localhost:8080/glu/repository/tgzs/jetty-distribution-7.2.2.v20101205.tar.gz",
            metadata:
              [
                product: "product1",
                container: [name: "sample"], cluster:"c1", version:"1.0.0"
              ]
          ]
        ]

    GluGroovyTestUtils.assertEqualsIgnoreType(this,
                                              expected_4_6_2,
                                              sd_4_6_2.toExternalRepresentation())

    assertTrue(sd_4_6_2 instanceof ScriptDefinition)
    // recreate the object (simulate code in ScriptManagerImpl)
    sd_4_6_2 = new ScriptDefinition(sd_4_6_2.mountPoint,
                                    sd_4_6_2.parent,
                                    sffi.createScriptFactory(sd_4_6_2),
                                    sd_4_6_2.initParameters)

    assertTrue(sd_4_6_2.scriptFactory instanceof FromLocationScriptFactory)
  }
}
