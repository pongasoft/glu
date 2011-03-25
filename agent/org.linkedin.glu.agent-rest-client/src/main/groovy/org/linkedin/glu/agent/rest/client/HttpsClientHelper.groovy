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

import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.scheme.SchemeRegistry
import org.apache.http.conn.ssl.AllowAllHostnameVerifier
import org.apache.http.conn.ssl.SSLSocketFactory
import org.linkedin.groovy.util.config.Config
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.util.codec.Base64Codec
import org.linkedin.util.codec.Codec
import org.linkedin.util.codec.CodecUtils
import org.linkedin.util.codec.OneWayCodec
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.restlet.Client
import org.restlet.data.Form
import org.restlet.data.Protocol
import org.restlet.engine.security.DefaultSslContextFactory
import org.restlet.ext.httpclient.HttpClientHelper
import org.restlet.util.Series

/**
 * Client helper for https connections: create the proper ssl socket factories
 *
 * @author ypujante@linkedin.com
 */
class HttpsClientHelper extends HttpClientHelper
{
  private static final Codec TWO_WAY_CODEC
 
  static {
    String p0 = "gluos2way"
    TWO_WAY_CODEC = new Base64Codec(p0)
  }

  private final def _protocol
  private final URI _serverURI
  private final SSLSocketFactory _sslSocketFactory

  HttpsClientHelper(Client client)
  {
    super(client);
    getProtocols().remove(Protocol.HTTP)
    if(client)
    {
      _serverURI = client.context.attributes['serverURI']
      _sslSocketFactory = new SSLSocketFactory(client.context.attributes['sslContext'])
      _sslSocketFactory.hostnameVerifier = new AllowAllHostnameVerifier()
    }
    else
    {
      _serverURI = null
      _sslSocketFactory = null
    }
  }

  public void start()
  {
    super.start()
  }

  /**
   * Registers only for https on the given port
   */
  protected void configure(SchemeRegistry schemeRegistry)
  {
    schemeRegistry.register(new Scheme("https", _sslSocketFactory, _serverURI.port));
  }


  public static def initSSLContext(def config)
  {
    Series params = new Form()

    // keystore
    getPrivateKeyStoreParams(config).each {k, v ->
        params.add(k, v)
    }

    // truststore
    getTrustStoreParams(config).each {k, v ->
        params.add(k, v)
    }

    def sslContextFactory = new DefaultSslContextFactory()
    sslContextFactory.init(params)

    sslContextFactory.createSslContext()
  }

  private static Map getPrivateKeyStoreParams(def config)
  {
    return getKeyStoreParams(config, 'keystorePath')
  }

  public static Map getKeyStoreParams(def config, String keyStorePathProp)
  {
    Map params = [:]

    // keystore
    params.put(keyStorePathProp,
               GroovyIOUtils.toFile(Config.getRequiredString(config, keyStorePathProp)).path)
    params.put('keystorePassword', getPassword(config, 'keystorePassword'))
    params.put('keyPassword', getPassword(config, 'keyPassword'))

    return params
  }

  private static Map getTrustStoreParams(def config)
  {
    Map params = [:]

    // truststore
    params.put('truststorePath',
               GroovyIOUtils.toFile(Config.getRequiredString(config, 'truststorePath')).path)
    params.put('truststorePassword', getPassword(config, 'truststorePassword'))

    return params
  }

  private static String getPassword(def config, String name)
  {
    def password = Config.getRequiredString(config, name)

    if(Config.getOptionalBoolean(config, "${name}Encrypted", true))
    {
      def codec = config.codec ?: TWO_WAY_CODEC
      password = CodecUtils.decodeString(codec, password)
    }

    return password
  }
}
