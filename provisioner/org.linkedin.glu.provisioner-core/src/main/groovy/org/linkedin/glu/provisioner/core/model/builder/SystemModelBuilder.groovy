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

package org.linkedin.glu.provisioner.core.model.builder

import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.core.model.SystemModel

/**
 * @author yan@pongasoft.com  */
public class SystemModelBuilder extends ModelBuilder<SystemModel>
{
  private SystemModel _systemModel

  @Override
  Map doParseJsonGroovy(String jsonModel)
  {
    SystemModelJsonGroovyDsl.parseJsonGroovy(jsonModel)
  }

  @Override
  SystemModelBuilder deserializeFromJsonMap(Map jsonModel)
  {
    if(jsonModel == null)
      _systemModel = null
    else
    {
      _systemModel = new SystemModel(id: jsonModel.id,
                                     fabric: jsonModel.fabric,
                                     metadata: jsonModel.metadata ?: [:])

      jsonModel.agentTags?.each { agent, tags ->
        _systemModel.addAgentTags(agent, tags)
      }

      jsonModel.entries?.each {
        _systemModel.addEntry(SystemEntry.fromExternalRepresentation(it))
      }

      if(jsonModel.name)
        _systemModel.metadata.name = jsonModel.name
    }

    return this
  }

  @Override
  SystemModel toModel()
  {
    return _systemModel
  }
}