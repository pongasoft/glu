/*
 * Copyright (c) 2012 Yan Pujante
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

package org.linkedin.glu.groovy.utils.net

/**
 * @author yan@pongasoft.com */
public class ReinitializableSingletonURLStreamHandlerFactory implements URLStreamHandlerFactory
{
  static ReinitializableSingletonURLStreamHandlerFactory INSTANCE = new ReinitializableSingletonURLStreamHandlerFactory()

  protected final def _handlers = [:]

  public URLStreamHandler createURLStreamHandler(String protocol)
  {
    def handler

    synchronized(_handlers)
    {
       handler = _handlers[protocol]
    }

    if(!handler)
      return null

    if(handler instanceof Closure)
      return handler(protocol)
    else
      handler.createURLStreamHandler(protocol)
  }

  /**
   * Register a handler for the protocol
   */
  void registerHandler(String protocol, handler)
  {
    synchronized(_handlers)
    {
      if(_handlers[protocol])
        throw new IllegalStateException("already registered protocol ${protocol}".toString())

      _handlers[protocol] = handler
    }
  }

  void reInit()
  {
    synchronized(_handlers)
    {
      _handlers.clear()
    }
  }
}
