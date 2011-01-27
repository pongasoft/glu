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


package org.linkedin.glu.agent.impl.storage

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.api.NoSuchMountPointException
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.groovy.util.io.fs.FileSystem
import org.slf4j.Logger
import org.slf4j.LoggerFactory

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
    def state = _fileSystem.toResource(toPath(mountPoint))
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
    return _fileSystem.ls().collect { resource ->
      fromPath(resource.filename)
    }
  }

  public synchronized loadState(MountPoint mountPoint)
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

  public synchronized void storeState(MountPoint mountPoint, state)
  {
    _fileSystem.serializeToFile(toPath(mountPoint), state)
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

  private String toPath(MountPoint mp)
  {
    return mp.path.replace('/', '_')
  }

  private MountPoint fromPath(String path)
  {
    return MountPoint.create(path.replace('_', '/'))
  }
}
