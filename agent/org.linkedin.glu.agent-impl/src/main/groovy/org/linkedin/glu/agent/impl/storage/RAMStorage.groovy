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
 * Simply store everything in memory.
 *
 * @author ypujante@linkedin.com
 */
class RAMStorage implements Storage
{
  private final Map<MountPoint, Object> _storage

  RAMStorage()
  {
    this([:])
  }

  RAMStorage(Map<MountPoint, Object> storage)
  {
    _storage = storage
  }

  Map<MountPoint, Object> getStorage()
  {
    return _storage
  }

  public void clearState(MountPoint mountPoint)
  {
    _storage.remove(mountPoint)
  }

  public getMountPoints()
  {
    return _storage.keySet();
  }

  public loadState(MountPoint mountPoint)
  {
    return _storage[mountPoint];
  }

  public void storeState(MountPoint mountPoint, Object state)
  {
    _storage[mountPoint] = state
  }

  public void clearAllStates()
  {
    _storage.clear()
  }
}
