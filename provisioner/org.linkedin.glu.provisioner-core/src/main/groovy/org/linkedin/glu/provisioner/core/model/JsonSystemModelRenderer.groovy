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

package org.linkedin.glu.provisioner.core.model

import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.codec.CodecUtils
import org.linkedin.util.codec.HexaCodec
import org.linkedin.util.codec.OneWayCodec
import org.linkedin.util.codec.OneWayMessageDigestCodec

/**
 * Customizes pretty printing.
 *
 * @author ypujante@linkedin.com */
class JsonSystemModelRenderer implements SystemModelRenderer
{
  public static final OneWayCodec SHA1 =
    OneWayMessageDigestCodec.createSHA1Instance('', HexaCodec.INSTANCE)

  private static final JACKSON_CANONICAL_MAPPER = JsonUtils.newJacksonMapper(true)

  int prettyPrint = 2

  boolean maintainBackwardCompatibilityInSystemId = false

  @Override
  String computeSystemId(SystemModel model)
  {
    if(model == null)
      return null

    def ext = model.toCanonicalRepresentation()
    ext.remove('id') // we remove id from the computation
    def json
    if(maintainBackwardCompatibilityInSystemId)
      json = JsonUtils.toJSON(ext).toString(prettyPrint)
    else
      json = JACKSON_CANONICAL_MAPPER.writeValueAsString(ext)
    return CodecUtils.encodeString(SHA1, json)
  }

  @Override
  String prettyPrint(SystemModel model)
  {
    return JsonUtils.prettyPrint(model.toCanonicalRepresentation(), prettyPrint)
  }

  @Override
  String compactPrint(SystemModel model)
  {
    return JsonUtils.compactPrint(model.toCanonicalRepresentation())
  }

  @Override
  String canonicalPrint(SystemModel model)
  {
    if(model == null)
      return null
    return JACKSON_CANONICAL_MAPPER.writeValueAsString(model.toCanonicalRepresentation())
  }
}
