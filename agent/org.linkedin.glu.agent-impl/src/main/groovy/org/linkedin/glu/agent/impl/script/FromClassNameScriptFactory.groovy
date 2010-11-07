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

  FromClassNameScriptFactory(String className)
  {
    _className = className
  }

  FromClassNameScriptFactory(Class c)
  {
    this(c.name)
  }

  public createScript(ScriptConfig scriptConfig)
  {
    return ReflectUtils.forName(_className).newInstance();
  }

  String toString()
  {
    return "FromClassNameScriptFactory[${_className}]".toString();
  }

  public toExternalRepresentation()
  {
    return ['class': FromClassNameScriptFactory.class.getName(), className: _className];
  }

  boolean equals(o)
  {
    if(this.is(o)) return true;

    if(!(o instanceof FromClassNameScriptFactory)) return false;

    FromClassNameScriptFactory that = (FromClassNameScriptFactory) o;

    if(_className != that._className) return false;

    return true;
  }

  int hashCode()
  {
    return _className.hashCode();
  }
}
