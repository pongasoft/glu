/*
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

import org.linkedin.groovy.util.io.GroovyIOUtils

/**
 * Encapsulates the notion of agent properties. There are 2 kinds of agent properties:
 *
 * 1. persistent properties are stored locally and reused when the agent restart
 * 2. exposed properties (a subset of persistent properties) are stored in zookeeper and available
 *    as <code>shell.env</code>
 *
 * This class is thread safe.
 *
 * @author yan@pongasoft.com */
public class AgentProperties
{
  private volatile Map<String, String> _persistentProperties
  private volatile Map<String, String> _exposedProperties = null

  AgentProperties()
  {
    _persistentProperties = Collections.unmodifiableMap(new HashMap<String,String>())
  }

  AgentProperties(Map<String, String> persistentProperties)
  {
    _persistentProperties =
      Collections.unmodifiableMap(new HashMap<String,String>(persistentProperties))
  }

  Map<String, String> getPersistentProperties()
  {
    return _persistentProperties
  }

  synchronized Map<String, String> getExposedProperties()
  {
    if(_exposedProperties == null)
    {
      Map<String, String> props = new HashMap<String, String>()
      props.putAll(_persistentProperties.groupBy { k,v ->
        k.toLowerCase().contains('password') ? 'passwordKeys' : 'nonPasswordKeys'
      }.nonPasswordKeys)
      props.remove('line.separator')

      _exposedProperties = Collections.unmodifiableMap(props)
    }

    return _exposedProperties
  }

  synchronized Object setAgentProperty(String name, String value)
  {
    Map<String, String> newMap = new HashMap<String,String>(_persistentProperties)

    Object res = newMap.put(name, value)

    _persistentProperties = Collections.unmodifiableMap(newMap)
    _exposedProperties = null

    return res
  }

  String getExposedProperty(String name)
  {
    return exposedProperties.get(name)
  }

  String getPersistentProperty(String name)
  {
    return persistentProperties.get(name)
  }

  @Override
  Object getAt(String name)
  {
    return getExposedProperty(name)
  }

  @Override
  void putAt(String name, Object value)
  {
    setAgentProperty(name, value?.toString())
  }

  synchronized void load(AgentProperties agentProperties)
  {
    if(!this.is(agentProperties))
    {
      _persistentProperties = agentProperties._persistentProperties
      _exposedProperties = null
    }
  }

  /**
   * Load/Initialize this object from a file
   */
  synchronized void load(File file)
  {
    Properties props  = new Properties()

    if(file?.exists())
    {
      file.withReader { Reader reader ->
        props.load(reader)
      }
    }

    _persistentProperties = Collections.unmodifiableMap(new HashMap<String, String>(props))
    _exposedProperties = null
  }

  /**
   * Save the persistent properties to a file
   */
  synchronized void save(File file)
  {
    if(file)
    {
      GroovyIOUtils.safeOverwrite(file) { File f ->
        f.withWriter { Writer writer ->
          new Properties(_persistentProperties).store(writer, null)
        }
      }
    }
  }
}