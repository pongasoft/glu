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

import org.linkedin.util.codec.Base64Codec
import org.linkedin.util.codec.OneWayCodec
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.restlet.Client
import org.restlet.data.Reference

/**
 * @author ypujante@linkedin.com */
class ConfigurableFactoryImpl implements ConfigurableFactory
{
  int port = 12907
  OneWayCodec codec = defaultCodec()
  RestClientFactory restClientFactory

  def withRemoteConfigurable(String host, Closure closure)
  {
    withRemoteConfigurable(new URI("http://${host}:${port}/config"), closure)
  }

  def withRemoteConfigurable(URI uri, Closure closure)
  {
    restClientFactory.withRestClient(uri) { Client client ->
      ConfigurableRestClient c = new ConfigurableRestClient(client: client,
                                                            codec: codec,
                                                            reference: new Reference(uri.toString()))
      closure(c)
    }
  }

  private static OneWayCodec defaultCodec()
  {
    OneWayMessageDigestCodec.createSHA1Instance("gluos1way1", new Base64Codec("gluos1way2"))
  }
}
