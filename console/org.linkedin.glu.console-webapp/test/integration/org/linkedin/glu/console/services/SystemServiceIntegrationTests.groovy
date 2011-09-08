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

package org.linkedin.glu.console.services

import org.linkedin.glu.console.domain.DbSystemModel
import org.linkedin.glu.console.domain.DbCurrentSystem
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemEntry

import org.linkedin.glu.orchestration.engine.system.SystemService
import org.linkedin.glu.provisioner.core.model.JSONSystemModelSerializer

/**
 * @author ypujante@linkedin.com */
class SystemServiceIntegrationTests extends GroovyTestCase
{
  JSONSystemModelSerializer serializer = new JSONSystemModelSerializer(prettyPrint: 2)
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
    assertEquals(serializer.serialize(model1), serializer.serialize(currentModel.systemModel))
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

  public void testSetAsCurrentSystem()
  {
    SystemModel model1 = new SystemModel(fabric: 'f1')
    model1.addEntry(new SystemEntry(agent: 'h1', script: 's1', mountPoint: '/m1'))

    SystemModel model2 = new SystemModel(fabric: 'f1')
    model2.addEntry(new SystemEntry(agent: 'h2', script: 's2', mountPoint: '/m2'))

    SystemModel model3 = new SystemModel(fabric: 'f2')
    model2.addEntry(new SystemEntry(agent: 'h3', script: 's3', mountPoint: '/m3'))

    assertTrue(systemService.saveCurrentSystem(model1))
    model1 = systemService.findCurrentSystem('f1')

    // setting the current system to be model1: already model1 => return false
    assertFalse(systemService.setAsCurrentSystem('f1', model1.id))

    // changing current system to model2
    assertTrue(systemService.saveCurrentSystem(model2))
    model2 = systemService.findCurrentSystem('f1')

    // should be the new one
    assertNotSame(model1.id, model2.id)

    // now resetting current system to model1 => return true
    assertTrue(systemService.setAsCurrentSystem('f1', model1.id))
    assertEquals(model1.id, systemService.findCurrentSystem('f1').id)

    // saving model3 as current system (in fabric f2!)
    assertTrue(systemService.saveCurrentSystem(model3))
    model3 = systemService.findCurrentSystem('f2')

    // should not affect f1!
    assertEquals(model1.id, systemService.findCurrentSystem('f1').id)

    // trying to set as current a system from a different fabric should fail
    shouldFail(IllegalArgumentException) {
      systemService.setAsCurrentSystem('f1', model3.id)
    }

    // trying to set an unknown system should fail
    shouldFail(IllegalArgumentException) {
      systemService.setAsCurrentSystem('f1', 'unknown system')
    }
  }
}
