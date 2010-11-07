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

package org.linkedin.glu.provisioner.core.model

import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.groovy.util.json.JsonUtils

/**
 * @author ypujante@linkedin.com */
class SystemEntry
{
  String agent
  String mountPoint
  def script
  def initParameters = [:]
  def metadata = [:]

  String getKey()
  {
    return "${agent}:${mountPoint}".toString()
  }

  def toExternalRepresentation()
  {
    return [
        agent: agent,
        mountPoint: mountPoint,
        script: script,
        initParameters: initParameters,
        metadata: metadata
    ]
  }

  /**
   * @return a flattened version of the entry (a map with only one level)
   */
  Map flatten()
  {
    flatten([:])
  }

  /**
   * @param destMap the map to store the result
   * @return destMap
   */
  Map flatten(Map destMap)
  {
    GroovyCollectionsUtils.flatten(toExternalRepresentation(), destMap)
    destMap.key = key
    return destMap
  }

  static SystemEntry fromExternalRepresentation(def er)
  {
    new SystemEntry(er)
  }

  boolean equals(o)
  {
    if(this.is(o)) return true;

    if(getClass() != o.class) return false;

    SystemEntry that = (SystemEntry) o;

    if(agent != that.agent) return false;
    if(initParameters != that.initParameters) return false;
    if(metadata != that.metadata) return false;
    if(mountPoint != that.mountPoint) return false;
    if(script != that.script) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = (agent != null ? agent.hashCode() : 0);
    result = 31 * result + (mountPoint != null ? mountPoint.hashCode() : 0);
    result = 31 * result + (script != null ? script.hashCode() : 0);
    result = 31 * result + (initParameters != null ? initParameters.hashCode() : 0);
    result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
    return result;
  }

  def String toString()
  {
    return JsonUtils.toJSON(toExternalRepresentation()).toString(2)
  }

}
