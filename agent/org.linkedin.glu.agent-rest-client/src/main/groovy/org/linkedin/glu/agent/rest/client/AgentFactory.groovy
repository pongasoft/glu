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


package org.linkedin.glu.agent.rest.client

/**
 * Defines the agent factory
 *
 * @author ypujante@linkedin.com
 */
interface AgentFactory
{
  /**
   * Calls the closure with an agent object pointing to the URI provided.
   */
  def withRemoteAgent(URI agentURI, Closure closure)

  /**
   * Calls the closure with an agent object pointing to the host provided.
   * It uses the fabric settings to construct the proper URI
   */
  def withRemoteAgent(String agentHost, Closure closure)

}
