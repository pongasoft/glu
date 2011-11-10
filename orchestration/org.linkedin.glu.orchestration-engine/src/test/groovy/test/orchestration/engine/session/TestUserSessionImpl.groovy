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

package test.orchestration.engine.session

import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinitionSerializer
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinition
import org.linkedin.glu.orchestration.engine.session.UserSessionImpl
import org.linkedin.glu.orchestration.engine.delta.UserCustomDeltaDefinition
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinitionSerializerImpl

/**
 * @author yan@pongasoft.com */
public class TestUserSessionImpl extends GroovyTestCase
{
  CustomDeltaDefinitionSerializer customDeltaDefinitionSerializer = new CustomDeltaDefinitionSerializerImpl()

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
   * Test for the custom display name
   */
  public void testCustomFilterDisplayName()
  {
    CustomDeltaDefinition definition = toCustomDeltaDefinition(cdd)
    def ucdd = new UserCustomDeltaDefinition(customDeltaDefinition: definition)

    def userSession = new UserSessionImpl(original: ucdd,
                                          current: ucdd.clone(),
                                          fabric: 'f1')

    // no custom filter
    userSession.clearCustomFilter()
    assertEquals("Fabric [f1]", userSession.customFilterDisplayName)

    // 1 filter
    userSession.setCustomFilter("metadata.version='1.0.0'")
    assertEquals("vs [1.0.0]", userSession.customFilterDisplayName)

    // 1 filter (node defined in the definition)
    userSession.setCustomFilter("metadata.undefined='foo'")
    assertEquals("metadata.undefined [foo]", userSession.customFilterDisplayName)

    // 2 filters
    userSession.setCustomFilter("+metadata.version='1.0.0'")
    assertEquals("metadata.undefined [foo] - vs [1.0.0]", userSession.customFilterDisplayName)

    // many filters
    userSession.setCustomFilter("and{metadata.version='1.0.0';or{agent='ag1';metadata.mountPoint='/a'}}")
    assertEquals("vs [1.0.0] - (ag [ag1] or metadata.mountPoint [/a])", userSession.customFilterDisplayName)
  }

  CustomDeltaDefinition toCustomDeltaDefinition(LinkedHashMap<String, Serializable> cdd)
  {
    return customDeltaDefinitionSerializer.deserialize(JsonUtils.toJSON(cdd).toString(),
                                                       customDeltaDefinitionSerializer.contentVersion)
  }
}