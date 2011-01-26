/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Copyright (c) 2011 Yan Pujante
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

import org.linkedin.glu.agent.impl.storage.FileSystemStorage
import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.api.NoSuchMountPointException
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
    storage.storeState(mp, [p1: 'v1'])
    assertTrue new File(rootFile, '_a_b_c').exists()

    // 1 mount point in the storage...
    assertEquals([mp], storage.mountPoints)

    // we read it back
    assertEquals([p1: 'v1'], storage.loadState(mp))

    // we make sure we can write another value
    storage.storeState(mp, [p1: 'v2'])
    assertEquals([p1: 'v2'], storage.loadState(mp))

    // we store 2 more values
    storage.storeState(MountPoint.create('/d'), [p2: 'v2'])
    storage.storeState(MountPoint.create('/a/b'), [p2: 'v2'])

    // we make sure that they are returned
    assertEquals([mp, MountPoint.create('/a/b'), MountPoint.create('/d')].sort(), storage.mountPoints.sort())

    // we remove the state
    storage.clearState(MountPoint.create('/a/b'))
    assertEquals([mp, MountPoint.create('/d')], storage.mountPoints)

    // nothing under /a/b/c
    shouldFail(NoSuchMountPointException) {
      storage.loadState(MountPoint.create('/a/b'))
    }
  }
}
