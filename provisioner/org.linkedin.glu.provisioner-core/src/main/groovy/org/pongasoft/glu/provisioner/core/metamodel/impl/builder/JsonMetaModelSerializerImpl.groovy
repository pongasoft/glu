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

package org.pongasoft.glu.provisioner.core.metamodel.impl.builder

import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel

/**
 * @author yan@pongasoft.com  */
public class JsonMetaModelSerializerImpl
{
  String gluVersion

  GluMetaModel deserialize(Collection<Resource> resources)
  {
    def builder = new GluMetaModelBuilder(gluVersion: gluVersion)
    resources.each { builder.deserializeFromJsonResource(it)}
    builder.toModel()
  }

  GluMetaModel deserialize(Map<String, Resource> resources)
  {
    def builder = new GluMetaModelBuilder(gluVersion: gluVersion)
    resources.each { fabricName, resource ->
      builder.deserializeFromJsonResource(resource)
    }
    builder.toModel()
  }

  String serialize(GluMetaModel gluMetaModel, boolean prettyPrint = false)
  {
    if(prettyPrint)
      JsonUtils.prettyPrint(gluMetaModel.toExternalRepresentation())
    else
      JsonUtils.compactPrint(gluMetaModel.toExternalRepresentation())
  }
}