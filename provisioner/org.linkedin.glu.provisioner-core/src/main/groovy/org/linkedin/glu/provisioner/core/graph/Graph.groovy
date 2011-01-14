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

package org.linkedin.glu.provisioner.core.graph

/**
 * A graph of objects
 *
 * This class doesn't make a copy of the given collections. Changing the collections
 * after the creation of the graph will affect the graph!
 *
 * This class is not thread safe.
 *
 * author:  Riccardo Ferretti
 * created: Sep 16, 2009
 */
public class Graph <T extends Object> {

  private Set<GraphNode<T>> _nodes = new HashSet()

  private Map<GraphNode<T>, Set<GraphNode<T>>> _inboundConnections = [:]
  private Map<GraphNode<T>, Set<GraphNode<T>>> _outboundConnections = [:]

  def Graph(Collection<GraphNode<T>> nodes)
  {
    nodes?.each {
      if (!it instanceof GraphNode)
      {
        throw new IllegalArgumentException("Nodes can only depend on other nodes (was ${it.class.name}")
      }
      else
      {
        _nodes << it
      }
    }
  }

  void addNode(GraphNode<T> node)
  {
    addNode(node, null, null)
  }

  /**
   * Add a node with dependencies to the graph. If the fromNodes or the toNodes are
   * not already present in the graph, they will be added.
   */
  void addNode(GraphNode<T> node, Collection<GraphNode<T>> fromNodes, Collection<GraphNode<T>> toNodes)
  {
    _nodes << node
    if (!_inboundConnections.containsKey(node)) _inboundConnections[node] = new HashSet()
    if (!_outboundConnections.containsKey(node)) _outboundConnections[node] = new HashSet()
    fromNodes?.each {
      if (!_nodes.contains(it))
      {
        if (!it instanceof GraphNode)
        {
          throw new IllegalArgumentException("Nodes can only depend on other nodes (was ${it.class.name}")
        }
        else
        {
          _nodes << it  // if the fromNode is not in the graph, it will be added
        }
      }
      _inboundConnections[node] << it
      if (!_outboundConnections.containsKey(it))
      {
        _outboundConnections[it] = new HashSet()
      }
      _outboundConnections[it] << node
    }
    toNodes?.each {
      if (!_nodes.contains(it))
      {
        if (!it instanceof GraphNode)
        {
          throw new IllegalArgumentException("Nodes can only depend on other nodes (was ${it.class.name}")
        }
        else
        {
          _nodes << it  // if the toNode is not in the graph, it will be added
        }
      }
      _outboundConnections[node] << it
      if (!_inboundConnections.containsKey(it))
      {
        _inboundConnections[it] = new HashSet()
      }
      _inboundConnections[it] << node
    }
  }


  /**
   * Changes the orientation of all the connections in the graph
   */
  public void reverse()
  {
    nodes.each {
      def tmp = _inboundConnections[it]
      _inboundConnections[it] = _outboundConnections[it]
      _outboundConnections[it] = tmp
    }
  }

  public void leftShift(GraphNode<T> node)
  {
    addNode (node)
  }

  public void leftShift(Graph<T> graph)
  {
    // kinda of brute force way to copy all the nodes, but works
    // (I don't think being more sofisticated would be that beneficial)
    graph.nodes.each {
      addNode (it, graph.getInboundNodes(it), graph.getOutboundNodes(it))
    }
  }

  /**
   * Return the size of the graph (n. of nodes)
   */
  int size()
  {
    return _nodes.size()
  }

  /**
   * Return the collection of nodes that compose this graph
   */
  Set<GraphNode<T>> getNodes()
  {
    return Collections.unmodifiableSet(_nodes)
  }

  /**
   * Rerturn all the nodes that have a property with the given key
   */
  Set<GraphNode<T>> getNodes(String key)
  {
    def res = new HashSet()
    res += _nodes.findAll {it.props.containsKey(key)}
    return res
  }

  /**
   * Rerturn all the nodes that have a property with the given
   * key and value
   */
  Set<GraphNode<T>> getNodes(String key, String value)
  {
    def res = new HashSet()
    res += _nodes.findAll {it[key] == value}
    return res
  }


  /**
   * Return a node that has the given value
   * (NOTE: deterministic int its results only when there is only one node per value) 
   */
  GraphNode<T> getNodeWithValue(T value)
  {
    // TODO LOW RF: use data structure if we need to improve performance
    _nodes.find {it.value == value}
  }

  /**
   * Return a subgraph with only the nodes that contain the given property.
   * The relationships are preserved within these nodes
   */
  Graph getSubGraph(String key)
  {
    return getSubGraphWithNodes(getNodes(key))
  }

  /**
   * Return a subgraph with only the nodes that contain the given property
   * with the given value.
   * The relationships are preserved within these nodes
   */
  Graph getSubGraph(String key, String value)
  {
    return getSubGraphWithNodes(getNodes(key, value))
  }

  /**
   * Return a subgraph made out of the nodes that satisfy the closure
   *
   * TODO LOW RF: should we rename getSubGraph into grep?
   */
  Graph getSubGraph(Closure cl)
  {
    def nodes = []
    _nodes.each {
      if (cl(it))
      {
        nodes << it
      }
    }
    return getSubGraphWithNodes(nodes)
  }

  /**
   * return a subgraph of this graph that includes the given nodes
   * and their dependencies, if the includeDeps flag is set to <code>true</code> 
   */
  Graph getSubGraph(Collection<GraphNode> nodes, boolean includeDeps)
  {
    Set res = new HashSet()
    nodes.each {
      res << it
      if (includeDeps)
      {
        _inboundConnections[it]?.each { dep ->
          res << dep
        }
      }
    }
    return getSubGraphWithNodes(res)
  }

  private Graph getSubGraphWithNodes(Collection<GraphNode> nodes)
  {
    Graph res = new Graph()
    nodes.each { GraphNode node ->
      def inbound = _inboundConnections[node]?.intersect(nodes)
      def outbound = _outboundConnections[node]?.intersect(nodes)
      res.addNode(node, inbound, outbound)
    }
    return res
  }

  /**
   * Return all the property keys found in the nodes of the graph
   */
  Set<String> getPropertyKeys()
  {
    def res = new HashSet()
    _nodes.each { GraphNode node ->
      res += node.props.keySet()
    }
    return res
  }

  /**
   * Return the set of values found, for the given property, in the graph
   */
  Set<String> getPropertyValues(String key)
  {
    def res = new HashSet()
    _nodes.each { GraphNode node ->
      if (node.props.containsKey(key))
      {
        res << node.props[key]
      }
    }
    return res  
  }

  /**
   * Return the collection of nodes that have no inbound connections
   */
  Set<GraphNode<T>> getSeeds()
  {
    return _inboundConnections.findAll {k, v-> _inboundConnections[k].size() == 0}.keySet().toList()
  }

  /**
   * Returns the nodes organized by depth in the graph.
   * Depth is defined as follows:
   * 0   for seeds
   * n+1 for non-seeds, where n is the highest depth among the nodes it depends on
   */
  List<List<GraphNode<T>>> getNodesByDepth()
  {
    def depths = [:]
    
    new DepFirstVisitor(this).accept { GraphNode node ->
      def depth = 0
      if (this.getInboundNodes(node))
      {
        this.getInboundNodes(node).each {
          if (depths[it] > depth) depth = depths[it]
        }
        depth++
      }
      depths[node] = depth
    }

    def res = []
    depths.each {k, v ->
      if (!res[v]) res[v] = []
      res[v] << k
    }
    return res
  }

  /**
   * Return the nodes by depth and, within each depth,
   * order the nodes according to the closure
   * @see #getNodesByDepth()
   */
  List<List<GraphNode<T>>> getNodesByDepth(Closure cl)
  {
    def nodes = nodesByDepth
    // now order them
    nodes.each { List nodesAtDepth ->
      nodesAtDepth.sort(cl)
    }
    return nodes
  }

  /**
   * Return the outbound nodes (the "to nodes") for the given node.
   * Return an empty array if the node cannot be found in the graph
   */
  Set<GraphNode<T>> getOutboundNodes(GraphNode node)
  {
    return Collections.unmodifiableSet(_outboundConnections[node] ?: Collections.emptySet())
  }

  /**
   * Return the inbound nodes (the "from nodes") for the given node.
   * Return an empty array if the node cannot be found in the graph
   */
  Set<GraphNode<T>> getInboundNodes(GraphNode node)
  {
    return Collections.unmodifiableSet(_inboundConnections[node] ?: Collections.emptySet())
  }


  boolean equals(o)
  {
    if (this.is(o)) return true;

    if (!o || getClass() != o.class) return false;

    Graph graph = (Graph) o;

    if (_nodes != graph._nodes) return false;
    if (_inboundConnections != graph._inboundConnections) return false;
    if (_outboundConnections != graph._outboundConnections) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = _nodes.hashCode() ?: 0;
    result = 31 * result + (_inboundConnections.hashCode() ?: 0);
    result = 31 * result + (_outboundConnections.hashCode() ?: 0);
    return result;
  }
}