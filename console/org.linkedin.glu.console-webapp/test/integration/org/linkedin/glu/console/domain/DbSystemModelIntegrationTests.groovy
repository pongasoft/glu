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

package org.linkedin.glu.console.domain

import org.linkedin.glu.console.provisioner.services.storage.SystemStorageImpl
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.system.SystemModelDetails
import org.linkedin.glu.orchestration.engine.system.SystemService

/**
 * @author yan@pongasoft.com */
public class DbSystemModelIntegrationTests extends GroovyTestCase
{
  SystemStorageImpl systemStorage

  /**
   * Injected */
  SystemService systemService

  @Override
  protected void setUp()
  {
    super.setUp()
    systemStorage = systemService.systemService.systemStorage
  }

  /**
   * Test for find systems api
   */
  public void testFindSystems()
  {
    // adding system 1 in fabric f1
    systemService.saveCurrentSystem(m('f1',
                                      [agent: 'a2', mountPoint: '/m1', script: 's1']))
    SystemModelDetails details1 = systemStorage.findCurrentDetailsByFabric('f1')
    assertEquals('f1', details1.fabric)

    // adding system 2 in fabric f2
    Thread.sleep(1) // make sure that the current time changes
    systemService.saveCurrentSystem(m('f2',
                                      [agent: 'a23', mountPoint: '/m1', script: 's1']))
    SystemModelDetails details2 = systemStorage.findCurrentDetailsByFabric('f2')
    assertEquals('f2', details2.fabric)

    // adding system 3 in fabric f1
    Thread.sleep(1) // make sure that the current time changes
    systemService.saveCurrentSystem(m('f1',
                                      [agent: 'a234', mountPoint: '/m1', script: 's1']))
    SystemModelDetails details3 = systemStorage.findCurrentDetailsByFabric('f1')
    assertEquals('f1', details3.fabric)

    // with details
    Map res = systemService.findSystems('f1', true, [:])
    assertEquals(2, res.count)
    assertEquals(2, res.systems.size())
    assertEquals(details3.systemId, res.systems[0].systemId)
    assertEquals(details3.fabric, 'f1')
    assertEquals(details3.dateCreated, res.systems[0].dateCreated)
    assertEquals('a234', res.systems[0].systemModel.findEntries()[0].agent)
    assertEquals(details3.size, res.systems[0].size)

    assertEquals(details1.systemId, res.systems[1].systemId)
    assertEquals(details1.fabric, 'f1')
    assertEquals(details1.dateCreated, res.systems[1].dateCreated)
    assertEquals('a2', res.systems[1].systemModel.findEntries()[0].agent)
    assertEquals(details1.size, res.systems[1].size)

    // without details
    res = systemService.findSystems('f1', false, [:])
    assertEquals(2, res.count)
    assertEquals(2, res.systems.size())
    assertEquals(details3.systemId, res.systems[0].systemId)
    assertEquals(details3.fabric, 'f1')
    assertEquals(details3.dateCreated.time, res.systems[0].dateCreated.time)
    assertNull(res.systems[0].systemModel)
    assertEquals(details3.size, res.systems[0].size)

    assertEquals(details1.systemId, res.systems[1].systemId)
    assertEquals(details1.fabric, 'f1')
    assertEquals(details1.dateCreated.time, res.systems[1].dateCreated.time)
    assertNull(res.systems[1].systemModel)
    assertEquals(details1.size, res.systems[1].size)

    // testing parameters
    res = systemService.findSystems('f1', false, [max: 1, offset: 1])
    assertEquals(2, res.count)
    assertEquals(1, res.systems.size())
    assertEquals(details1.systemId, res.systems[0].systemId)
    assertEquals(details1.fabric, 'f1')
    assertEquals(details1.dateCreated.time, res.systems[0].dateCreated.time)
    assertNull(res.systems[0].systemModel)
    assertEquals(details1.size, res.systems[0].size)
  }

  /**
   * Testing the fact that a nullable size column is added
   */
  public void testAddSizeColumn()
  {
    // adding system 1 in fabric f1
    systemService.saveCurrentSystem(m('f1',
                                      [agent: 'a2', mountPoint: '/m1', script: 's1']))
    SystemModelDetails details = systemStorage.findCurrentDetailsByFabric('f1')

    // setting the size column to null to simulate old columns
    DbSystemModel model = DbSystemModel.findBySystemId(details.systemId)
    int size = model.size
    assertNotNull(model.size)
    model.size = null
    if(!model.save())
    {
      fail("cannot save model")
    }

    // this queries the full model hence the size can be recomputed
    details = systemStorage.findCurrentDetailsByFabric('f1')
    assertEquals(size, details.size)

    // this one does not recompute the size (it is not available)
    details = systemService.findSystems('f1', false, [:]).systems[0]
    assertNull(details.size)
  }

  private SystemModel m(String fabric, Map... entries)
  {
    SystemModel model = new SystemModel(fabric: fabric)


    entries.each {
      model.addEntry(SystemEntry.fromExternalRepresentation(it))
    }

    return model
  }

}