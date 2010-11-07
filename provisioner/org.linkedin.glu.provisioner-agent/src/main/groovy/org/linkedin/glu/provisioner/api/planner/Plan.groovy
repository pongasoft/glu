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

package org.linkedin.glu.provisioner.api.planner

import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.core.graph.Graph
import org.linkedin.glu.provisioner.core.graph.GraphNode

/**
 * An action plan
 */
public class Plan
{
  /**
   * The main phases, in the order of execution
   * NOTE RF: the reason why the phases are in this order is because this will be
   * the order of execution. We want to first stop, unconfigure and uninstall the old installations
   * and the bring up the new evironment
   */
  public final static List<String> PHASES =
      Collections.unmodifiableList(['noop', 'stop', 'unconfigure', 'uninstall', 'uninstallscript', 'installscript', 'install', 'configure', 'start'])

  /**
   * The name of the property that contains the phase
   */
  public static final String PHASE_PN = 'phase'

  /**
   * The name of the property that contains the host
   */
  public static final String HOST_PN = 'host'

  /**
   * The name of the property that contains the installation name
   */
  public static final String NAME_PN = 'name'

  private final Graph<ActionDescriptor> _graph

  def Plan(Graph<ActionDescriptor> graph)
  {
    _graph = graph
  }

  /**
   * Return the list of phases that are present in the plan.
   * The list is ordered by execution
   */
  List<String> getPhases()
  {
    _graph.getPropertyValues(PHASE_PN).toList().sort {a, b -> PHASES.indexOf(a) <=> PHASES.indexOf(b)}
  }

  /**
   * Return the list of hosts affected by the plan.
   * The list is ordered alphabetically
   */
  List<String> getHosts()
  {
    _graph.getPropertyValues(HOST_PN).toList().sort {a, b -> a <=> b}
  }

  /**
   * Return the list of action descriptors ordered by:
   * <ul>
   * <li>phase</li>
   * <li>host</li>
   * <li>name</li>
   * </ul>
   */
  List<ActionDescriptor> getActionNodes()
  {
    def res = []
    phases.each { phase ->
      res.addAll (_graph.getNodes(PHASE_PN, phase).toList().sort { GraphNode a, GraphNode b ->
                   a[HOST_PN] == b[HOST_PN] ? a[NAME_PN] <=> b[NAME_PN] : a[HOST_PN] <=> b[HOST_PN]
                 }.value)
    }
    return res
  }
  
  /**
   * Return the graph of actions on which the plan is based
   */
  Graph<ActionDescriptor> getGraph()
  {
    return _graph
  }
}