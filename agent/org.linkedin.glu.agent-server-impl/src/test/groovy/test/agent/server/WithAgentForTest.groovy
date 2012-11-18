/*
 * Copyright (c) 2012 Yan Pujante
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

package test.agent.server

import org.linkedin.glu.groovy.utils.GluGroovyLangUtils

/**
 * @author yan@pongasoft.com */
public abstract class WithAgentForTest extends GroovyTestCase
{
  AgentForTest agent

  def shutdownSequence = []

  @Override
  protected void setUp()
  {
    super.setUp()
    shutdownSequence << { super.tearDown() }

    agent = new AgentForTest()
    agent.start()

    shutdownSequence << { agent?.destroy() }
  }

  @Override
  protected void tearDown()
  {
    GluGroovyLangUtils.onlyOneException(shutdownSequence.reverse())
  }
}