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


package org.linkedin.glu.agent.impl.script

import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.utils.core.Externable

/**
 * Represents the script definition (does not change over the life of the script)
 *
 * @author ypujante@linkedin.com
 */
class ScriptDefinition implements Serializable, Externable
{
  private static final long serialVersionUID = 1L;

  final MountPoint mountPoint
  final MountPoint parent
  final ScriptFactory scriptFactory
  final def initParameters
  final def _scriptFactoryArgs

  ScriptDefinition(MountPoint mountPoint,
                   MountPoint parent,
                   ScriptFactory scriptFactory,
                   initParameters)
  {
    // parent is required unless mountpoint is root...
    assert parent != null || mountPoint == MountPoint.ROOT

    this.mountPoint = mountPoint
    this.parent = parent
    this.scriptFactory = null
    this.initParameters = initParameters ?: [:]
    this._scriptFactoryArgs = scriptFactory.toExternalRepresentation()
  }

  def getScriptFactoryArgs()
  {
    if(_scriptFactoryArgs == null)
      return scriptFactory.toExternalRepresentation()
    else
      _scriptFactoryArgs
  }

  ScriptFactory getScriptFactory(ScriptFactoryFactory scriptFactoryFactory)
  {
    // for backward compatibility
    if(_scriptFactoryArgs == null)
      return scriptFactory
    else
      return scriptFactoryFactory.createScriptFactory(_scriptFactoryArgs)
  }

  public String toString()
  {
    return "[mountPoint: ${mountPoint}, parent: ${parent}, scriptFactory: ${scriptFactory}, scriptFactoryArgs: ${_scriptFactoryArgs}, initParameters: ${initParameters}]";
  }

  def toExternalRepresentation()
  {
    return [mountPoint: mountPoint,
            parent: parent,
            scriptFactory: scriptFactory?.toExternalRepresentation() ?: _scriptFactoryArgs,
            initParameters: initParameters]
  }

  boolean equals(o)
  {
    if(this.is(o)) return true;

    if(!o || getClass() != o.class) return false;

    ScriptDefinition that = (ScriptDefinition) o;

    if(that.mountPoint != this.mountPoint) return false;
    if(that.parent != this.parent) return false;
    if(that.scriptFactory != this.scriptFactory) return false;
    if(that._scriptFactoryArgs != this._scriptFactoryArgs) return false;
    if(that.initParameters != this.initParameters) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = (mountPoint ? mountPoint.hashCode() : 0);
    result = 31 * result + (parent ? parent.hashCode() : 0);
    result = 31 * result + (scriptFactory ? scriptFactory.hashCode() : 0);
    result = 31 * result + (scriptFactoryArgs ? scriptFactoryArgs.hashCode() : 0);
    result = 31 * result + (initParameters ? initParameters.hashCode() : 0);
    return result;
  }
}
