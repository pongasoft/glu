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

package org.linkedin.glu.console.provisioner.services.storage

import org.linkedin.glu.orchestration.engine.system.SystemStorage
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.console.domain.DbCurrentSystem
import org.linkedin.glu.console.domain.DbSystemModel
import org.springframework.transaction.TransactionStatus

/**
 * @author yan@pongasoft.com */
public class SystemStorageImpl implements SystemStorage
{
  @Override
  SystemModel findCurrentByFabric(String fabric)
  {
    DbCurrentSystem.findByFabric(fabric, [cache: false])?.systemModel?.systemModel
  }

  @Override
  SystemModel findCurrentByFabric(Fabric fabric)
  {
    findCurrentByFabric(fabric.name)
  }

  @Override
  SystemModel findBySystemId(String systemId)
  {
    DbSystemModel.findBySystemId(systemId)?.systemModel
  }

  void saveCurrentSystem(SystemModel systemModel)
  {
    DbSystemModel.withTransaction { TransactionStatus txStatus ->
      DbSystemModel dbSystem = DbSystemModel.findBySystemId(systemModel.id)

      if(!dbSystem)
      {
        dbSystem = new DbSystemModel(systemModel: systemModel)
        if(!dbSystem.save())
        {
          txStatus.setRollbackOnly()
          throw new SystemStorageException(dbSystem.errors)
        }
      }

      DbCurrentSystem dbc = DbCurrentSystem.findByFabric(systemModel.fabric, [cache: false])

      if(dbc)
        dbc.systemModel = dbSystem
      else
        dbc = new DbCurrentSystem(systemModel: dbSystem,
                                  fabric: systemModel.fabric)

      if(!dbc.save())
      {
        txStatus.setRollbackOnly()
        throw new SystemStorageException(dbc.errors)
      }
    }
  }
}