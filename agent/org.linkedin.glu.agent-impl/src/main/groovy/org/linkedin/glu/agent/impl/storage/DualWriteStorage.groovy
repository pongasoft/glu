/*
 * Copyright 2010-2010 LinkedIn, Inc
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

/**
 * All the reads are delegated to the <code>readWriteStorage</code>, all the writes are dispatched
 * to both. This class is thread safe.
 *
 * @author ypujante@linkedin.com
 */
class DualWriteStorage extends FilteredStorage
{
  private final WriteOnlyStorage _writeOnlyStorage

  DualWriteStorage(Storage readWriteStorage, WriteOnlyStorage writeOnlyStorage)
  {
    super(readWriteStorage);
    _writeOnlyStorage = writeOnlyStorage
  }

  WriteOnlyStorage getWriteOnlyStorage()
  {
    return _writeOnlyStorage
  }

  synchronized void clearState(MountPoint mountPoint)
  {
    super.clearState(mountPoint);
    writeOnlyStorage.clearState(mountPoint)
  }

  synchronized void storeState(MountPoint mountPoint, state)
  {
    super.storeState(mountPoint, state);
    writeOnlyStorage.storeState(mountPoint, state)
  }

  synchronized public void clearAllStates()
  {
    super.clearAllStates();
    writeOnlyStorage.clearAllStates()
  }

  /**
   * Synchronizes the 2 storages (the source of truth is supposed to be the readWriteStorage).
   */
  synchronized public void sync()
  {
    writeOnlyStorage.clearAllStates()

    mountPoints.each { mountPoint ->
      writeOnlyStorage.storeState(mountPoint, loadState(mountPoint))
    }
  }
}
