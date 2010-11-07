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

import javax.net.SocketFactory
import javax.net.ssl.SSLContext
import org.apache.http.params.HttpConnectionParams
import org.apache.http.conn.ConnectTimeoutException

/**
 * Copied from example on httpclient web site.
 *
 * @author ypujante@linkedin.com
 */
class HttpClientSecureProtocolSocketFactory  // implements SecureProtocolSocketFactory
{
  private final _sslContext

  HttpClientSecureProtocolSocketFactory(SSLContext sslContext)
  {
    _sslContext = sslContext
  }

  SSLContext getSSLContext()
  {
    return _sslContext
  }

  public Socket createSocket(String host,
                             int port,
                             InetAddress clientHost,
                             int clientPort)
   throws IOException, UnknownHostException
  {

    return getSSLContext().getSocketFactory().createSocket(host,
                                                           port,
                                                           clientHost,
                                                           clientPort);
  }

  public Socket createSocket(final String host,
                             final int port,
                             final InetAddress localAddress,
                             final int localPort,
                             final HttpConnectionParams params)
    throws IOException, UnknownHostException, ConnectTimeoutException
  {
    if(params == null)
    {
      throw new IllegalArgumentException("Parameters may not be null");
    }

    int timeout = params.getConnectionTimeout();
    SocketFactory socketfactory = getSSLContext().getSocketFactory();
    if(timeout == 0)
    {
      return socketfactory.createSocket(host, port, localAddress, localPort);
    }
    else
    {
      Socket socket = socketfactory.createSocket();
      SocketAddress localaddr = new InetSocketAddress(localAddress, localPort);
      SocketAddress remoteaddr = new InetSocketAddress(host, port);
      socket.bind(localaddr);
      socket.connect(remoteaddr, timeout);
      return socket;
    }
  }

  public Socket createSocket(String host, int port)
    throws IOException, UnknownHostException
  {
    return getSSLContext().getSocketFactory().createSocket(host, port);
  }

  public Socket createSocket(Socket socket,
                             String host,
                             int port,
                             boolean autoClose)
    throws IOException, UnknownHostException
  {
    return getSSLContext().getSocketFactory().createSocket(socket,
                                                           host,
                                                           port,
                                                           autoClose);
  }
}
