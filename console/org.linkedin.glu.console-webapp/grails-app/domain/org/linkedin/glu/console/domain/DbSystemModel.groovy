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

package org.linkedin.glu.console.domain

import org.linkedin.glu.provisioner.core.model.JSONSystemModelSerializer
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.fabric.Fabric

class DbSystemModel
{
  public static final SERIALIZER = new JSONSystemModelSerializer()

  static constraints = {
    content(nullable: false)
    fabric(nullable: false, blank: false, validator: {val, obj ->
      return val == obj.systemModel.fabric
    })
    systemId(nullable: false, blank: false, unique: true, validator: {val, obj ->
      return val == obj.systemModel.id
    })
    name(nullable: true, blank: true)
    size(nullable: true)
  }

  static mapping = {
    columns {
      content type: 'text'
    }
  }

  Date dateCreated

  String fabric
  String systemId
  private String _content
  Integer size
  String name

  String getContent()
  {
    return _content
  }
  
  void setContent(String content)
  {
    _content = content
    _systemModel = SERIALIZER.deserialize(content)
    fabric = _systemModel.fabric
    systemId = _systemModel.id
    name = _systemModel.name
  }

  String contentSerializer = SERIALIZER.type

  private SystemModel _systemModel

  SystemModel getSystemModel()
  {
    return _systemModel
  }

  Map getMetadata()
  {
    return _systemModel.metadata
  }

  void setSystemModel(SystemModel systemModel)
  {
    _systemModel = systemModel
    _content = SERIALIZER.serialize(systemModel)
    fabric = systemModel.fabric
    systemId = systemModel.id
    size = _content.size()
    name = systemModel.name
  }

  static transients = ['metadata', 'systemModel', '_systemModel', '_content']


  /**
   * Shortcut to get the current one 
   */
  static DbSystemModel findCurrent(String fabric)
  {
    return DbCurrentSystem.findByFabric(fabric, [cache: false])?.systemModel
  }

  /**
   * Shortcut to get the current one
   */
  static DbSystemModel findCurrent(Fabric fabric)
  {
    return findCurrent(fabric.name)
  }
}
