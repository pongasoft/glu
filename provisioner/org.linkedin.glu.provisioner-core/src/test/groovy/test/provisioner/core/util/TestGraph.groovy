/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package test.provisioner.core.util

import org.linkedin.glu.provisioner.core.graph.GraphNode
import org.linkedin.glu.provisioner.core.graph.Graph
import org.linkedin.glu.provisioner.core.graph.DepFirstVisitor

/**
 * Test the use of the graph
 *
 * author:  Riccardo Ferretti
 * created: Sep 15, 2009
 */
public class TestGraph extends GroovyTestCase
{
  public void testGraphNode()
  {
    def node = new GraphNode('1')

    assertEquals('1', node.value)
    assertNull(node['x'])

    node = new GraphNode('2', ['key':'value'])
    assertEquals('2', node.value)
    assertEquals('value', node['key'])

  }

  public void testGraph()
  {
    def node1 = new GraphNode('1')
    def node2 = new GraphNode('2')
    def node3 = new GraphNode('3')
    def node4 = new GraphNode('4')
    def node5 = new GraphNode('5')
    def node6 = new GraphNode('6')

    Graph graph = new Graph()

    assertEquals(0, graph.nodes.size())
    assertEquals(0, graph.seeds.size())

    graph.addNode node1
    assertEquals(1, graph.nodes.size())
    assertEquals(1, graph.seeds.size())

    graph.addNode(node2, [node1], null)
    assertEquals(2, graph.nodes.size())
    assertEquals(1, graph.seeds.size())
    assertTrue(graph.seeds.contains(node1))

    graph.addNode(node3, [node1, node2], null)
    assertEquals(3, graph.nodes.size())
    assertEquals(1, graph.seeds.size())
    assertTrue(graph.seeds.contains(node1))

    graph.addNode(node4, null, [node2])
    assertEquals(4, graph.nodes.size())
    assertEquals(2, graph.seeds.size())
    assertTrue(graph.seeds.contains(node1))
    assertTrue(graph.seeds.contains(node4))

    graph.addNode(node5)
    assertEquals(5, graph.nodes.size())
    assertEquals(3, graph.seeds.size())
    assertTrue(graph.seeds.contains(node1))
    assertTrue(graph.seeds.contains(node4))
    assertTrue(graph.seeds.contains(node5))


    graph.addNode(node6, [node5], [node4])
    assertEquals(6, graph.nodes.size())
    assertEquals(2, graph.seeds.size())
    assertTrue(graph.seeds.contains(node1))
    assertTrue(graph.seeds.contains(node5))

  }


  void testTraverseGraph()
  {

    def node1 = new GraphNode('1')
    def node2 = new GraphNode('2')
    def node3 = new GraphNode('3')
    def node4 = new GraphNode('4')
    def node5 = new GraphNode('5')
    def node6 = new GraphNode('6')

    Graph graph = new Graph()
    graph.addNode(node1)
    graph.addNode(node2, [node1], null)
    graph.addNode(node3, [node1, node2], null)
    graph.addNode(node4, null, [node2])
    graph.addNode(node5)
    graph.addNode(node6, [node5], [node4])

    // let's visit!
    def visitor = new DepFirstVisitor(graph)

    def res = []
    visitor.accept { res << it.value }

    // 5 or 1 could be in any order, the order of the others is set
    assertTrue(['1','5','6','4','2','3'] == res || ['5','1','6','4','2','3'] == res)
  }

  void testReverseGraph()
  {
    def node1 = new GraphNode('1')
    def node2 = new GraphNode('2')
    def node3 = new GraphNode('3')
    def node4 = new GraphNode('4')
    def node5 = new GraphNode('5')
    def node6 = new GraphNode('6')

    Graph graph = new Graph()
    graph.addNode(node1)
    graph.addNode(node2, [node1], null)
    graph.addNode(node3, [node1, node2], null)
    graph.addNode(node4, null, [node2])
    graph.addNode(node5)
    graph.addNode(node6, [node5], [node4])

    assertEquals(new HashSet([node1, node5]), graph.seeds)
    assertEquals(new HashSet([node2, node3]), graph.getOutboundNodes(node1))
    assertEquals(new HashSet(), graph.getInboundNodes(node1))
    assertEquals(new HashSet([node3]), graph.getOutboundNodes(node2))
    assertEquals(new HashSet([node1, node4]), graph.getInboundNodes(node2))
    assertEquals(new HashSet(), graph.getOutboundNodes(node3))
    assertEquals(new HashSet([node2, node1]), graph.getInboundNodes(node3))
    assertEquals(new HashSet([node2]), graph.getOutboundNodes(node4))
    assertEquals(new HashSet([node6]), graph.getInboundNodes(node4))
    assertEquals(new HashSet([node6]), graph.getOutboundNodes(node5))
    assertEquals(new HashSet(), graph.getInboundNodes(node5))
    assertEquals(new HashSet([node4]), graph.getOutboundNodes(node6))
    assertEquals(new HashSet([node5]), graph.getInboundNodes(node6))

    graph.reverse()

    assertEquals(new HashSet([node3]), graph.seeds)
    assertEquals(new HashSet(), graph.getOutboundNodes(node1))
    assertEquals(new HashSet([node2, node3]), graph.getInboundNodes(node1))
    assertEquals(new HashSet([node1, node4]), graph.getOutboundNodes(node2))
    assertEquals(new HashSet([node3]), graph.getInboundNodes(node2))
    assertEquals(new HashSet([node2, node1]), graph.getOutboundNodes(node3))
    assertEquals(new HashSet(), graph.getInboundNodes(node3))
    assertEquals(new HashSet([node6]), graph.getOutboundNodes(node4))
    assertEquals(new HashSet([node2]), graph.getInboundNodes(node4))
    assertEquals(new HashSet(), graph.getOutboundNodes(node5))
    assertEquals(new HashSet([node6]), graph.getInboundNodes(node5))
    assertEquals(new HashSet([node5]), graph.getOutboundNodes(node6))
    assertEquals(new HashSet([node4]), graph.getInboundNodes(node6))

  }

  void testGraphNodeEquals()
  {
    assertEquals([new GraphNode('1', ['key1':'value1']),new GraphNode('2', ['key1':'value2','key2':'value3'])],
                 [new GraphNode('1', ['key1':'value1']),new GraphNode('2', ['key1':'value2',"key2":"value3"])],)
  }

  void testGraphProperties()
  {
    // define nodes and graph
    def node1 = new GraphNode('1', ['key1':'value1'])
    def node2 = new GraphNode('2', ['key1':'value2','key2':'value3'])
    def node3 = new GraphNode('3', ['key1':'value3'])
    def node4 = new GraphNode('4', ['key1':'value1'])
    def node5 = new GraphNode('5', ['key2':'value2'])
    def node6 = new GraphNode('6', ['key1':'value1'])

    Graph graph = new Graph()
    graph.addNode(node1)
    graph.addNode(node2, [node1], null)
    graph.addNode(node3, [node1, node2], null)
    graph.addNode(node4, null, [node2])
    graph.addNode(node5)
    graph.addNode(node6, [node5], [node4])

    // check the properties and the filtering
    assertEquals (new HashSet(['key1','key2']), graph.getPropertyKeys())
    assertEquals (new HashSet(['value1','value2','value3']), graph.getPropertyValues('key1'))
    assertEquals (new HashSet([node1, node2, node3, node4, node6]), graph.getNodes('key1'))
    assertEquals (new HashSet([node1, node4, node6]), graph.getNodes('key1', 'value1'))

    assertEquals (new HashSet(['value2','value3']), graph.getPropertyValues('key2'))
    assertEquals (new HashSet([node2, node5]), graph.getNodes('key2'))
    assertEquals (new HashSet([node2]), graph.getNodes('key2', 'value3'))
  }

  void testSubgraph()
  {
    // define nodes and graph
    def node1 = new GraphNode('1', ['key1':'value1'])
    def node2 = new GraphNode('2', ['key1':'value2','key2':'value3'])
    def node3 = new GraphNode('3', ['key1':'value3'])
    def node4 = new GraphNode('4', ['key1':'value1'])
    def node5 = new GraphNode('5', ['key2':'value2'])
    def node6 = new GraphNode('6', ['key1':'value1'])

    Graph graph = new Graph()
    graph.addNode(node1)
    graph.addNode(node2, [node1], null)
    graph.addNode(node3, [node1, node2, node4], null)
    graph.addNode(node4, null, [node2])
    graph.addNode(node5)
    graph.addNode(node6, [node5], [node4])

    // create a subgraph
    Graph key1subgraph = new Graph()
    key1subgraph.addNode(node1)
    key1subgraph.addNode(node2, [node1], null)
    key1subgraph.addNode(node3, [node1, node2, node4], null)
    key1subgraph.addNode(node4, null, [node2])
    key1subgraph.addNode(node6, null, [node4])

    assertEquals(new HashSet([node1, node6]), graph.getSubGraph('key1').seeds)
    assertEquals(key1subgraph, graph.getSubGraph('key1'))

    // check an empty query
    assertEquals(0, graph.getSubGraph {it.value == '10'}.size() )

    // check a query that returns a graph with one node
    assertEquals(1, graph.getSubGraph {it.value == '5'}.size() )

    // check a query that returns a graph
    Graph sg = graph.getSubGraph {'123'.contains(it.value)}
    assertEquals(new HashSet([node1, node2, node3]), new HashSet(sg.nodes))
    // note that node4 has been execluded
    assertEquals(new HashSet([node1, node2]), new HashSet(sg.getInboundNodes(node3)))
  }


  void testSubGraphAndDep()
  {
    // define nodes and graph
    def node1 = new GraphNode('1', ['key1':'value1'])
    def node2 = new GraphNode('2', ['key1':'value2','key2':'value3'])
    def node3 = new GraphNode('3', ['key1':'value3'])
    def node4 = new GraphNode('4', ['key1':'value1'])
    def node5 = new GraphNode('5', ['key2':'value2'])
    def node6 = new GraphNode('6', ['key1':'value1'])

    Graph graph = new Graph()
    graph.addNode(node1)
    graph.addNode(node2, [node1], null)
    graph.addNode(node3, [node1, node2, node4], null)
    graph.addNode(node4, null, [node2])
    graph.addNode(node5)
    graph.addNode(node6, [node5], [node4])

    assertEquals(new HashSet([node3]), graph.getSubGraph([node3], false).nodes)
    assertEquals(new HashSet([node3, node1, node2, node4]), graph.getSubGraph([node3], true).nodes)

  }

  void testNodesByTree()
  {
    // define nodes and graph
    def node1 = new GraphNode('1')
    def node2 = new GraphNode('2')
    def node3 = new GraphNode('3')
    def node4 = new GraphNode('4')
    def node5 = new GraphNode('5')
    def node6 = new GraphNode('6')
    def node7 = new GraphNode('7')
    def node8 = new GraphNode('8')
    def node9 = new GraphNode('9')
    def node10 = new GraphNode('10')

    Graph graph = new Graph()
    graph.addNode(node1)
    graph.addNode(node2, [node1], null)
    graph.addNode(node3, [node1, node2, node4], null)
    graph.addNode(node4, null, [node2])
    graph.addNode(node5)
    graph.addNode(node6, [node5], [node4])
    graph.addNode(node10)
    graph.addNode(node9, [node10], null)
    graph.addNode(node8, [node10, node6], null)
    graph.addNode(node7, [node10, node1, node5], null)

    def expected = [
      [node1, node5, node10],
      [node6, node7, node9],
      [node4, node8],
      [node2],
      [node3]
    ]

    assertEquals(expected,
                 graph.getNodesByDepth {a, b ->
                   Integer.valueOf(a.value) <=> Integer.valueOf(b.value)
                 }
    )
  }
}