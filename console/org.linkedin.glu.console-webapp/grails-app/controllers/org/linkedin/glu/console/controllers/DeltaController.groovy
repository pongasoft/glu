/*
 * Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.console.controllers

import org.linkedin.glu.orchestration.engine.delta.DeltaService
import org.linkedin.groovy.util.json.JsonUtils

/**
 * @author ypujante@linkedin.com
 */
class DeltaController extends ControllerBase
{
  DeltaService deltaService

  def beforeInterceptor = {
    // we make sure that the fabric is always set before executing any action
    return ensureCurrentFabric()
  }

  /**
   * Delta from
   */
  def rest_get_delta = {
    def delta = deltaService.computeDelta(request.system)

    if(params.errorsOnly?.toString() == 'true')
    {
      delta.delta = delta.delta.findAll { entry ->
        entry.state != 'OK'
      }
    }

    delta = JsonUtils.toJSON(delta)

    if(params.prettyPrint?.toString() == 'true')
      delta = delta.toString(2)
    else
      delta = delta.toString()

    render delta
  }
}
