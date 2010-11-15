/*
 * Copyright 2010-2010 LinkedIn, Inc
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

package test.provisioner.core.model

import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.core.model.JSONSystemModelSerializer
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils

/**
 * @author ypujante@linkedin.com */
class TestSystemModel extends GroovyTestCase
{
  public void testSerializer()
  {
    def sd = new SystemModel(fabric: 'f1', metadata: [m1: 'v1'])

    (1..2).each {
      sd.addEntry(new SystemEntry(agent: 'h1',
                                  mountPoint: "/m${it}",
                                  script: 's1',
                                  initParameters: [ip1: 'iv1', ip2: ['c1'], ip3: [m1: 'mv1']],
                                  metadata: [em1: 'ev1']))
    }

    def serializer = new JSONSystemModelSerializer(prettyPrint: 2)
    assertEquals(sd, serializer.deserialize(serializer.serialize(sd)))
  }

  public void testFilters()
  {
    def sd = new SystemModel(fabric: 'f1', metadata: [m1: 'v1'])

    sd.addEntry(new SystemEntry(agent: 'h1',
                                mountPoint: "/m/1",
                                script: 's1',
                                initParameters: [ip1: 'iv1', ip2: ['c1'], ip3: [m1: 'mv1']],
                                metadata: [em1: 'ev1']))

    sd.addEntry(new SystemEntry(agent: 'h1',
                                mountPoint: "/m/2",
                                script: 's2',
                                initParameters: [ip1: 'iv2'],
                                metadata: [em1: 'ev2', em2: [eem2: 'eev2']]))

    sd.addEntry(new SystemEntry(agent: 'h2',
                                mountPoint: "/m/1",
                                script: 's1',
                                initParameters: [ip3: [m1: 'mv1']],
                                metadata: [em1: 'ev2']))

    def allEntries = sd.findEntries()

    def filteredModel = sd.filterBy('agent', 'h2')
    assertEquals([allEntries[2]], filteredModel.findEntries())
    assertEquals(sd.fabric, filteredModel.fabric)
    assertEquals(sd.metadata, filteredModel.metadata)

    filteredModel = sd.filterByMetadata('em1', 'ev1')
    assertEquals([allEntries[0]], filteredModel.findEntries())
    assertEquals(sd.fabric, filteredModel.fabric)
    assertEquals(sd.metadata, filteredModel.metadata)

    filteredModel = sd.filterBy('metadata.em1', 'ev1')
    assertEquals([allEntries[0]], filteredModel.findEntries())
    assertEquals(sd.fabric, filteredModel.fabric)
    assertEquals(sd.metadata, filteredModel.metadata)

    filteredModel = sd.filterByMetadata('em1', 'ev2')
    assertEquals([allEntries[1], allEntries[2]], filteredModel.findEntries())
    assertEquals(sd.fabric, filteredModel.fabric)
    assertEquals(sd.metadata, filteredModel.metadata)

    filteredModel = sd.filterBy('initParameters.ip3.m1', 'mv1')
    assertEquals([allEntries[0], allEntries[2]], filteredModel.findEntries())
    assertEquals(sd.fabric, filteredModel.fabric)
    assertEquals(sd.metadata, filteredModel.metadata)

    filteredModel = sd.filterBy { it.agent == 'h1' && it.script == 's2' }
    assertEquals([allEntries[1]], filteredModel.findEntries())
    assertEquals(sd.fabric, filteredModel.fabric)
    assertEquals(sd.metadata, filteredModel.metadata)

    filteredModel = sd.filterByMetadata('n1.n2.n3', 'nnn')
    assertEquals([], filteredModel.findEntries())
    assertEquals(sd.fabric, filteredModel.fabric)
    assertEquals(sd.metadata, filteredModel.metadata)

    filteredModel = sd.filterBy("agent='h1';metadata.em2.eem2='eev2'")
    assertEquals([allEntries[1]], filteredModel.findEntries())
    assertEquals(sd.fabric, filteredModel.fabric)
    assertEquals(sd.metadata, filteredModel.metadata)
  }

  public void testStats()
  {
    def sd = new SystemModel(fabric: 'f1', metadata: [m1: 'v1'])

    sd.addEntry(new SystemEntry(agent: 'h1',
                                mountPoint: "/m/1",
                                script: 's1',
                                initParameters: [ip1: 'iv1', ip2: ['c1'], ip3: [m1: 'mv1']],
                                metadata: [em1: 'ev1']))

    sd.addEntry(new SystemEntry(agent: 'h1',
                                mountPoint: "/m/2",
                                script: 's2',
                                initParameters: [ip1: 'iv2'],
                                metadata: [em1: 'ev2', em2: [eem2: 'eev2']]))

    sd.addEntry(new SystemEntry(agent: 'h2',
                                mountPoint: "/m/1",
                                script: 's3',
                                initParameters: [ip3: [m1: 'mv1']],
                                metadata: [em1: 'ev2']))

    def expectedStats =
    [
        agent: 2,
        mountPoint: 2,
        script: 3,
        key: 3,
        'initParameters.ip1': 2,
        'initParameters.ip2[0]': 'c1',
        'initParameters.ip3.m1': 'mv1',
        'metadata.em1': 2,
        'metadata.em2.eem2': 'eev2',
    ]

    assertTrue(GroovyCollectionsUtils.compareIgnoreType(expectedStats, sd.computeStats()))

    expectedStats =
    [
        agent: 2,
        script: 3,
        'initParameters.ip2[0]': 'c1'
    ]

    assertTrue(GroovyCollectionsUtils.compareIgnoreType(expectedStats,
                                                  sd.computeStats(['agent',
                                                                  'script',
                                                                  'initParameters.ip2[0]'])))

    assertTrue(GroovyCollectionsUtils.compareIgnoreType(['ev1', 'ev2'], sd.groupByEntryMetadata('em1')))
  }
}
