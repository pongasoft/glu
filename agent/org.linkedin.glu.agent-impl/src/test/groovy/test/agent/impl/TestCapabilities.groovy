/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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


package test.agent.impl

import org.linkedin.glu.agent.api.ShellExecException
import org.linkedin.glu.agent.impl.capabilities.ShellImpl
import org.linkedin.glu.agent.impl.storage.AgentProperties
import org.linkedin.groovy.util.io.fs.FileSystem
import org.linkedin.groovy.util.io.fs.FileSystemImpl

/**
 * Test for various capabilities. Most of the test has been moved under the utils module
 * for the generic shell
 *
 * @author yan@pongasoft.com
 */
class TestCapabilities extends GroovyTestCase
{
  public static final String MODULE = TestCapabilities.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  /**
   * Test that we can exec commands.
   */
  void testExec()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      def shell = new ShellImpl(fileSystem: fs)

      // non existent script
      try
      {
        shell.exec("/non/existent/666")
        fail("should fail")
      }
      catch(ShellExecException e)
      {
        assertEquals(127, e.res)
        assertEquals('', e.stringOutput)
        String shellScriptError = 'bash: /non/existent/666: No such file or directory'
        assertEquals(shellScriptError, e.stringError)
        assertEquals('Error while executing command /non/existent/666: res=127 - output= - error='
                     + shellScriptError,
                     e.message)
      }
    }
  }

  /**
   * Test for passing environment variables */
  void testEnv()
  {
    FileSystemImpl.createTempFileSystem() { FileSystem fs ->
      AgentProperties agentProperties =
       new AgentProperties([p1: 'v1', p2: 'v2', storePassword: 'abcd'])
      def shell = new ShellImpl(fileSystem: fs, agentProperties: agentProperties)

      assertEquals('v1', shell.env.p1)
      assertEquals('v2', shell.env['p2'])
      assertNull(shell.env.p3)
      assertNull(shell.env.storePassword) // should be filtered out

      shell = shell.newShell(fs.newFileSystem('/foo'))

      assertEquals('v1', shell.env.p1)
      assertEquals('v2', shell.env['p2'])
      assertNull(shell.env.p3)
      assertNull(shell.env.storePassword) // should be filtered out
    }
  }
}
