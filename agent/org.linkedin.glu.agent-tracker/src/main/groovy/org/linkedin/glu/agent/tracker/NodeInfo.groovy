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


package org.linkedin.glu.agent.tracker

import org.apache.zookeeper.data.Stat
import org.linkedin.zookeeper.tracker.TrackedNode
import org.linkedin.groovy.util.json.JsonUtils

/**
 * Base class for nodes
 *
 * @author ypujante@linkedin.com
 */
class NodeInfo
{
  TrackedNode trackedNode

  private volatile Map _data = null

  Stat getStat()
  {
    return trackedNode.stat
  }

  String getPath()
  {
    return trackedNode.path
  }

  long getCreationTime()
  {
    return trackedNode.creationTime
  }

  long getModifiedTime()
  {
    return trackedNode.modifiedTime
  }


  /**
   * Returns the data associated to this node as map (the data is supposed to be encoded in JSON
   * format).
   */
  Map getData()
  {
    if(_data == null)
    {
      _data = JsonUtils.fromJSON(trackedNode.data)
    }

    return _data
  }
}
