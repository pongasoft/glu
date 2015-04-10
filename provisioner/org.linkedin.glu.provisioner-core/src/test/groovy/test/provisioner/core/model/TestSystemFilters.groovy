/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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

import org.linkedin.glu.provisioner.core.model.FlattenSystemFilter
import org.linkedin.glu.provisioner.core.model.PropertySystemFilter
import org.linkedin.glu.provisioner.core.model.SystemEntryKeyModelFilter
import org.linkedin.glu.provisioner.core.model.SystemEntryStateSystemFilter
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.core.model.SystemFilterBuilder
import org.linkedin.glu.provisioner.core.model.TagsSystemFilter

/**
 * @author ypujante@linkedin.com */
class TestSystemFilters extends GroovyTestCase
{
  SystemModel model
  def entries = []

  protected void setUp()
  {
    super.setUp()

    model= new SystemModel(fabric: 'f1', metadata: [m1: 'v1'])

    model.addAgentTags('h1', ['a:tag1', 'a:tag2'])
    model.addAgentTags('h2', ['a:tag2', 'a:tag3'])

    entries << new SystemEntry(agent: 'h1',
                               mountPoint: "/m/1",
                               script: 's1',
                               initParameters: [ip1: 'iv1', ip2: ['c1'], ip3: [m1: 'mv1'], ip4: [[m2: 'mv2'], [m2: 'mv3'], [m3: 'mv4']]],
                               metadata: [em1: 'ev1'],
                               tags: ['e:tag1', 'e:tag2'])

    entries << new SystemEntry(agent: 'h1',
                               mountPoint: "/m/2",
                               script: 's2',
                               initParameters: [ip1: 'iv2'],
                               metadata: [em1: 'ev2', em2: [eem2: 'eev2']],
                               tags: ['e:tag1', 'e:tag3'])

    entries << new SystemEntry(agent: 'h2',
                               mountPoint: "/m/1",
                               script: 's1',
                               initParameters: [ip3: [m1: 'mv1']],
                               metadata: [em1: 'ev2'],
                               tags: ['e:tag3'])

    entries.each { model.addEntry(it) }
  }

  public void testSystemFilterBuilderParse()
  {
    assertEquals(null, SystemFilterBuilder.parse(null))

    checkFiltering("initParameters.ip4[0..-1].m2='mv3'", "initParameters.ip4[[0, -1]].m2='mv3'", ["h1:/m/1"])
    checkFiltering("initParameters.ip4[1].m2='mv3'", "initParameters.ip4[1].m2='mv3'", ["h1:/m/1"])
    checkFiltering("initParameters.ip4[25].m2='mv3'", "initParameters.ip4[25].m2='mv3'", [])
    checkFiltering("metadata.em1='ev1'", "metadata.em1='ev1'", ["h1:/m/1"])
    checkFiltering("initParameters.ip1=='iv2'", "initParameters.ip1='iv2'", ["h1:/m/2"])
    checkFiltering("initParameters.ip1='iv2'", "initParameters.ip1='iv2'", ["h1:/m/2"])
    checkFiltering("agent='h1'", "agent='h1'", ["h1:/m/1", "h1:/m/2"])
    checkFiltering("metadata.em2.eem2='eev2'", "metadata.em2.eem2='eev2'", ["h1:/m/2"])
    checkFiltering("agent='h1';metadata.em1='ev1'", "and{agent='h1';metadata.em1='ev1'}", ["h1:/m/1"])
    checkFiltering("and{agent='h1';metadata.em1='ev1'}", "and{agent='h1';metadata.em1='ev1'}", ["h1:/m/1"])
    checkFiltering("or{agent='h1';metadata.em1='ev1'}", "or{agent='h1';metadata.em1='ev1'}", ["h1:/m/1", "h1:/m/2"])
    checkFiltering("not{or{agent='h1';metadata.em1='ev1'}}", "not{or{agent='h1';metadata.em1='ev1'}}", ["h2:/m/1"])

    shouldFail(IllegalArgumentException) {SystemFilterBuilder.parse('a=b')}
    shouldFail(IllegalArgumentException) {SystemFilterBuilder.parse('a.b=b')}

    def dsl = """
   agent = 'h1'
   mountPoint == '/m/1'
   and {
     agent = 'h2'
     mountPoint == '/m/2'
   }
   or {
     agent = 'h3'
     mountPoint == '/m/3'
   }
   not {
     agent = 'h3'
     mountPoint == '/m/4'
   }
"""

    assertEquals("and{agent='h1';" +
                 "mountPoint='/m/1';" +
                 "and{agent='h2';mountPoint='/m/2'};" +
                 "or{agent='h3';mountPoint='/m/3'};" +
                 "not{and{agent='h3';mountPoint='/m/4'}}}",
                 SystemFilterBuilder.parse(dsl).toString())

    def filter = new SystemFilterBuilder().and {
      agent = 'h1'
      mountPoint == '/m/1'
      and {
        agent = 'h2'
        mountPoint == '/m/2'
      }
      or {
        agent = 'h3'
        mountPoint == '/m/3'
      }
      not {
        agent = 'h3'
        mountPoint == '/m/4'
      }
    }

    assertEquals("and{agent='h1';" +
                 "mountPoint='/m/1';" +
                 "and{agent='h2';mountPoint='/m/2'};" +
                 "or{agent='h3';mountPoint='/m/3'};" +
                 "not{and{agent='h3';mountPoint='/m/4'}}}",
                 filter.toString())
  }

  public void testTagsFiltering()
  {
    // tags testing
    checkFiltering("tags='e:tag1'", "tags='e:tag1'", ["h1:/m/1", "h1:/m/2"])

    checkFiltering("tags='e:tag3;a:tag1'", "tags='a:tag1;e:tag3'", ["h1:/m/2"])
    checkFiltering("tags=='e:tag3;a:tag1'", "tags='a:tag1;e:tag3'", ["h1:/m/2"])
    checkFiltering("tags.hasAll('e:tag3;a:tag1')", "tags='a:tag1;e:tag3'", ["h1:/m/2"])
    checkFiltering("tags.hasAll(['e:tag3', 'a:tag1'])", "tags='a:tag1;e:tag3'", ["h1:/m/2"])

    checkFiltering("tags.hasAny('e:tag3;a:tag1')", "tags.hasAny('a:tag1;e:tag3')", ["h1:/m/1", "h1:/m/2", "h2:/m/1"])
    checkFiltering("tags.hasAny(['e:tag3', 'a:tag1'])", "tags.hasAny('a:tag1;e:tag3')", ["h1:/m/1", "h1:/m/2", "h2:/m/1"])
  }

  public void testNullSystemEntry()
  {
    assertFalse(new PropertySystemFilter(name: 'metadata.foo', value: 'abc').filter(null))
    assertFalse(new FlattenSystemFilter(name: 'metadata.foo', value: 'abc').filter(null))
    assertFalse(new SystemEntryKeyModelFilter(keys: ['/a']).filter(null))
    assertFalse(new SystemEntryStateSystemFilter(states: ['running']).filter(null))
    assertFalse(new TagsSystemFilter(['osx'], true).filter(null))
    assertFalse(new TagsSystemFilter(['osx'], false).filter(null))
  }

  private checkFiltering(String filterString, String expectedToString, expectedEntries)
  {
    def filter = SystemFilterBuilder.parse(filterString)
    assertEquals(expectedToString, filter.toString())

    SystemModel newModel = model.filterBy(filter)
    
    assertEquals(expectedEntries, newModel.findEntries().key)
  }

}

//  public void testSpeed()
//  {
//    int N = 10000
//
//    def entries = [:]
//
//    (1..N).each { i ->
//      SystemEntry entry = new SystemEntry(agent: "agent-${i}".toString(),
//                                          mountPoint: "/m${i}".toString())
//      entry.initParameters.skeleton="ivy:/com.linkedin.network.container/container-jetty/0.0.007-RC1.1${i}".toString()
//      entry.initParameters.wars="ivy:/com.linkedin.network.app/app/0.0.007-RC1.1${i}".toString()
//      entry.metadata.cluster = "cluster-${i}".toString()
//      entry.metadata.container = [name: "container-${i}".toString(), kind: 'servlet']
//      entry.metadata.product = 'network'
//      entry.metadata.version = 'R950'
//      entry.metadata.p1 = [p2: [p3: [p4: "p5-${i}"]]]
//
//      entries[entry.key] = entry
//    }
//
//    Chronos c = new Chronos()
//    (1..100).each { findByDirectAccess(entries)}
//
//    println "findByDirectAccess: ${findByDirectAccess(entries)}"
//    c.tick()
//    (1..100).each { findByDirectAccess(entries)}
//    println c.tick()
//
//    println "findByMetadata: ${findByMetadata(entries)}"
//    c.tick()
//    (1..100).each { findByMetadata(entries)}
//    println c.tick()
//
//    println "findBySystemProperty: ${findBySystemProperty(entries)}"
//    c.tick()
//    (1..100).each { findBySystemProperty(entries)}
//    println c.tick()
//
////    println "findByQueryString: ${findByQueryString(entries)}"
////    c.tick()
////    (1..100).each { findByQueryString(entries)}
////    println c.tick()
//  }
//
//  private def findByDirectAccess(entries)
//  {
//    def res = []
//
//    def filter = new DirectAccessFilter(value: 'container-400')
//
//    entries.values().each { SystemEntry entry ->
//      if(filter.filter(entry))
//        res << entry
//    }
//
//    return res
//  }
//
//  private def findByMetadata(entries)
//  {
//    def res = []
//
//    def filter = new MetadataSystemFilter(name: 'cluster', value: 'cluster-400')
//
//    entries.values().each { SystemEntry entry ->
//      if(filter.filter(entry))
//        res << entry
//    }
//
//    return res
//  }
//
//  private def findBySystemProperty(entries)
//  {
//    def res = []
//
////    def filter = new PropertySystemFilter2(name: 'metadata.container.name', value: 'container-400')
////    def filter = new PropertySystemFilter2(name: 'metadata.cluster', value: 'cluster-400')
//    def filter = new PropertySystemFilter2(name: 'metadata.p1.p2.p3.p4', value: 'p5-400')
//
//    entries.values().each { SystemEntry entry ->
//      if(filter.filter(entry))
//        res << entry
//    }
//
//    return res
//  }
//}
//
//class DirectAccessFilter implements SystemFilter
//{
//  String value
//
//  def Object toExternalRepresentation()
//  {
//    return null;
//  }
//
//  def boolean filter(SystemEntry entry)
//  {
//    return entry.metadata.container.name == value
//  }
//
//  def String getKind()
//  {
//    return null;
//  }
//}
//
//class PropertySystemFilter2 implements SystemFilter
//{
//  String name
//  def value
//
//  private def tokens
//
//  void setName(String name)
//  {
//    tokens = name.tokenize('.')
//  }
//
//  String getKind()
//  {
//    return 'property';
//  }
//
//  def toExternalRepresentation()
//  {
//    return [(name): value]
//  }
//
//  def boolean filter(SystemEntry entry)
//  {
//    def v = entry
//    tokens.each {
//      if(v != null)
//        v = v[it]
//    }
//    return v == value
//  }
//
//  def String toString()
//  {
//    return "p:${name}=${value}".toString();
//  }
//}
//
