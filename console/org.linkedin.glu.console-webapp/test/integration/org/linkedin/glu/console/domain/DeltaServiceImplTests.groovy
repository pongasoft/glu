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

import org.linkedin.glu.orchestration.engine.delta.DeltaServiceImpl
import org.linkedin.glu.orchestration.engine.authorization.AuthorizationService
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinition
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinitionSerializer
import org.linkedin.glu.orchestration.engine.delta.UserCustomDeltaDefinition
import java.security.AccessControlException

import org.linkedin.glu.orchestration.engine.session.UserSession

/**
 * @author yan@pongasoft.com */
public class DeltaServiceImplTests extends GroovyTestCase
{
  CustomDeltaDefinitionSerializer customDeltaDefinitionSerializer

  DeltaServiceImpl deltaService

  // will hold the executing principal
  String executingPrincipal = 'user1'

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

  @Override
  protected void setUp()
  {
    super.setUp()

    def authorizationService = [
      getExecutingPrincipal: {
        return executingPrincipal
      }
    ]

    deltaService.authorizationService = authorizationService as AuthorizationService
  }

  /**
   * Test that save handles executing principal appriopriately
   */
  void testSave()
  {
    UserCustomDeltaDefinition ucdd =
      new UserCustomDeltaDefinition(username: 'user1',
                                    customDeltaDefinition: toCustomDeltaDefinition(cdd))
    assertTrue(deltaService.saveUserCustomDeltaDefinition(ucdd))

    // executing principal mismatch!
    ucdd =
      new UserCustomDeltaDefinition(username: 'user2',
                                    customDeltaDefinition: toCustomDeltaDefinition(cdd))
    shouldFail(AccessControlException) {
      deltaService.saveUserCustomDeltaDefinition(ucdd)
    }
  }

  /**
   * Test for session behavior
   */
  void testSession()
  {
    assertNull(deltaService.findUserCustomDeltaDefinitionByName(DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME))

    // 'user1' has no default => it will return the global default
    UserSession default1 =
      deltaService.findUserSession('dashboard1')
    assertEquals('user1', default1.username)
    assertEquals(DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME, default1.name)

    // this verifies that a new entry in the database has been created
    UserCustomDeltaDefinition default2 =
      deltaService.findUserCustomDeltaDefinitionByName(DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME)
    assertEquals(default1.id, default2.id)

    // user2 has a <default> entry
    executingPrincipal = 'user2'
    assertNull(deltaService.findUserCustomDeltaDefinitionByName(DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME))

    CustomDeltaDefinition definition = toCustomDeltaDefinition(cdd)
    definition.name = DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME
    definition.description = 'user2-<default>'
    UserCustomDeltaDefinition ucdd =
      new UserCustomDeltaDefinition(username: 'user2',
                                    customDeltaDefinition: definition)
    assertTrue(deltaService.saveUserCustomDeltaDefinition(ucdd))

    // verifying that the <default> entry was used
    default1 =
      deltaService.findUserSession('dashboard1')
    assertEquals('user2', default1.username)
    assertEquals(DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME, default1.name)
    assertEquals("user2-<default>", default1.description)

    default2 =
      deltaService.findUserCustomDeltaDefinitionByName(DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME)
    assertEquals(default1.id, default2.id)

    // user3 has a dashboard1 entry
    executingPrincipal = 'user3'
    assertNull(deltaService.findUserCustomDeltaDefinitionByName(DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME))

    definition = toCustomDeltaDefinition(cdd)
    definition.name = 'dashboard1'
    definition.description = 'user3-dashboard1'
    ucdd =
      new UserCustomDeltaDefinition(username: 'user3',
                                    customDeltaDefinition: definition)
    assertTrue(deltaService.saveUserCustomDeltaDefinition(ucdd))

    // verifying that the dashboard1 entry was used
    default1 =
      deltaService.findUserSession('dashboard1')
    assertEquals('user3', default1.username)
    assertEquals('dashboard1', default1.name)
    assertEquals("user3-dashboard1", default1.description)

    default2 =
      deltaService.findUserCustomDeltaDefinitionByName('dashboard1')
    assertEquals(default1.id, default2.id)

    // no default entry was created
    assertNull(deltaService.findUserCustomDeltaDefinitionByName(DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME))

    // session still active... no change
    default1 =
      deltaService.findUserSession('dashboard2')
    assertEquals(default1.id, default2.id)

    // clearing the definition
    deltaService.clearUserSession()

    // dashboard2 does not exist and <default> does not exist either... will be created
    default1 =
      deltaService.findUserSession('dashboard2')
    assertEquals('user3', default1.username)
    assertEquals(DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME, default1.name)
    assertNotSame(default1.id, default2.id)
    assertNotNull(deltaService.findUserCustomDeltaDefinitionByName(DeltaServiceImpl.DEFAULT_CUSTOM_DELTA_DEFINITION_NAME))

  }

  CustomDeltaDefinition toCustomDeltaDefinition(LinkedHashMap<String, Serializable> cdd)
  {
    return customDeltaDefinitionSerializer.deserialize(JsonUtils.toJSON(cdd).toString(),
                                                       customDeltaDefinitionSerializer.contentVersion)
  }
}