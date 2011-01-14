/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

import org.restlet.data.Protocol
import org.restlet.Client
import org.restlet.data.Reference
import org.linkedin.util.codec.OneWayCodec
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.linkedin.util.codec.Base64Codec
import org.linkedin.util.clock.Timespan

/**
 * @author ypujante@linkedin.com */
class ConfigurableFactoryImpl implements ConfigurableFactory
{
  int port = 12907
  OneWayCodec codec = defaultCodec()
  Timespan connectionTimeout = Timespan.parse('1s')

  def withRemoteConfigurable(String host, Closure closure)
  {
    withRemoteConfigurable(new URI("http://${host}:${port}/config"), closure)
  }

  def withRemoteConfigurable(URI uri, Closure closure)
  {
    def client = new Client(Protocol.HTTP)
    client.connectTimeout = connectionTimeout.durationInMilliseconds
    client.start()
    try
    {
      ConfigurableRestClient c = new ConfigurableRestClient(client: client,
                                                            codec: codec,
                                                            reference: new Reference(uri.toString()))
      closure(c)
    }
    finally
    {
      client.stop()
    }
  }

  private static OneWayCodec defaultCodec()
  {
    OneWayMessageDigestCodec.createSHA1Instance("gluos", Base64Codec.INSTANCE)
  }
}
