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


package org.linkedin.glu.agent.impl.storage

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.api.NoSuchMountPointException
import org.linkedin.groovy.util.io.fs.FileSystem

/**
 * Store in the filesystem
 *
 * @author ypujante@linkedin.com
 */
class FileSystemStorage implements Storage
{
  private final FileSystem _fileSystem

  FileSystemStorage(fileSystem)
  {
    _fileSystem = fileSystem;
  }

  public void clearState(MountPoint mountPoint)
  {
    def state = _fileSystem.toResource(toPath(mountPoint))
    _fileSystem.rm(state)
  }

  public void clearAllStates()
  {
    _fileSystem.ls() { resource ->
      _fileSystem.rm(resource)
    }
  }

  public getMountPoints()
  {
    return _fileSystem.ls().collect { resource ->
      fromPath(resource.filename)
    }
  }

  public loadState(MountPoint mountPoint)
  {
    try
    {
      return _fileSystem.deserializeFromFile(toPath(mountPoint))
    }
    catch (FileNotFoundException e)
    {
      throw new NoSuchMountPointException(mountPoint?.path, e)
    }
  }

  public void storeState(MountPoint mountPoint, state)
  {
    _fileSystem.serializeToFile(toPath(mountPoint), state)
  }

  FileSystem getFileSystem()
  {
    return _fileSystem
  }

  private String toPath(MountPoint mp)
  {
    return mp.path.replace('/', '_')
  }

  private MountPoint fromPath(String path)
  {
    return MountPoint.create(path.replace('_', '/'))
  }
}
