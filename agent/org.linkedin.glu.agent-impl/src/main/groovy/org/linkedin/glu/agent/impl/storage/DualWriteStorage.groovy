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


package org.linkedin.glu.agent.impl.storage

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.groovy.util.lang.GroovyLangUtils

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
    GluGroovyLangUtils.onlyOneException(
      { super.clearState(mountPoint) },
      { writeOnlyStorage.clearState(mountPoint) }
    )
  }

  synchronized void storeState(MountPoint mountPoint, state)
  {
    GluGroovyLangUtils.onlyOneException(
      { super.storeState(mountPoint, state) },
      { writeOnlyStorage.storeState(mountPoint, state) }
    )
  }

  synchronized public void clearAllStates()
  {
    GluGroovyLangUtils.onlyOneException(
      { super.clearAllStates() },
      { writeOnlyStorage.clearAllStates() }
    )
  }

  @Override
  synchronized AgentProperties saveAgentProperties(AgentProperties agentProperties)
  {
    GluGroovyLangUtils.onlyOneException(
      { agentProperties = super.saveAgentProperties(agentProperties) },
      { writeOnlyStorage.saveAgentProperties(agentProperties) }
    )
    return agentProperties
  }

  @Override
  AgentProperties updateAgentProperty(String name, String value)
  {
    AgentProperties agentProperties

    GluGroovyLangUtils.onlyOneException(
      { agentProperties = super.updateAgentProperty(name, value) },
      { writeOnlyStorage.saveAgentProperties(agentProperties) }
    )

    return agentProperties
  }

  @Override
  synchronized def invalidateState(MountPoint mountPoint)
  {
    def invalidStateNewLocation = null

    GluGroovyLangUtils.onlyOneException(
      { invalidStateNewLocation = super.invalidateState(mountPoint) },
      { writeOnlyStorage.invalidateState(mountPoint) }
    )

    return invalidStateNewLocation
  }

  /**
   * Synchronizes the 2 storages (the source of truth is supposed to be the readWriteStorage).
   */
  synchronized public void sync()
  {
    writeOnlyStorage.clearAllStates()

    mountPoints.each { mountPoint ->
      def state = GroovyLangUtils.noException(mountPoint, null) { loadState(mountPoint) }
      if(state != null)
        writeOnlyStorage.storeState(mountPoint, state)
    }

    writeOnlyStorage.saveAgentProperties(loadAgentProperties())
  }
}
