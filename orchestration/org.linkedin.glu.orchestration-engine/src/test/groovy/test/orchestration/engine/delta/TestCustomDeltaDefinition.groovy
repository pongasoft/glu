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

package test.orchestration.engine.delta

import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinition
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinitionSerializer
import org.linkedin.glu.orchestration.engine.delta.CustomDeltaDefinitionSerializerImpl

/**
 * @author yan@pongasoft.com */
public class TestCustomDeltaDefinition extends GroovyTestCase
{
  CustomDeltaDefinitionSerializer serializer = new CustomDeltaDefinitionSerializerImpl()
  /**
   * Test for backward compatibility
   */
  public void testFromDashboard()
  {
    def oldDashboard =
      [
          mountPoint: [checked: true, name: 'mountPoint', groupBy: true, linkFilter: true],
          agent: [checked: true, name: 'agent', groupBy: true],
          'tag': [checked: false, name: 'tag', groupBy: true, linkFilter: true],
          'tags': [checked: true, name: 'tags', linkFilter: true],
          'metadata.container.name': [checked: true, name: 'container', groupBy: true, linkFilter: true],
          'metadata.version': [checked: true, name: 'version', groupBy: true],
          'metadata.product': [checked: true, name: 'product', groupBy: true, linkFilter: true],
          'metadata.cluster': [checked: true, name: 'cluster', groupBy: true, linkFilter: true],
          'initParameters.skeleton': [checked: false, name: 'skeleton', groupBy: true],
          script: [checked: false, name: 'script', groupBy: true],
          'metadata.modifiedTime': [checked: false, name: 'Last Modified', groupBy: false],
          status: [checked: true, name: 'status', groupBy: true]
      ]

    def jsonDashboard = """{
  "columnsDefinition": [
    {
      "groupBy": "uniqueCountOrUniqueVal",
      "linkable": true,
      "name": "mountPoint",
      "orderBy": "asc",
      "source": "mountPoint",
      "visible": true
    },
    {
      "groupBy": "uniqueCountOrUniqueVal",
      "linkable": true,
      "name": "agent",
      "orderBy": "asc",
      "source": "agent",
      "visible": true
    },
    {
      "groupBy": "uniqueVals",
      "linkable": true,
      "name": "tags",
      "orderBy": "asc",
      "source": "tags",
      "visible": true
    },
    {
      "groupBy": "uniqueCountOrUniqueVal",
      "linkable": true,
      "name": "container",
      "orderBy": "asc",
      "source": "metadata.container.name",
      "visible": true
    },
    {
      "groupBy": "uniqueCountOrUniqueVal",
      "linkable": true,
      "name": "version",
      "orderBy": "asc",
      "source": "metadata.version",
      "visible": true
    },
    {
      "groupBy": "uniqueCountOrUniqueVal",
      "linkable": true,
      "name": "product",
      "orderBy": "asc",
      "source": "metadata.product",
      "visible": true
    },
    {
      "groupBy": "uniqueCountOrUniqueVal",
      "linkable": true,
      "name": "cluster",
      "orderBy": "asc",
      "source": "metadata.cluster",
      "visible": true
    },
    {
      "groupBy": "uniqueCountOrUniqueVal",
      "linkable": true,
      "name": "status",
      "orderBy": "asc",
      "source": "status",
      "visible": true
    },
    {
      "groupBy": "vals",
      "linkable": true,
      "name": "statusInfo",
      "orderBy": "asc",
      "source": "statusInfo",
      "visible": false
    },
    {
      "groupBy": "uniqueCountOrUniqueVal",
      "linkable": true,
      "name": "state",
      "orderBy": "asc",
      "source": "state",
      "visible": false
    }
  ],
  "errorsOnly": false,
  "name": "dashboard",
  "summary": true
}"""

    def newDashboardMap = [
      columnsDefinition:
      [
        [ name: "mountPoint", source: "mountPoint" ],
        [ name: "agent",      source: "agent"],
        [ name: "tags",       source: "tags",       groupBy: "uniqueVals"],
        [ name: "container",  source: "metadata.container.name"],
        [ name: "version",    source: "metadata.version"],
        [ name: "product",    source: "metadata.product"],
        [ name: "cluster",    source: "metadata.cluster"],
        [ name: "status",     source: "status" ],
        [ name: "statusInfo", source: "statusInfo", groupBy: "vals", visible: false],
        [ name: "state",      source: "state",                       visible: false]
      ],
      name: "dashboard"
    ]

    def newDashboardCollection = [
      [ name: "mountPoint", source: "mountPoint" ],
      [ name: "agent",      source: "agent"],
      [ name: "tags",       source: "tags",       groupBy: "uniqueVals"],
      [ name: "container",  source: "metadata.container.name"],
      [ name: "version",    source: "metadata.version"],
      [ name: "product",    source: "metadata.product"],
      [ name: "cluster",    source: "metadata.cluster"],
      [ name: "status",     source: "status" ],
      [ name: "statusInfo", source: "statusInfo", groupBy: "vals", visible: false],
      [ name: "state",      source: "state",                       visible: false]
    ]

    assertEquals(jsonDashboard, serializer.serialize(CustomDeltaDefinition.fromDashboard(oldDashboard),
                                                     true))
    assertEquals(jsonDashboard, serializer.serialize(CustomDeltaDefinition.fromDashboard(jsonDashboard),
                                                     true))
    assertEquals(jsonDashboard, serializer.serialize(CustomDeltaDefinition.fromDashboard(newDashboardMap),
                                                     true))
    assertEquals(jsonDashboard, serializer.serialize(CustomDeltaDefinition.fromDashboard(newDashboardCollection),
                                                     true))
  }
}