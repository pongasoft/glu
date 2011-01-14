/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.console.services

import org.linkedin.glu.console.domain.DbSystemModel
import org.linkedin.glu.console.domain.DbCurrentSystem
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemEntry

/**
 * @author ypujante@linkedin.com */
class SystemServiceIntegrationTests extends GroovyTestCase
{
  SystemService systemService

  public void testCurrentSystem()
  {
    assertNull(DbSystemModel.findCurrent('f1'))

    SystemModel model1 = new SystemModel(fabric: 'f1')
    model1.addEntry(new SystemEntry(agent: 'h1', script: 's1', mountPoint: '/m1'))

    systemService.saveCurrentSystem(model1)

    def currentModel = DbSystemModel.findCurrent('f1')
    def currentId = currentModel.id
    assertEquals('f1', currentModel.fabric)
    assertEquals('json', currentModel.contentSerializer)
    assertEquals(model1, currentModel.systemModel)

    // we resave the exact same model... should not do anything
    def model2 = model1.clone()
    model2.id = null
    systemService.saveCurrentSystem(model2)

    currentModel = DbSystemModel.findCurrent('f1')
    assertEquals(currentId, currentModel.id)
    assertEquals('f1', currentModel.fabric)
    assertEquals('json', currentModel.contentSerializer)
    assertEquals(model2, currentModel.systemModel)

    def model3 = model2.clone()
    model3.addEntry(new SystemEntry(agent: 'h2', script: 's2', mountPoint: '/m2'))

    // we save a different model
    assertEquals("same id ${model3.id} but different content!", shouldFail(IllegalArgumentException) {
      systemService.saveCurrentSystem(model3) })

    model3.id = null
    systemService.saveCurrentSystem(model3)

    currentModel = DbSystemModel.findCurrent('f1')
    assertTrue("${currentId} < ${currentModel.id}", currentId < currentModel.id)
    assertEquals('f1', currentModel.fabric)
    assertEquals('json', currentModel.contentSerializer)
    assertEquals(model3, currentModel.systemModel)

    assertEquals(1, DbCurrentSystem.findAllByFabric('f1').size())

    // now we resave model2 as current model
    systemService.saveCurrentSystem(model2)
    currentModel = DbSystemModel.findCurrent('f1')
    assertEquals(currentId, currentModel.id)
    assertEquals('f1', currentModel.fabric)
    assertEquals('json', currentModel.contentSerializer)
    assertEquals(model2, currentModel.systemModel)
  }
}
