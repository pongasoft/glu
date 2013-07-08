/*
 * Copyright (c) 2011-2013 Yan Pujante
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
import org.linkedin.groovy.util.config.Config
import org.linkedin.util.annotations.Initializable
import org.linkedin.util.clock.Timespan
import org.restlet.Client
import org.restlet.Context
import org.restlet.data.Protocol

/**
 * @author yan@pongasoft.com */
class RestClientFactoryImpl implements RestClientFactory
{
  private final SSLContext _sslContext

  @Initializable
  Timespan connectionTimeout = Timespan.parse('30s')

  RestClientFactoryImpl(SSLContext sslContext)
  {
    _sslContext = sslContext
  }

  @Override
  Client createRestClient(URI uri)
  {
    def client
    def protocol

    switch(uri.scheme)
    {
      case 'http':
        protocol = Protocol.HTTP
        client = new Client(null,
                            [protocol] as List,
                            'org.restlet.ext.httpclient.HttpClientHelper') // forcing httpclient
        client.connectTimeout = connectionTimeout.durationInMilliseconds
        break;

      case 'https':
        protocol = Protocol.HTTPS
        Context context = new Context()
        context.attributes['serverURI'] = uri
        context.attributes['sslContext'] = _sslContext
        client = new Client(context,
                            [protocol] as List,
                            HttpsClientHelper.class.name)
        client.connectTimeout = connectionTimeout.durationInMilliseconds
        break;

      default:
        throw new IllegalArgumentException("unsupported scheme ${uri.scheme}")
    }

    return client
  }

  @Override
  def withRestClient(URI uri, Closure closure)
  {
    Client client = createRestClient(uri)

    try
    {
      client.start()

      return closure(client)
    }
    finally
    {
      client.stop()
    }
  }

  static RestClientFactory create(config)
  {
    SSLContext sslContext = null
    
    if(Config.getOptionalBoolean(config, 'sslEnabled', true))
    {
      sslContext = HttpsClientHelper.initSSLContext(config)
    }

    return new RestClientFactoryImpl(sslContext)
  }
}
