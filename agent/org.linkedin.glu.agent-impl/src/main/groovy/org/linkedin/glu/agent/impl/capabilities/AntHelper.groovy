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


package org.linkedin.glu.agent.impl.capabilities

import org.apache.tools.ant.Project
import org.apache.tools.ant.BuildException

/**
 * Helper methods for ant
 *
 * @author ypujante@linkedin.com
 */
class AntHelper
{
  /**
   * Executes the closure with a builder and make sure to catch <code>BuildException</code>
   * to propertly unwrap them
   */
  static def withBuilder(Closure closure)
  {
    AntBuilder builder = new AntBuilder()
    // removes info messages...
    builder.project.buildListeners[0].messageOutputLevel = Project.MSG_WARN
    try
    {
      return closure(builder)
    }
    catch (BuildException e)
    {
      if(e.cause)
        throw e.cause
      else
        throw e
    }
  }
}
