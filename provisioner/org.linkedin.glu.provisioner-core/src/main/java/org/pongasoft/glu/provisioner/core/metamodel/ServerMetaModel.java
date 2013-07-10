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

package org.pongasoft.glu.provisioner.core.metamodel;

/**
 * Represents a server which is a cli that starts a (usually long running) process that binds to
 * one (or more) port(s).
 *
 * @author yan@pongasoft.com
 */
public interface ServerMetaModel extends CliMetaModel
{
  /**
   * If undefined, return the {@link #getDefaultPort()}.
   *
   * @return the "main"/primary port of this server, which is how the server is usually contacted on
   */
  int getMainPort();

  /**
   * The default port for this server (ex: 12906 for agent)
   * @return -1 if no default port
   */
  int getDefaultPort();

  /**
   * @param portName the name of the port to return. <code>mainPort</code> is equivalent to
   *                 {@link #getMainPort()}
   * @return the port associated to the name (in case the server uses additional ports)
   */
  int getPort(String portName);
}
