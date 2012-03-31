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
   * Test for save as new user custom delta definition
   */
  void testSaveAsNew()
  {
    // saving name1/desc1
    CustomDeltaDefinition definition = toCustomDeltaDefinition(cdd)
    definition.name = 'name1'
    definition.description = 'desc1'
    UserCustomDeltaDefinition ucdd1 =
      new UserCustomDeltaDefinition(username: 'user1',
                                    customDeltaDefinition: definition)
    assertTrue(deltaService.saveUserCustomDeltaDefinition(ucdd1))
    println "ucdd1.id=${ucdd1.id}"

    // saving name2/desc2
    definition = toCustomDeltaDefinition(cdd)
    definition.name = 'name2'
    definition.description = 'desc2'
    UserCustomDeltaDefinition ucdd2 =
      new UserCustomDeltaDefinition(username: 'user1',
                                    customDeltaDefinition: definition)
    assertTrue(deltaService.saveUserCustomDeltaDefinition(ucdd2))
    println "ucdd2.id=${ucdd2.id}"

    // saving name2 as new name3
    ucdd2 = deltaService.findUserCustomDeltaDefinitionByName('name2')
    definition = toCustomDeltaDefinition(cdd)
    definition.name = 'name3'
    definition.description = 'desc3'
    ucdd2.customDeltaDefinition = definition
    UserCustomDeltaDefinition ucdd3 = deltaService.saveAsNewUserCustomDeltaDefinition(ucdd2)
    assertFalse(ucdd3.hasErrors())
    println "ucdd3.id=${ucdd3.id}"

    ucdd3 = deltaService.findUserCustomDeltaDefinitionByName('name3')
    assertEquals('name3', ucdd3.name)
    assertEquals('desc3', ucdd3.description)
    println "ucdd3.id=${ucdd3.id}"

    // saving name2 as name1 (will fail => diplicate name)
    ucdd2 = deltaService.findUserCustomDeltaDefinitionByName('name2')
    println "ucdd2.id=${ucdd2.id}"
    definition = toCustomDeltaDefinition(cdd)
    definition.name = 'name1'
    definition.description = 'desc4'
    ucdd2.customDeltaDefinition = definition
    UserCustomDeltaDefinition ucdd4 = deltaService.saveAsNewUserCustomDeltaDefinition(ucdd2)
    println "ucdd4.id=${ucdd4.id}"
    assertTrue(ucdd4.hasErrors())
    assertEquals(1, ucdd4.errors.errorCount)
    assertEquals(1, ucdd4.errors.fieldErrorCount)
    def errors = ucdd4.errors.getFieldErrors('name')
    assertEquals(1, errors.size())
    assertEquals('name1', errors[0].rejectedValue)
  }


  CustomDeltaDefinition toCustomDeltaDefinition(LinkedHashMap<String, Serializable> cdd)
  {
    return customDeltaDefinitionSerializer.deserialize(JsonUtils.compactPrint(cdd),
                                                       customDeltaDefinitionSerializer.contentVersion)
  }
}