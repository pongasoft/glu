/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2014 Yan Pujante
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
import org.linkedin.glu.utils.tags.TagsSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Represent an individual agent
 *
 * @author ypujante@linkedin.com
 */
class AgentInfo extends NodeInfo
{
  public static final String MODULE = AgentInfo.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static final TagsSerializer TAGS_SERIALIZER = TagsSerializer.INSTANCE

  AgentInfoPropertyAccessor agentInfoPropertyAccessor = PrefixAgentInfoPropertyAccessor.DEFAULT
  String agentName

  Map getAgentProperties()
  {
    return data
  }

  Set<String> getTags()
  {
    return TAGS_SERIALIZER.deserialize(agentInfoPropertyAccessor.getPropertyValue(this, 'agent.tags')) as Set<String> ?: []
  }

  int getPort()
  {
    return (agentInfoPropertyAccessor.getPropertyValue(this, 'agent.port') ?: "-1") as int
  }

  String getHostname()
  {
    return agentInfoPropertyAccessor.getPropertyValue(this, 'agent.hostname') ?: agentName
  }

  String getVersion()
  {
    return agentInfoPropertyAccessor.getPropertyValue(this, 'agent.version')
  }

  def getPropertyValue(String propertyName)
  {
    return agentInfoPropertyAccessor.getPropertyValue(this, propertyName)
  }

  URI getURI()
  {
    URLBuilder url = new URLBuilder()

    if(agentInfoPropertyAccessor.getPropertyValue(this, 'agent.sslEnabled')?.toString() == 'true')
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

  @Override
  protected Map handleInvalidData(Throwable error)
  {
    log.warn("Invalid state detected: agent=${agentName}; ex=${error.getClass().name}: \"${error.message}\"")
    if(log.isDebugEnabled())
      log.debug("Invalid state detected: agent=${agentName}", error)

    return [:]
  }
}
