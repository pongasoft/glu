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

import org.linkedin.glu.provisioner.services.fabric.FabricStorage
import org.linkedin.glu.provisioner.services.fabric.Fabric
import org.linkedin.util.clock.Timespan

/**
 * @author yan@pongasoft.com */
public class FabricStorageImpl implements FabricStorage
{
  @Override
  Collection<Fabric> loadFabrics()
  {
    org.linkedin.glu.console.domain.Fabric.list().collect {
      new Fabric(name: it.name,
                 zkConnectString: it.zkConnectString,
                 zkSessionTimeout: Timespan.parse(it.zkSessionTimeout),
                 color: it.color)
    }
  }
}