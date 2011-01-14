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

/**
 * Implement a filter/decorator pattern... subclasses can simply overrides only the methods
 * they care about.
 *
 * @author ypujante@linkedin.com
 */
class FilteredStorage implements Storage
{
  final Storage _storage

  def FilteredStorage(Storage storage)
  {
    _storage = storage;
  }

  Storage getStorage()
  {
    return _storage
  }

  void clearState(MountPoint mountPoint)
  {
    storage.clearState(mountPoint)
  }

  public void clearAllStates()
  {
    storage.clearAllStates()
  }

  def getMountPoints()
  {
    return storage.mountPoints;
  }

  def loadState(MountPoint mountPoint)
  {
    return storage.loadState(mountPoint);
  }

  void storeState(MountPoint mountPoint, state)
  {
    storage.storeState(mountPoint, state)
  }
}
