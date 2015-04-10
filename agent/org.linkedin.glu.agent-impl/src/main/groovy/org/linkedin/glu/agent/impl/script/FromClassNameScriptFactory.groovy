/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2013 Yan Pujante
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

import org.linkedin.util.reflect.ReflectUtils

/**
 * The script factory from a class name (simply use reflection to instantiate the class)
 *
 * @author ypujante@linkedin.com
 */
def class FromClassNameScriptFactory implements ScriptFactory, Serializable
{
  private static final long serialVersionUID = 1L;

  private final String _className
  private final def _classPath
  private def _jarFiles
  private transient LoadedScript _loadedScript

  FromClassNameScriptFactory(String className)
  {
    this(className, null)
  }

  FromClassNameScriptFactory(String className, def classPath)
  {
    _className = className
    _classPath = classPath
  }

  private FromClassNameScriptFactory(String className, def classPath, def localClassPath)
  {
    _className = className
    _classPath = classPath
    _jarFiles = localClassPath
  }

  FromClassNameScriptFactory(Class c)
  {
    this(c.name, null)
  }

  public createScript(ScriptConfig scriptConfig)
  {
    if(!_loadedScript)
    {
      if(!_jarFiles?.collect { it.exists() }?.inject(true) { res, i -> res && i })
      {
        // fetch all the jar files
        _jarFiles = _classPath?.collect { scriptConfig.shell.fetch(it) }
      }

      _loadedScript = scriptConfig.scriptLoader.loadScript(_className,
                                                           _jarFiles?.collect { it.file } )
    }

    return _loadedScript.script
  }

  @Override
  void destroyScript(ScriptConfig scriptConfig)
  {
    scriptConfig.scriptLoader.unloadScript(_loadedScript)

    _jarFiles?.each { scriptConfig.shell.rm(it) }
    _loadedScript = null
  }

  String toString()
  {
    if(_classPath)
      return "FromClassNameScriptFactory[${_className}, ${_classPath}]".toString();
    else
      return "FromClassNameScriptFactory[${_className}]".toString();
  }

  public toExternalRepresentation()
  {
    def res = ['class': FromClassNameScriptFactory.class.getName(), className: _className]
    if(_classPath)
      res.classPath = _classPath
    if(_jarFiles)
      res.localClassPath = _jarFiles
    return res;
  }

  public static ScriptFactory fromExternalRepresentation(def args)
  {
    if(args['class'] == FromClassNameScriptFactory.class.getName())
      return new FromClassNameScriptFactory(args.className,
                                            args.classPath,
                                            args.localClassPath)
    else
      return null
  }

  boolean equals(o)
  {
    if(this.is(o)) return true;

    if(!(o instanceof FromClassNameScriptFactory)) return false;

    FromClassNameScriptFactory that = (FromClassNameScriptFactory) o;

    if(_className != that._className) return false;
    if(_classPath != that._classPath) return false;

    return true;
  }


  int hashCode()
  {
    int result;
    result = (_className != null ? _className.hashCode() : 0);
    result = 31 * result + (_classPath != null ? _classPath.hashCode() : 0);
    return result;
  }
}
