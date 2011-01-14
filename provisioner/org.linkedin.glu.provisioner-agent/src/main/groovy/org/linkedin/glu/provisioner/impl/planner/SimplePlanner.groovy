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

package org.linkedin.glu.provisioner.impl.planner

import org.linkedin.glu.provisioner.api.planner.Plan
import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.core.action.IActionDescriptorFactory
import org.linkedin.glu.provisioner.core.environment.Environment
import org.linkedin.glu.provisioner.core.environment.Installation
import org.linkedin.glu.provisioner.core.graph.Graph
import org.linkedin.glu.provisioner.core.graph.GraphNode
import org.linkedin.glu.provisioner.impl.agent.AgentActionDescriptorFactory

import java.util.concurrent.atomic.AtomicInteger
import org.linkedin.glu.provisioner.core.action.IDescriptionProvider

/**
 * The simple planner computes the delta in the environments and creates the
 * action descriptors to bridge the gap
 *
 * author:  Riccardo Ferretti
 * created: Jul 24, 2009
 */
public class SimplePlanner extends AbstractPlanner
{
  public static final String MODULE = SimplePlanner.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  InstallationTransitions installationTransitions = new InstallationTransitions()

  def SimplePlanner()
  {
    super()
  }

  def SimplePlanner(Map<String, IActionDescriptorFactory> factories)
  {
    super(factories)
  }

  def SimplePlanner(List<IActionDescriptorFactory> factories)
  {
    super(factories)
  }

  void setFieldExcludes(excludes)
  {
    installationTransitions.fieldExcludes = excludes
  }

  void setPropsExcludes(excludes)
  {
    installationTransitions.propsExcludes = excludes
  }

  public Plan createPlan(Environment from, Environment to)
  {
    return createPlan(from, to, null);
  }

  Plan createPlan(Environment from, Environment to, IDescriptionProvider descriptionProvider)
  {
    IActionDescriptorFactory factory = _factories[AgentActionDescriptorFactory.ID]

    // get all the ids from the source and target environments
    def ids = from.installations.id + to.installations.id
    ids.unique()

    def graph = new Graph<ActionDescriptor>()
    // the counter doesn't need to be thread safe, but needs to be wrapped in an object.
    // we are using the atomic integer instead of creating a wrapper
    def counter = new AtomicInteger(0)
    ids.each { id ->
      // TODO MED RF: what if to.installationsById[id].parent is not null?!?! do we want to allow for that case?
      if ((from.installationsById[id] && from.installationsById[id].parent == null) ||
          (to.installationsById[id] && to.installationsById[id].parent == null))
      {
        addActions(counter,
                   graph,
                   factory,
                   descriptionProvider,
                   from.installationsById[id],
                   to.installationsById[id],
                   null)
      }
    }

    return new Plan(graph)
  }

  /**
   * Creates the actions descriptors to go from the first installation to the second one
   */
  private Map<String, Set<GraphNode<ActionDescriptor>>> addActions(AtomicInteger count, Graph graph,
                                                                   IActionDescriptorFactory factory,
                                                                   IDescriptionProvider descriptionProvider,
                                                                   Installation first, Installation second,
                                                                   String throughState)
  {
    // this will keep a map "phase" -> "list of action nodes for that phase from children"
    def childrenNodes = [:]
    Plan.PHASES.each { phase ->
      childrenNodes[phase] = []
    }

    String instThroughState = installationTransitions.getThroughState(first, second)
    String actualThroughState = installationTransitions.getThroughState([throughState, instThroughState])

    // find the transitions for this installation
    if(first?.transitionState)
    {
      // GLU-235: ignoring installations which are in transitionState
      // TOOLS-1357: we don't ignore them anymore: we generate a noop step
      def phase = 'noop'
      ActionDescriptor descriptor = factory.getActionDescriptor(phase, first, descriptionProvider)
      descriptor.id = count.addAndGet(1)
      def tags = [:]
      tags[Plan.PHASE_PN] = phase
      tags[Plan.HOST_PN] = first.hostname
      tags[Plan.NAME_PN] = first.name
      def adNode = new GraphNode(descriptor, tags)
      graph.addNode(adNode, [], [])
      return [(phase): [adNode]]
    }

    List transitions = installationTransitions.findShortestPath(first, second, actualThroughState)

    // check if the actions for this installation affect the through state
    def childrenThroughState = actualThroughState
    if (installationTransitions.getReinstallState() != actualThroughState)
    {
      List states = transitions.collect {installationTransitions.stateMachine.getEndState(it.name)} + [actualThroughState]
      childrenThroughState = installationTransitions.getThroughState(states)
    }

    // look at the children to see what to do
    def deltas = [:]
    first?.children.each { child ->
      deltas[child.id] = new InstallationsDelta(first: child, second: null)
    }
    second?.children.each { child ->
      // not the most readeable code, but basically if there is already an entry in the
      // map, we use it to compute the delta
      def old = deltas[child.id]?.first
      def delta = new InstallationsDelta(first: old, second: child)
      deltas[child.id] = delta
    }
    // add the actions descr for the children
    deltas.each { id, InstallationsDelta delta ->
      addActions(count,
                 graph,
                 factory,
                 descriptionProvider,
                 delta.first,
                 delta.second, 
                 childrenThroughState).each { phase, nodes ->
        childrenNodes[phase] += nodes
      }
    }

    def res = [:]
    def transitionsByName = [:]
    def lastADNode = null
    transitions.each { transitionsByName[it.name] = it }

    Plan.PHASES.each { phase ->
      if (transitionsByName.keySet().contains(phase))
      {
        res[phase] = []
        Installation inst = transitionsByName[phase].target
        ActionDescriptor descriptor = factory.getActionDescriptor(phase, inst, descriptionProvider)
        descriptor.id = count.addAndGet(1)
        // we create the tags map here, instead of doing it inline, because of the problem
        // with GString/String when using them as keys in maps
        def tags = [:]
        tags[Plan.PHASE_PN] = phase
        tags[Plan.HOST_PN] = inst.hostname
        tags[Plan.NAME_PN] = inst.name
        def adNode = new GraphNode(descriptor, tags)

        res[phase] << adNode

        def toNodes = []
        def fromNodes = []
        if (installationTransitions.destructiveTransitions.contains(phase))
        {
          fromNodes = childrenNodes[phase]
        }
        else
        {
          toNodes = childrenNodes[phase]
        }
        // if there is an action descr for this installation from a previous phase, we add it to the dependencies
        if (lastADNode) fromNodes << lastADNode
        // finally add the node to the graph!
        graph.addNode(adNode, fromNodes, toNodes)
        // this action descr will become the last action descr for the next phase, if any
        lastADNode = adNode
      }
      else
      {
        res[phase] = childrenNodes[phase]
      }
    }

    return res
  }

}