/*
 * Copyright 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.agent.rest.client;

import org.json.JSONObject
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.codec.OneWayCodec
import org.linkedin.util.lifecycle.CannotConfigureException
import org.linkedin.util.lifecycle.Configurable
import org.restlet.data.Reference
import org.restlet.data.Status
import org.restlet.ext.json.JsonRepresentation
import org.restlet.Uniform
import org.restlet.resource.ClientResource

/**
 * @author ypujante@linkedin.com
 */
class ConfigurableRestClient implements Configurable
{
  Uniform client
  OneWayCodec codec
  Reference reference

  @Override
  public void configure(Map config)
  {
    def map = new TreeMap(config)
    map.checksum = computeChecksum(map)

    JSONObject json = new JSONObject()
    json.put('args', JsonUtils.toJSON(map))

    def res
    def clientResource = new ClientResource(reference)
    clientResource.next = client
    try
    {
      res = clientResource.put(new JsonRepresentation(json))
    }
    catch(Throwable th)
    {
      throw new CannotConfigureException(th)
    }

    if(clientResource.status != Status.SUCCESS_OK)
      throw new CannotConfigureException(clientResource.status.toString())
  }

  private String computeChecksum(Map map)
  {
    // first we sort the map by keys
    map = new TreeMap(map)

    StringBuilder sb = new StringBuilder()

    map.each { k, v ->
      sb.append(k).append('\n')
      sb.append(v).append('\n')
    }

    return codec.encode(sb.toString().getBytes('UTF-8'))
  }

}
