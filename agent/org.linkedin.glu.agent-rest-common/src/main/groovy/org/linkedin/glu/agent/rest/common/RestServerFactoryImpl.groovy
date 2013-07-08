/*
 * Copyright (c) 2013 Yan Pujante
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

package org.linkedin.glu.agent.rest.common

import org.restlet.Server
import org.restlet.data.Protocol

/**
 * @author yan@pongasoft.com */
public class RestServerFactoryImpl implements RestServerFactory
{
  public static final String NON_SECURE_RESTLET_HELPER_CLASS =
    'org.restlet.ext.jetty.HttpServerHelper'
  public static final String SECURE_RESTLET_HELPER_CLASS =
    'org.restlet.ext.jetty.HttpsServerHelper'

  @Override
  Server createRestServer(boolean secure, String address, int port)
  {
    if(secure)
    {
      new Server(null,
                 [Protocol.HTTPS],
                 address,
                 port,
                 null,
                 SECURE_RESTLET_HELPER_CLASS)
    }
    else
    {
      new Server(null,
                 [Protocol.HTTP],
                 address,
                 port,
                 null,
                 NON_SECURE_RESTLET_HELPER_CLASS)
    }
  }
}