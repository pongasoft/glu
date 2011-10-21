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

import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinitionStorageImpl
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinitionSerializer
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinition
import org.linkedin.glu.orchestration.engine.delta.UserCustomDeltaDefinition
import org.springframework.dao.DataIntegrityViolationException
import org.linkedin.glu.orchestration.engine.delta.LightUserCustomDeltaDefinition

/**
 * @author yan@pongasoft.com */
public class CustomDeltaDefinitionStorageTests extends GroovyTestCase
{
  CustomDeltaDefinitionStorageImpl customDeltaDefinitionStorage

  CustomDeltaDefinitionSerializer customDeltaDefinitionSerializer

  def cdd = [
    name: 'd1',
    description: 'desc1',
    errorsOnly: false,
    summary: true,
    columnsDefinition: [
      [
        name: 'ag',
        source: 'agent',
        orderBy: 'asc'
      ],
      [
        name: 'mp',
        source: 'mountPoint'
      ],
      [
        name: 'vs',
        source: 'metadata.version'
      ],
      [
        name: 'oldest',
        source: 'metadata.modifiedTime',
        groupBy: 'min'
      ],
      [
        name: 'newest',
        source: 'metadata.modifiedTime',
        groupBy: 'max'
      ],
    ]
  ]

//  public void testSave()
//  {
//    assertEquals(0, customDeltaDefinitionStorage.findAllByUsername('user1', true, [:]).list.size())
//    assertEquals(0, customDeltaDefinitionStorage.findAllByUsername('user1', false, [:]).list.size())
//    assertEquals(0, customDeltaDefinitionStorage.findAllShareable(false, [:]).list.size())
//    assertEquals(0, customDeltaDefinitionStorage.findAllShareable(true, [:]).list.size())
//
//    assertNull(customDeltaDefinitionStorage.findByUsernameAndName('user1', 'd1'))
//
//    UserCustomDeltaDefinition ud11 =
//      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
//                                    username: 'user1')
//
//    // it should work because there is nothing in the database
//    assertTrue(customDeltaDefinitionStorage.save(ud11))
//
//    UserCustomDeltaDefinition ud11Read =
//      customDeltaDefinitionStorage.findByUsernameAndName('user1', 'd1')
//
//    checkCustomDeltaDefinition(ud11.customDeltaDefinition, ud11Read.customDeltaDefinition)
//    assertEquals(ud11.id, ud11Read.id)
//    assertEquals('d1', ud11Read.name)
//    assertEquals('desc1', ud11Read.description)
//
//    UserCustomDeltaDefinition ud12 =
//      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
//                                    username: 'user2')
//
//    // it should work because it is a different user using the same name
//    assertTrue(customDeltaDefinitionStorage.save(ud12))
//
//    // creating an entry with no username
//    UserCustomDeltaDefinition ud1N =
//      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
//                                    username: null)
//
//    assertTrue(customDeltaDefinitionStorage.save(ud1N))
//
//    // adding another one for user 1
//    cdd.name = 'd2'
//    UserCustomDeltaDefinition ud21 =
//      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
//                                    username: 'user1',
//                                    shareable: false)
//
//    // it should work because there is nothing in the database
//    assertTrue(customDeltaDefinitionStorage.save(ud21))
//
//    // testing find all shareable with details
//    def map = customDeltaDefinitionStorage.findAllShareable(true, [:])
//    assertEquals(3, map.count)
//    assertEquals(3, map.list.size())
//    assertTrue(map.list.id.containsAll([ud11.id, ud12.id, ud1N.id]))
//    map.list.each { assertTrue(it instanceof UserCustomDeltaDefinition) }
//
//    // testing find all shareable without details
//    map = customDeltaDefinitionStorage.findAllShareable(false, [:])
//    assertEquals(3, map.count)
//    assertEquals(3, map.list.size())
//    assertTrue(map.list.id.containsAll([ud11.id, ud12.id, ud1N.id]))
//    map.list.each { assertTrue(it instanceof LightUserCustomDeltaDefinition) }
//
//  }
//
//  /**
//   * The problem is that as soon as an exception is thrown, the current session is dead this is
//   * why this goes in a different test method
//   */
//  void testDuplicateEntry()
//  {
//    UserCustomDeltaDefinition ud11 =
//      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
//                                    username: 'user1')
//
//    // it should work because there is nothing in the database
//    assertTrue(customDeltaDefinitionStorage.save(ud11))
//
//    ud11 =
//      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
//                                    username: 'user1')
//
//    // it should fail because there is already a row in the database
//    shouldFail(DataIntegrityViolationException) {
//      customDeltaDefinitionStorage.save(ud11)
//    }
//  }

  void testShareableShouldBeTrueWhenNoUser()
  {
    // creating an entry with no username
    UserCustomDeltaDefinition ud1N =
      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
                                    username: null,
                                    shareable: false)

    assertFalse(customDeltaDefinitionStorage.save(ud1N))

    println ud1N.errors
  }

  private void checkCustomDeltaDefinition(CustomDeltaDefinition expected,
                                          CustomDeltaDefinition actual)
  {
    assertEquals(customDeltaDefinitionSerializer.serialize(expected, true),
                 customDeltaDefinitionSerializer.serialize(actual, true))
  }

  CustomDeltaDefinition toCustomDeltaDefinition(LinkedHashMap<String, Serializable> cdd)
  {
    return customDeltaDefinitionSerializer.deserialize(JsonUtils.toJSON(cdd).toString(),
                                                       customDeltaDefinitionSerializer.contentVersion)
  }
}