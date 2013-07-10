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

import org.linkedin.glu.utils.core.Externable;

import java.net.URI;
import java.util.Collection;
import java.util.Map;

/**
 * Represents the console (UI) used in glu. Note that at this stage, although a console can host
 * many fabrics there are a couple of restrictions/constraints to be aware of:
 *
 * <ol>
 *   <li>
 *     Because a fabric is tied to a ZooKeeper cluster, it is recommended that the console
 *     be physically hosted on the same network/data center where the cluster is hosted. As a result
 *     it is not recommended to host fabrics that are using different ZooKeeper clusters spanning
 *     multiple networks/data centers.
 *   </li>
 *   <li>
 *     Due to some implementation details, at this stage the console can have only one set of keys.
 *     Technically speaking, the keys are tied to a fabric, but because of this restriction, it means
 *     that all the fabrics hosted by a given console must use the same set of keys. As a result,
 *     you will need more than one console if you have fabrics with different keys.
 *   </li>
 * </ol>
 *
 * @author yan@pongasoft.com
 */
public interface ConsoleMetaModel extends ServerMetaModel, Externable
{
  public static final int DEFAULT_PORT = 8080;
  public static final String DEFAULT_PATH = "/console";

  /**
   * @return the name as defined in the model
   */
  String getName();

  /**
   * @return the set of fabrics hosted by this console
   */
  Map<String, FabricMetaModel> getFabrics();
  FabricMetaModel findFabric(String fabricName);

  /**
   * The console is a web application server (jetty) and in some scenario is accessed
   * via a web server (nginx, apache, ...). This would be the port of the web server (if different
   * from the web application server). The port of the web application server is returned by
   * {@link #getMainPort()}
   *
   * @return by default, returns {@link #getMainPort()} unless redefined
   */
  int getExternalPort();

  /**
   * The console is a web application server (jetty) and in some scenario is accessed
   * via a web server (nginx, apache, ...). This would be the host of the web server (if different
   * from the web application server). The host of the web application server is returned by
   * {@link #getHost()}
   *
   * @return by default, returns {@link #getHost()}, unless redefined
   */
  String getExternalHost();

  /**
   * The console is a web application server (jetty) and in some scenario is accessed
   * via a web server (nginx, apache, ...). This would be the path to use for the web server
   * itself.
   *
   * @return by default returns {@link #DEFAULT_PATH}
   */
  String getExternalPath();

  /**
   * The console is a web application server (jetty) and in some scenario is accessed
   * via a web server (nginx, apache, ...). This would be the path to use for the web application
   * server itself.
   *
   * @return by default returns {@link #DEFAULT_PATH}
   */
  String getInternalPath();

  /**
   * @return the plugins/extensions to the console
   */
  Collection<ConsolePluginMetaModel> getPlugins();

  /**
   * @return the data source driver for the console
   *         (ex: http://jcenter.bintray.com/mysql/mysql-connector-java/5.1.25/mysql-connector-java-5.1.25.jar
   *         for MySql)
   */
  URI getDataSourceDriverUri();
}