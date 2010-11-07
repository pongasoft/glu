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

package org.linkedin.glu.provisioner.core.graph

/**
 * A graph of objects
 *
 * author:  Riccardo Ferretti
 * created: Sep 15, 2009
 */
public class DepFirstVisitor <T extends Object> {

  private final Graph _graph

  def DepFirstVisitor(Graph graph)
  {
    _graph = graph
  }

  def accept(Closure cl)
  {
    // get all the nodes that have no dependencies
    process(_graph.seeds, cl)
  }

  private void process(Collection<GraphNode> seeds, Closure cl)
  {
    Set visited = new HashSet()

    Queue queue = new LinkedList()

    seeds.each { GraphNode node ->
      queue.offer(node)
    }

    while (!queue.isEmpty())
    {
      GraphNode node = queue.remove()

      if (!visited.contains(node))
      {
        boolean ready = true
        _graph.getInboundNodes(node).each {
          if (!visited.contains(it)) ready = false
        }
        if (ready)
        {
          // if all the dependencies have been visited, execute it
          node.accept (cl)
          _graph.getOutboundNodes(node).each { queue.offer(it) }
          visited << node
        }
        else
        {
          // otherwise, put it on top of the queue
          queue.offer(node)
        }
      }
    }
  }

}