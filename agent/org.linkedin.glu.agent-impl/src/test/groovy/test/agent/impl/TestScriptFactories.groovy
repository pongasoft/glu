/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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
import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.impl.storage.RAMStorage
import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.groovy.util.io.fs.FileSystemImpl

/**
 * Test for script factories.
 *
 * @author ypujante@linkedin.com
 */
class TestScriptFactories extends GroovyTestCase
{
  FileSystem fileSystem
  Agent agent
  def ramStorage = [:]

  protected void setUp()
  {
    super.setUp();

    fileSystem = FileSystemImpl.createTempFileSystem()
    def shell = new ShellImpl(fileSystem: fileSystem)
    agent = new AgentImpl()
    agent.boot(shellForScripts: shell, storage: new RAMStorage(ramStorage))
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
}
