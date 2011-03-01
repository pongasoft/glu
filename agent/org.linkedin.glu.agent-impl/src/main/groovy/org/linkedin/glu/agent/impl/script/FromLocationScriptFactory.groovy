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


package org.linkedin.glu.agent.impl.script

/**
 * The script factory from a location: will 'fetch' the script, execute it and returns
 * the class found inside.
 *
 * @author ypujante@linkedin.com
 */
def class FromLocationScriptFactory implements ScriptFactory, Serializable
{
  private static final long serialVersionUID = 1L;

  private final def _location
  private def _scriptFile
  private transient def _script

  FromLocationScriptFactory(location)
  {
    _location = location
  }

  public createScript(ScriptConfig scriptConfig)
  {
    if(!_script)
    {
      if(!_scriptFile?.exists())
        _scriptFile = scriptConfig.shell.fetch(_location)

      Class scriptClass = new GroovyClassLoader(getClass().classLoader).parseClass(_scriptFile.file)
      _script = scriptClass.newInstance()
    }
    
    return _script
  }

  String toString()
  {
    return "FromLocationScriptFactory[${_location}]".toString();
  }

  public toExternalRepresentation()
  {
    return ['class': FromLocationScriptFactory.class.getName(), location: _location];
  }

  boolean equals(o)
  {
    if(this.is(o)) return true;

    if(!(o instanceof FromLocationScriptFactory)) return false;

    FromLocationScriptFactory that = (FromLocationScriptFactory) o;

    if(_location != that._location) return false;

    return true;
  }

  int hashCode()
  {
    return _location.hashCode();
  }
}
