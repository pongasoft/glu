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


package org.linkedin.glu.agent.tracker

import org.linkedin.util.url.URLBuilder

/**
 * Represent an individual agent
 *
 * @author ypujante@linkedin.com
 */
class AgentInfo extends NodeInfo
{
  String agentName

  Map getAgentProperties()
  {
    return data
  }

  int getPort()
  {
    return (agentProperties['glu.agent.port'] ?: "-1") as int
  }

  String getHostname()
  {
    return agentProperties['glu.agent.hostname'] ?: agentName
  }

  URI getURI()
  {
    URLBuilder url = new URLBuilder()

    if(agentProperties['glu.agent.sslEnabled']?.toString() == 'true')
      url.scheme = 'https'
    else
      url.scheme = 'http'
    url.host = hostname
    url.port = port 

    return url.toJavaURL().toURI()
  }

  public String toString()
  {
    return "AgentInfo: ${[agentName: agentName, agentProperties: data]}".toString()
  }
}
