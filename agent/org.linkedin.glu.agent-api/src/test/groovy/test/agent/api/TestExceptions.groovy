/*
 * Copyright (c) 2013 Yan Pujante
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


package test.agent.api

import org.linkedin.glu.agent.api.DuplicateMountPointException
import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.api.NoSuchActionException
import org.linkedin.glu.agent.api.NoSuchCommandException
import org.linkedin.glu.agent.api.NoSuchMountPointException
import org.linkedin.glu.agent.api.ScriptException
import org.linkedin.glu.agent.api.ScriptExecutionCauseException
import org.linkedin.glu.agent.api.ScriptExecutionException
import org.linkedin.glu.agent.api.ScriptFailedException
import org.linkedin.glu.agent.api.ScriptIllegalStateException
import org.linkedin.glu.agent.api.ShellExecException
import org.linkedin.glu.agent.api.TimeOutException

/**
 * @author yan@pongasoft.com
 */
def class TestExceptions extends GroovyTestCase
{
  /**
   * Test for issue: glu-235: in agent => java.lang.IllegalStateException: Can't overwrite cause
   */
  void testWithNullArgument()
  {
    def e1 = new Exception('e1')

    new TimeOutException(null).initCause(e1)
    new DuplicateMountPointException((String) null).initCause(e1)
    new DuplicateMountPointException((MountPoint) null).initCause(e1)
    new NoSuchMountPointException((String) null).initCause(e1)
    new NoSuchMountPointException((MountPoint) null).initCause(e1)
    new NoSuchCommandException((String) null).initCause(e1)
    new ScriptException((String) null).initCause(e1)
    new ScriptExecutionException((String) null).initCause(e1)
    new ScriptIllegalStateException((String) null).initCause(e1)
    new ShellExecException((String) null).initCause(e1)
    new ScriptFailedException((String) null).initCause(e1)
    new ScriptExecutionCauseException((String) null).initCause(e1)
    new NoSuchActionException((String) null).initCause(e1)
  }
}
