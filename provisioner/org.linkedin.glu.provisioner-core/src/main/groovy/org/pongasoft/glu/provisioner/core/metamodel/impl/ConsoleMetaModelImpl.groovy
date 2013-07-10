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

package org.pongasoft.glu.provisioner.core.metamodel.impl

import org.pongasoft.glu.provisioner.core.metamodel.ConsoleMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ConsolePluginMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.FabricMetaModel

/**
 * @author yan@pongasoft.com  */
public class ConsoleMetaModelImpl extends ServerMetaModelImpl implements ConsoleMetaModel
{
  String name
  Map<String, FabricMetaModel> fabrics
  String externalHost
  Collection<ConsolePluginMetaModel> plugins
  URI dataSourceDriverUri
  String internalPath
  String externalPath

  @Override
  int getDefaultPort()
  {
    return DEFAULT_PORT;
  }

  @Override
  int getExternalPort()
  {
    getPort('externalPort', mainPort)
  }

  @Override
  String getExternalHost()
  {
    externalHost ?: host.resolveHostAddress()
  }

  @Override
  String getInternalPath()
  {
    internalPath ?: DEFAULT_PATH
  }

  @Override
  String getExternalPath()
  {
    externalPath ?: DEFAULT_PATH
  }

  @Override
  FabricMetaModel findFabric(String fabricName)
  {
    return fabrics[fabricName]
  }

  @Override
  Object toExternalRepresentation()
  {
    def ext = super.toExternalRepresentation()

    ext.name = name

    if(externalHost)
      ext.externalHost = externalHost

    if(internalPath)
      ext.internalPath = internalPath

    if(externalPath)
      ext.externalPath = externalPath

    if(plugins)
      ext.plugins = plugins.collect { it.toExternalRepresentation() }

    if(dataSourceDriverUri)
      ext.dataSourceDriverUri = dataSourceDriverUri.toString()

    return ext
  }
}