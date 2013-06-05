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

import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils
import org.pongasoft.glu.provisioner.core.metamodel.HostMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ServerMetaModel

/**
 * @author yan@pongasoft.com  */
public class ServerMetaModelImpl implements ServerMetaModel
{
  HostMetaModel host
  Map<String, Integer> ports

  @Override
  int getMainPort()
  {
    getPort('main')
  }

  @Override
  int getPort(String portName)
  {
    if(ports.containsKey(portName))
      ports[portName]
    else
      throw new IllegalArgumentException("not a valid portName: [${portName}]")
  }

  @Override
  Object toExternalRepresentation()
  {
    def res = [
      host: host.toExternalRepresentation(),
    ]

    if(ports.containsKey('main'))
      res.port = mainPort

    if(ports.size() > 1)
      res.ports =  GluGroovyCollectionUtils.xorMap(ports, ['main'])

    return res
  }
}