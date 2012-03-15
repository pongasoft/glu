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

package org.linkedin.glu.provisioner.core.model

import org.linkedin.groovy.util.json.JsonUtils

/**
 * Uses json for the format.
 *
 * @author ypujante@linkedin.com */
class JSONSystemModelSerializer implements SystemModelSerializer
{
  public static final JSONSystemModelSerializer INSTANCE = new JSONSystemModelSerializer()

  def prettyPrint

  SystemModel deserialize(String model)
  {
    SystemModel.fromExternalRepresentation(JsonUtils.fromJSON(model))
  }

  String serialize(SystemModel model)
  {
    if (prettyPrint)
      return JsonUtils.prettyPrint(model.toCanonicalRepresentation())
    else
      return JsonUtils.compactPrint(model.toCanonicalRepresentation())
  }

  String getType()
  {
    return 'json';
  }
}
