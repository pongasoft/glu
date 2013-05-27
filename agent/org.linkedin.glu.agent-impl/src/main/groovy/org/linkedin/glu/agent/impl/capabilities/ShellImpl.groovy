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


package org.linkedin.glu.agent.impl.capabilities

import org.linkedin.glu.agent.api.ScriptFailedException
import org.linkedin.glu.agent.api.Shell
import org.linkedin.glu.agent.impl.storage.AgentProperties

/**
 * contains the utility methods for the shell
 *
 * @author ypujante@linkedin.com
 */
def class ShellImpl extends org.linkedin.glu.groovy.utils.shell.ShellImpl implements Shell
{
  public static final String MODULE = ShellImpl.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  // agent properties
  AgentProperties agentProperties

  @Override
  ShellImpl newShell(fileSystem)
  {
    return new ShellImpl(fileSystem: fileSystem,
                         agentProperties: agentProperties,
                         charset: charset,
                         clock: clock,
                         submitter: _submitter)
  }

  Map<String, String> getEnv()
  {
    return agentProperties?.exposedProperties ?: [:]
  }

  /**
   * @{inheritDoc}
   */
  def exec(Map args)
  {
    try
    {
      super.exec(args)
    }
    catch(org.linkedin.glu.groovy.utils.shell.ShellExecException e)
    {
      // need to rethrow the exception to match the api!
      def ne = new org.linkedin.glu.agent.api.ShellExecException(e.message)
      ne.res = e.res
      ne.output = e.output
      ne.error = e.error
      ne.initCause(e.cause)
      e.suppressed.each { ne.addSuppressed(it) }
      throw ne
    }
  }

  /**
   * Calling this method will force a script failure
   */
  void fail(message)
  {
    throw new ScriptFailedException(message?.toString())
  }
}
