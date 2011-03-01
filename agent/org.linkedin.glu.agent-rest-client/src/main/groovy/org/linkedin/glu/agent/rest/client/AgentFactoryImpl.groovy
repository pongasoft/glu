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

import javax.net.ssl.SSLContext
import org.restlet.Client
import org.restlet.data.Protocol
import org.restlet.data.Reference
import org.linkedin.groovy.util.config.Config
import org.linkedin.util.clock.Timespan
import org.linkedin.util.annotations.Initializable
import org.restlet.Context

/**
 * Implementation which will create {@link AgentRestClient}.
 *
 * @author ypujante@linkedin.com
 */
class AgentFactoryImpl implements AgentFactory
{
  private final Map<String, String> _paths
  private final SSLContext _sslContext
  private final Integer _agentPort

  @Initializable
  Timespan connectionTimeout = Timespan.parse('1s')

  AgentFactoryImpl(Map<String, String> paths,
                   Integer agentPort,
                   SSLContext sslContext)
  {
    _agentPort = agentPort
    _paths = paths
    _sslContext = sslContext
  }

  def withRemoteAgent(String host, Closure closure)
  {
    URI uri
    if (_sslContext)
    {
      uri = new URI("https://${host}:${_agentPort}")
    }
    else
    {
      uri = new URI("http://${host}:${_agentPort}")
    }
    withRemoteAgent(uri, closure)
  }

  def withRemoteAgent(URI agentURI, Closure closure)
  {
    def client
    def protocol

    switch(agentURI.scheme)
    {
      case 'http':
        protocol = Protocol.HTTP
        client = new Client(null, [protocol] as List, 'org.restlet.ext.httpclient.HttpClientHelper')
        client.connectTimeout = connectionTimeout.durationInMilliseconds
        break;

      case 'https':
        protocol = Protocol.HTTPS
        Context context = new Context()
        context.attributes['serverURI'] = agentURI
        context.attributes['sslContext'] = _sslContext
        client = new Client(context, [protocol] as List, HttpsClientHelper.class.name)
        client.connectTimeout = connectionTimeout.durationInMilliseconds
        break;

      default:
        throw new IllegalArgumentException("unsupported scheme ${agentURI.scheme}")
    }

    def baseRef = new Reference(protocol, agentURI.host, agentURI.port)

    Map<String, Reference> references = [:]

    _paths.each { name, path ->
      references[name] = new Reference(baseRef, path)
    }

    def agent = new AgentRestClient(client, references)

    try
    {
      client.start()

      return closure(agent)
    }
    finally
    {
      client.stop()
    }
  }

  static AgentFactory create(config)
  {
    SSLContext sslContext
    
    if(Config.getOptionalBoolean(config, 'sslEnabled', true))
    {
      sslContext = HttpsClientHelper.initSSLContext(config)
    }

    def references = [:]

    ['agent', 'mountPoint', 'host', 'process', 'log', 'file', 'tags'].each { name ->
      references[name] =
        Config.getOptionalString(config, "${name}Path".toString(), "/${name}".toString())
    }

    return new AgentFactoryImpl(references,
                                Config.getOptionalInt(config, 'agentPort', 12906),
                                sslContext)
  }
}
