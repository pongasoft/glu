/*
 * Copyright (c) 2011-2013 Yan Pujante
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

package org.linkedin.glu.orchestration.engine.system

import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.orchestration.engine.audit.AuditLogService

/**
 * @author yan@pongasoft.com */
public class AuditedSystemService implements SystemService
{
  @Delegate
  @Initializable(required = true)
  SystemService systemService

  @Initializable
  AuditLogService auditLogService

  @Override
  boolean saveCurrentSystem(SystemModel newSystemModel)
  {
    boolean saved = systemService.saveCurrentSystem(newSystemModel)

    if(saved)
    {
      auditLogService.audit('system.change',
                            "fabric: ${newSystemModel.fabric}, systemId: ${newSystemModel.id}")
    }

    return saved
  }

  @Override
  boolean deleteCurrentSystem(String fabric)
  {
    boolean res = systemService.deleteCurrentSystem(fabric)

    if(res)
    {
      auditLogService.audit('system.current.delete', "fabric: ${fabric}")
    }

    return res
  }

}