/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

import org.linkedin.glu.agent.impl.capabilities.ShellImpl
import org.linkedin.glu.agent.impl.script.ScriptDefinition
import org.linkedin.glu.agent.impl.storage.FileSystemStorage
import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.api.NoSuchMountPointException
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.glu.agent.impl.storage.AgentProperties

/**
 * Tests for file system storage (actually writing to the disk)
 *
 * @author ypujante@linkedin.com
 */
class TestFileSystemStorage extends GroovyTestCase
{
  FileSystem stateFileSystem
  FileSystem agentPropertiesFileSystem
  FileSystemStorage storage
  AgentProperties agentProperties
  File rootFile
  File agentPropertiesFile

  protected void setUp()
  {
    super.setUp();

    stateFileSystem = FileSystemImpl.createTempFileSystem()
    rootFile = stateFileSystem.root.file

    agentPropertiesFileSystem = FileSystemImpl.createTempFileSystem()

    agentPropertiesFile = agentPropertiesFileSystem.toResource('/config/agent.properties').file

    storage = new FileSystemStorage(stateFileSystem, agentProperties, agentPropertiesFile)
  }

  protected void tearDown()
  {
    stateFileSystem.destroy()
    agentPropertiesFileSystem.destroy()
  }

  /**
   * Basic test for file system storage
   */
  void testFileSystemStorage()
  {
    // first it is empty...
    assertEquals(0, storage.mountPoints.size())

    MountPoint mp = MountPoint.create('/a/b/c')

    // nothing under /a/b/c
    shouldFail(NoSuchMountPointException) {
      storage.loadState(mp)
    }

    // we store some state
    assertFalse new File(rootFile, '_a_b_c').exists()
    storage.storeState(mp, [p1: 'v1', scriptDefinition: [mountPoint: mp]])
    assertTrue new File(rootFile, '_a_b_c').exists()

    // 1 mount point in the storage...
    assertEquals([mp], storage.mountPoints)

    // we read it back
    assertEquals([p1: 'v1', scriptDefinition: [mountPoint: mp]], storage.loadState(mp))

    // we make sure we can write another value
    storage.storeState(mp, [p1: 'v2', scriptDefinition: [mountPoint: mp]])
    assertEquals([p1: 'v2', scriptDefinition: [mountPoint: mp]], storage.loadState(mp))

    // we store 2 more values
    storage.storeState(MountPoint.create('/d'), [p2: 'v2', scriptDefinition: [mountPoint: MountPoint.create('/d')]])
    storage.storeState(MountPoint.create('/a/b'), [p2: 'v2', scriptDefinition: [mountPoint: MountPoint.create('/a/b')]])

    // we make sure that they are returned
    assertEquals([mp,
                 MountPoint.create('/a/b'),
                 MountPoint.create('/d')].sort(), storage.mountPoints.sort())

    // we remove the state
    storage.clearState(MountPoint.create('/a/b'))
    assertEquals([mp, MountPoint.create('/d')], storage.mountPoints)

    // nothing under /a/b/c
    shouldFail(NoSuchMountPointException) {
      storage.loadState(MountPoint.create('/a/b'))
    }

    assertEquals([mp,
                 MountPoint.create('/d')].sort(), storage.mountPoints.sort())

    // trying to store an invalid state
    shouldFail(IllegalArgumentException) {
      storage.storeState(MountPoint.create('/foo'),
                         [p1: 'v3', scriptDefinition: [mountPoint: MountPoint.create('/foo2')]])
    }

    // bypassing the api to generate a valid state file
    stateFileSystem.serializeToFile('_foo',
                                    [p1: 'v3', scriptDefinition: [mountPoint: MountPoint.create('/foo')]])

    assertEquals([p1: 'v3', scriptDefinition: [mountPoint: MountPoint.create('/foo')]],
                 storage.loadState(MountPoint.create('/foo')))

    assertEquals([mp,
                 MountPoint.create('/d'),
                 MountPoint.create('/foo')].sort(), storage.mountPoints.sort())

    // bypassing the api to generate a non valid state file (mountpoint mismatch)
    stateFileSystem.serializeToFile('_foo',
                                    [p1: 'v3', scriptDefinition: [mountPoint: MountPoint.create('/foo2')]])

    shouldFail(NoSuchMountPointException) {
      storage.loadState(MountPoint.create('/foo'))
    }

    assertEquals([mp,
                 MountPoint.create('/d')].sort(), storage.mountPoints.sort())

    // bypassing the api to generate a non valid state file (non deserializable)
    stateFileSystem.saveContent('_foo1', "oasasoasdokdasok")

    shouldFail(NoSuchMountPointException) {
      storage.loadState(MountPoint.create('/foo1'))
    }

    assertEquals([mp,
                 MountPoint.create('/d')].sort(), storage.mountPoints.sort())

    // should be 4 entries in the folder
    assertEquals(4, stateFileSystem.ls().size())

    // deleting invalid states
    def invalidStates = storage.deleteInvalidStates()

    // should be 2 entries in the folder now
    assertEquals(2, stateFileSystem.ls().size())

    assertEquals([stateFileSystem.toResource('_foo'), stateFileSystem.toResource('_foo1')], invalidStates)

  }

  /**
   * Test for bug #151
   */
  public void testMountPointWithUnderscore()
  {
    // first it is empty...
    assertEquals(0, storage.mountPoints.size())

    MountPoint mp = MountPoint.create('/a/_b_%1_0_/c')

    // we store some state
    def file = new File(rootFile, '_a_%5Fb%5F%251%5F0%5F_c')

    assertFalse file.exists()
    storage.storeState(mp, [p1: 'v1', scriptDefinition: [mountPoint: mp]])
    assertTrue file.exists()

    // 1 mount point in the storage...
    assertEquals([mp], storage.mountPoints)

    // we read it back
    assertEquals([p1: 'v1', scriptDefinition: [mountPoint: mp]], storage.loadState(mp))
  }

  /**
   * Some "files" may be invalid in which case it should ignore them
   */
  public void testInvalidEntries()
  {
    MountPoint mp = MountPoint.create('/a/b/c')
    storage.storeState(mp, [p1: 'v1', scriptDefinition: [mountPoint: mp]])
    assertEquals(['/a/b/c'], storage.mountPoints.path)

    // mountPoint mismatch
    stateFileSystem.cp("_a_b_c", '_q')
    assertEquals(['/a/b/c'], storage.mountPoints.path)

    // add one /a
    storage.storeState(MountPoint.create('/a'), [p1: 'v1',
                                                 scriptDefinition: [mountPoint: MountPoint.create('/a')]])
    assertEquals(['/a', '/a/b/c'], storage.mountPoints.path.sort(GluGroovyLangUtils.COMPARATOR_CLOSURE))

    // bad name (need to start with _)
    stateFileSystem.cp("_a", "a")
    assertEquals(['/a', '/a/b/c'], storage.mountPoints.path.sort(GluGroovyLangUtils.COMPARATOR_CLOSURE))
  }

  /**
   * delete invalid states should properly delete invalid states as well as "upgrade" the format
   * of old serialized entries
   */
  public void testDeleteInvalidStates()
  {
    MountPoint mp = MountPoint.create('/a/b/c')
    storage.storeState(mp, [p1: 'v1', scriptDefinition: [mountPoint: mp]])

    // mountPoint mismatch
    stateFileSystem.cp("_a_b_c", '_q')

    // should start with a
    stateFileSystem.cp("_a_b_c", 'a_b_c')

    // pre 4.7.1 format (should be converted...)
    def shell = new ShellImpl(fileSystem: stateFileSystem)
    shell.fetch("./src/test/resources/_sample_i001.glu4.6.2.ser",
                "_sample_i001")
    def state = storage.loadState(MountPoint.create('/sample/i001'))
    assertTrue(state.scriptDefinition instanceof ScriptDefinition)

    // non deserializable
    stateFileSystem.saveContent('_foo1', "oasasoasdokdasok")

    // now we call the method and make sure it does the right thing
    storage.deleteInvalidStates()

    assertEquals(['_a_b_c', '_sample_i001'],
                 stateFileSystem.ls().collect { it.file.name }.sort(GluGroovyLangUtils.COMPARATOR_CLOSURE))

    state = storage.loadState(MountPoint.create('/sample/i001'))
    assertTrue(state.scriptDefinition instanceof Map)
  }
}
