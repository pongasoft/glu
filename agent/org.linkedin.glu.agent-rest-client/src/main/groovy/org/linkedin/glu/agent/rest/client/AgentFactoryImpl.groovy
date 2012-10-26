/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

import org.linkedin.groovy.util.config.Config
import org.restlet.Client
import org.restlet.data.Reference
import org.linkedin.util.annotations.Initializer
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils

/**
 * Implementation which will create {@link AgentRestClient}.
 *
 * @author ypujante@linkedin.com
 */
class AgentFactoryImpl implements AgentFactory
{
  public static final def DEFAULT_MAPPINGS =
    ['agent', 'commands', 'command', 'mountPoint', 'host', 'process', 'log', 'file', 'tags']

  public static final def DEFAULT_PATHS = GroovyCollectionsUtils.toMapKey(DEFAULT_MAPPINGS) {
    "/${it}".toString()
  }

  Map<String, String> paths = DEFAULT_PATHS
  RestClientFactory restClientFactory

  @Initializer
  void setPaths(config)
  {
    DEFAULT_MAPPINGS.each { name ->
      paths[name] =
        Config.getOptionalString(config, "${name}Path".toString(), "/${name}".toString())
    }
  }

  def withRemoteAgent(URI agentURI, Closure closure)
  {
    restClientFactory.withRestClient(agentURI) { Client client ->
      def protocol = client.protocols[0]
      def baseRef = new Reference(protocol, agentURI.host, agentURI.port)
      Map<String, Reference> references = [:]

      paths.each { name, path ->
        references[name] = new Reference(baseRef, path)
      }

      return closure(new AgentRestClient(client, references))
    }
  }

  static AgentFactory create(config)
  {
    new AgentFactoryImpl(restClientFactory: RestClientFactoryImpl.create(config),
                         paths: config)
  }
}
