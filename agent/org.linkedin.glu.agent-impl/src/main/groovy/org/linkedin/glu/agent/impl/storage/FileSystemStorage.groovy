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
import org.linkedin.glu.agent.api.NoSuchMountPointException
import org.linkedin.groovy.util.io.fs.FileSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.groovy.util.lang.GroovyLangUtils
import org.linkedin.util.io.resource.Resource

/**
 * Store in the filesystem
 *
 * @author ypujante@linkedin.com
 */
class FileSystemStorage implements Storage
{
  public static final String MODULE = FileSystemStorage.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private final FileSystem _fileSystem
  private final AgentProperties _agentProperties
  private final File _agentPropertiesFile

  FileSystemStorage(FileSystem fileSystem,
                    AgentProperties agentProperties,
                    File agentPropertiesFile)
  {
    _fileSystem = fileSystem
    _agentProperties = agentProperties
    _agentPropertiesFile = agentPropertiesFile
  }

  public synchronized void clearState(MountPoint mountPoint)
  {
    def state = _fileSystem.toResource(mountPoint.toPathWithNoSlash())
    _fileSystem.rm(state)
  }

  public synchronized void clearAllStates()
  {
    _fileSystem.ls() { resource ->
      _fileSystem.rm(resource)
    }
  }

  public synchronized getMountPoints()
  {
    def potentialMountPoints = _fileSystem.ls().collect { resource ->
      MountPoint.fromPathWithNoSlash(resource.filename)
    }

    potentialMountPoints.findAll { MountPoint mp ->
      GroovyLangUtils.noException(mp, null) { loadState(mp) }
    }
  }

  public synchronized Collection<Resource> deleteInvalidStates()
  {
    _fileSystem.ls().findAll { Resource resource ->
      GroovyLangUtils.noExceptionWithValueOnException(false) {
        
        def state = GroovyLangUtils.noException(resource, null) {
          loadState(MountPoint.fromPathWithNoSlash(resource.filename))
        }

        if(state == null)
        {
          Resource moved = _fileSystem.mv(resource, _fileSystem.createTempDir())
          log.warn("Detected invalid state... moved to ${moved}")
          return true
        }

        return false
      }
    }
  }

  public synchronized loadState(MountPoint mountPoint)
  {
    def state = GroovyLangUtils.noException(mountPoint, null) {
      try
      {
        _fileSystem.deserializeFromFile(mountPoint.toPathWithNoSlash())
      }
      catch (FileNotFoundException e)
      {
        return null
      }
    }

    if(extractMountPointFromState(state) != mountPoint)
    {
      if(log.isDebugEnabled())
        log.debug("mountPoint mismatch [ignored]: ${extractMountPointFromState(state)} != ${mountPoint}")
      throw new NoSuchMountPointException(mountPoint?.path)
    }

    return state
  }

  public synchronized void storeState(MountPoint mountPoint, state)
  {
    if(extractMountPointFromState(state) != mountPoint)
      throw new IllegalArgumentException("mismatch mountPoint: ${mountPoint} != ${extractMountPointFromState(state)}")

    _fileSystem.serializeToFile(mountPoint.toPathWithNoSlash(), state)
  }

  private MountPoint extractMountPointFromState(state)
  {
    state?.scriptDefinition?.mountPoint
  }

  FileSystem getFileSystem()
  {
    return _fileSystem
  }

  @Override
  synchronized AgentProperties loadAgentProperties()
  {
    _agentProperties.load(_agentPropertiesFile)

    if(log.isDebugEnabled())
      log.debug "Loading agent properties from ${_agentPropertiesFile}: ${new TreeMap(_agentProperties.persistentProperties)}"

    return _agentProperties
  }

  @Override
  synchronized AgentProperties saveAgentProperties(AgentProperties agentProperties)
  {
    _agentProperties.load(agentProperties)
    _agentProperties.save(_agentPropertiesFile)

    if(log.isDebugEnabled())
      log.debug"Saving agent properties to ${_agentPropertiesFile}: ${new TreeMap(_agentProperties.persistentProperties)}"

    return _agentProperties
  }

  @Override
  synchronized AgentProperties updateAgentProperty(String name, String value)
  {
    AgentProperties agentProperties = loadAgentProperties()
    agentProperties.setAgentProperty(name, value)
    saveAgentProperties(agentProperties)
  }
}
