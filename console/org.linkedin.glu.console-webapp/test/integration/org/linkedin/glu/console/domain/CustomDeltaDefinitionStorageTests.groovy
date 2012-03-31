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
import org.linkedin.glu.orchestration.engine.delta.LightUserCustomDeltaDefinition
import org.linkedin.glu.orchestration.engine.delta.DeltaServiceImpl
import com.fasterxml.jackson.core.JsonParseException

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

  /**
   * Test for save functionnality
   */
  public void testSave()
  {
    assertEquals(0, customDeltaDefinitionStorage.findAllByUsername('user1', true, [:]).list.size())
    assertEquals(0, customDeltaDefinitionStorage.findAllByUsername('user1', false, [:]).list.size())
    // bootstrap initializes 1 entry
    assertEquals(1, customDeltaDefinitionStorage.findAllShareable(false, [:]).list.size())
    assertEquals(1, customDeltaDefinitionStorage.findAllShareable(true, [:]).list.size())

    assertNull(customDeltaDefinitionStorage.findByUsernameAndName('user1', 'd1'))

    UserCustomDeltaDefinition ud11 =
      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
                                    username: 'user1')

    // it should work because there is nothing in the database
    assertTrue(customDeltaDefinitionStorage.save(ud11))

    UserCustomDeltaDefinition ud11Read =
      customDeltaDefinitionStorage.findByUsernameAndName('user1', 'd1')

    checkCustomDeltaDefinition(ud11.customDeltaDefinition, ud11Read.customDeltaDefinition)
    assertEquals(ud11.id, ud11Read.id)
    assertEquals('d1', ud11Read.name)
    assertEquals('desc1', ud11Read.description)

    // it should not work because there is already an entry for this user with this name
    UserCustomDeltaDefinition ud11Duplicate =
      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
                                    username: 'user1')

    assertFalse(customDeltaDefinitionStorage.save(ud11Duplicate))
    assertEquals(1, ud11Duplicate.errors.errorCount)
    assertEquals(1, ud11Duplicate.errors.fieldErrorCount)
    def errors = ud11Duplicate.errors.getFieldErrors('name')
    assertEquals(1, errors.size())
    assertEquals('d1', errors[0].rejectedValue)

    UserCustomDeltaDefinition ud12 =
      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
                                    username: 'user2')

    // it should work because it is a different user using the same name
    assertTrue(customDeltaDefinitionStorage.save(ud12))

    // creating an entry with no username
    UserCustomDeltaDefinition ud1N =
      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
                                    username: null)

    assertTrue(customDeltaDefinitionStorage.save(ud1N))

    // adding another one for user 1
    cdd.name = 'd2'
    UserCustomDeltaDefinition ud21 =
      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
                                    username: 'user1',
                                    shareable: false)

    // it should work because there is nothing in the database
    assertTrue(customDeltaDefinitionStorage.save(ud21))

    UserCustomDeltaDefinition udDN =
      customDeltaDefinitionStorage.findByUsernameAndName(null,
                                                         DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME)

    // testing find all shareable with details
    def map = customDeltaDefinitionStorage.findAllShareable(true, [:])
    assertEquals(4, map.count)
    assertEquals(4, map.list.size())
    assertTrue(map.list.id.containsAll([ud11.id, ud12.id, ud1N.id, udDN.id]))
    map.list.each { assertTrue(it instanceof UserCustomDeltaDefinition) }

    // testing find all shareable without details
    map = customDeltaDefinitionStorage.findAllShareable(false, [:])
    assertEquals(4, map.count)
    assertEquals(4, map.list.size())
    assertTrue(map.list.id.containsAll([ud11.id, ud12.id, ud1N.id, udDN.id]))
    map.list.each { assertTrue(it instanceof LightUserCustomDeltaDefinition) }

  }

  /**
   * Test for constraint: shareable should be <code>true</code> when no username
   */
  void testShareableShouldBeTrueWhenNoUser()
  {
    // creating an entry with no username
    UserCustomDeltaDefinition ud1N =
      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
                                    username: null,
                                    shareable: false)

    assertFalse(customDeltaDefinitionStorage.save(ud1N))
    assertEquals(1, ud1N.errors.errorCount)
    assertEquals(1, ud1N.errors.fieldErrorCount)
    def errors = ud1N.errors.getFieldErrors('shareable')
    assertEquals(1, errors.size())
    assertEquals(Boolean.FALSE, errors[0].rejectedValue)
  }

  /**
   * Test for constraint: name and description must match
   */
  void testNameAndDescriptionConstraints()
  {
    // creating an entry
    UserCustomDeltaDefinition ud11 =
      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
                                    username: 'user1')
    ud11.name = 'nonMatchingName'
    ud11.description = 'nonMatchingDescription'

    assertFalse(customDeltaDefinitionStorage.save(ud11))
    assertEquals(2, ud11.errors.errorCount)
    assertEquals(2, ud11.errors.fieldErrorCount)
    def errors = ud11.errors.getFieldErrors('name')
    assertEquals(1, errors.size())
    assertEquals('nonMatchingName', errors[0].rejectedValue)
    errors = ud11.errors.getFieldErrors('description')
    assertEquals(1, errors.size())
    assertEquals('nonMatchingDescription', errors[0].rejectedValue)
  }

  /**
   * Make sure that content is always valid...
   */
  void testUpdateContent()
  {
    assertNull(customDeltaDefinitionStorage.findByUsernameAndName('user1', 'd1'))

    UserCustomDeltaDefinition ud11 =
      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
                                    username: 'user1')
    assertTrue(customDeltaDefinitionStorage.save(ud11))

    UserCustomDeltaDefinition ud11Read =
      customDeltaDefinitionStorage.findByUsernameAndName('user1', 'd1')

    // not a json string... should fail (using updateContent method)
    shouldFail(JsonParseException) {
      ud11Read.updateContent('abc')
    }

    // should not affect the underlying object!
    ud11Read =
      customDeltaDefinitionStorage.findByUsernameAndName('user1', 'd1')
    assertTrue(ud11Read.content != 'abc')

    // setting content explicitely will make update fail => should not be updated!
    ud11Read.content = 'abc'
    assertFalse(customDeltaDefinitionStorage.save(ud11Read))
  }

  /**
   * Test fror deleting a custom delta definition
   */
  void testDelete()
  {
    assertNull(customDeltaDefinitionStorage.findByUsernameAndName('user1', 'd1'))

    UserCustomDeltaDefinition ud11 =
      new UserCustomDeltaDefinition(customDeltaDefinition: toCustomDeltaDefinition(cdd),
                                    username: 'user1')
    assertTrue(customDeltaDefinitionStorage.save(ud11))

    UserCustomDeltaDefinition ud11Read =
      customDeltaDefinitionStorage.findByUsernameAndName('user1', 'd1')
    assertNotNull(ud11Read)
    println customDeltaDefinitionStorage.delete(ud11Read)

    assertNull(customDeltaDefinitionStorage.findByUsernameAndName('user1', 'd1'))
  }

  private void checkCustomDeltaDefinition(CustomDeltaDefinition expected,
                                          CustomDeltaDefinition actual)
  {
    assertEquals(customDeltaDefinitionSerializer.serialize(expected, true),
                 customDeltaDefinitionSerializer.serialize(actual, true))
  }

  CustomDeltaDefinition toCustomDeltaDefinition(LinkedHashMap<String, Serializable> cdd)
  {
    return customDeltaDefinitionSerializer.deserialize(JsonUtils.compactPrint(cdd),
                                                       customDeltaDefinitionSerializer.contentVersion)
  }
}