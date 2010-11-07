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

package test.provisioner.impl.planner

import org.linkedin.glu.provisioner.core.environment.Environment
import org.linkedin.glu.provisioner.core.environment.Installation

import org.linkedin.glu.provisioner.core.graph.Graph
import org.linkedin.glu.provisioner.core.graph.GraphNode
import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.api.planner.Plan
import org.linkedin.glu.provisioner.impl.planner.SimplePlanner
import org.linkedin.glu.provisioner.impl.planner.InstallationTransitions
import org.linkedin.groovy.util.state.StateMachineImpl

/**
 * Tests for the planner
 *
 * author:  Riccardo Ferretti
 * created: Jul 28, 2009
 */
public class TestPlanner extends GroovyTestCase
{

  /**
   * Test that the planner is returning the expected phases
   */
  void testPhases()
  {
    // start from an empty environment
    Environment from = new Environment(name: 'env', installations: [])

    // add two installations
    Environment to = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null)
                                     ])
    SimplePlanner planner = getPlanner()

    Plan plan = planner.createPlan(from, to)
    Graph graph = plan.graph

    assertEquals(8, graph.nodes.size())
    assertEquals(new HashSet(['installscript','install','configure','start']),
                 graph.getPropertyValues('phase'))
    // in the plan, the phases are ordered
    assertEquals(['installscript','install','configure','start'], plan.phases)
  }

  void testSameEnvironment()
  {
    Environment from = new Environment(name: 'old', installations: [
                                       new Installation(hostname: 'localhost', mount: '/webtrack/i001',
                                          name: 'webtrack', gluScript: 'ivy:/com.linkedin.webtrack/webtrack/1.4',
                                          props: ['skeleton': 'ivy:/com.linkedin.containers/jetty-container/1.2',
                                                  'services':'webtrack',
                                                  'serviceArtifacts':['webtrack': 'ivy:/com.linkedin.webtrack/webtrack/0.0.486-RC1.2022'],
                                                  'glu.installation.name':'webtrack'],
                                          parent: null),
                                       ])
    Environment to = new Environment(name: 'new', installations: [
                                       new Installation(hostname: 'localhost', mount: '/webtrack/i001',
                                          name: 'webtrack', gluScript: 'ivy:/com.linkedin.webtrack/webtrack/1.4',
                                          props: ['services':'webtrack',
                                                  'skeleton': 'ivy:/com.linkedin.containers/jetty-container/1.2',
                                                  'serviceArtifacts':['webtrack': 'ivy:/com.linkedin.webtrack/webtrack/0.0.486-RC1.2022'],
                                                  'glu.installation.name':'webtrack'],
                                          parent: null),
                                       ])
    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph

    assertEquals(0, graph.nodes.size())
  }

  /**
   * Test that we can start from an empty environment and install something
   */
  void testNewEnvironment()
  {
    // start from an empty environment
    Environment from = new Environment(name: 'env', installations: [])

    // add two installations
    Environment to = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null)
                                     ])
    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph

    def actionsByPhase =
     ['installscript': ["Install script for installation media on mes01.prod", "Install script for installation media on mes02.prod"],
      'install': ["Install media on mes01.prod", "Install media on mes02.prod"],
      'configure' : ["Configure media on mes01.prod", "Configure media on mes02.prod"],
      'start': ["Start media on mes01.prod", "Start media on mes02.prod"]]

    checkActions(actionsByPhase, graph)
  }

  /**
   * Test that we can start from an provisioned environment and uninstall everything
   */
  void testCleanEnvironment()
  {
    Environment from = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null)
                                     ])

    Environment to = new Environment(name: 'env', installations: [])

    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph

    def actionsByPhase =
     ['uninstallscript': ["Uninstall script for installation media on mes01.prod", "Uninstall script for installation media on mes02.prod"],
      'uninstall': ["Uninstall media from mes01.prod", "Uninstall media from mes02.prod"],
      'unconfigure': ["Unconfigure media on mes02.prod", "Unconfigure media on mes02.prod"],
      'stop': ["Stop media on mes01.prod", "Stop media on mes02.prod"]]

    checkActions(actionsByPhase, graph)
  }

  /**
   * Test that we can add installations to an existing environment
   */
  void testAddingInstallations()
  {
    // start from two installations
    Environment from = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null)
                                     ])

    // add two installations
    Environment to = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes03.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mupld.prod', mount: '/mupld',
                                          name: 'mupld', gluScript: 'ivy:/com.linkedin.mupld/mupld-frontend/1.2',
                                          props: [:], parent: null)
                                     ])
    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph
    def actionsByPhase =
     ['installscript': ["Install script for installation media on mes03.prod", "Install script for installation mupld on mupld.prod"],
      'install': ["Install media on mes03.prod", "Install mupld on mupld.prod"],
      'configure' : ["Configure media on mes03.prod", "Configure mupld on mupld.prod"],
      'start': ["Start media on mes03.prod", "Start mupld on mupld.prod"]]

    checkActions(actionsByPhase, graph)
  }

  /**
   * Test that the same installation can be properly transitioned across states
   */
  void testChangingState()
  {
    Environment from = new Environment(name: 'env', installations: [
                                 new Installation(hostname: 'mes01.prod', mount: '/media',
                                      name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                      props: [:], state: 'stopped' ,parent: null),
                                 new Installation(hostname: 'mes02.prod', mount: '/media',
                                      name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                      props: [:], state: 'running', parent: null)
                                 ])

    Environment to = new Environment(name: 'env', installations: [
                                 new Installation(hostname: 'mes01.prod', mount: '/media',
                                      name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                      props: [:], state: 'running', parent: null),
                                 new Installation(hostname: 'mes02.prod', mount: '/media',
                                      name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                      props: [:], state: 'stopped', parent: null)
                                 ])

    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph
    def actionsByPhase =
     ['start': ["Start media on mes01.prod"],
      'stop': ["Stop media on mes02.prod"]]

    checkActions(actionsByPhase, graph)
  }

  /**
   * Test that installation in transition generates a noop (TOOLS-1357)
   */
  void testTransitionState()
  {
    Environment from = new Environment(name: 'env', installations: [
                                 new Installation(hostname: 'mes01.prod', mount: '/media',
                                      name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                      props: [:], state: 'stopped' , transitionState: 'stopped->running', parent: null),
                                 new Installation(hostname: 'mes02.prod', mount: '/media',
                                      name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                      props: [:], state: 'running', parent: null)
                                 ])

    Environment to = new Environment(name: 'env', installations: [
                                 new Installation(hostname: 'mes01.prod', mount: '/media',
                                      name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                      props: [:], state: 'running', parent: null),
                                 new Installation(hostname: 'mes02.prod', mount: '/media',
                                      name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                      props: [:], state: 'stopped', parent: null)
                                 ])

    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph
    def actionsByPhase =
     ['noop': ["Currently in transition [stopped->running] => noop"],
      'stop': ["Stop media on mes02.prod"]]

    checkActions(actionsByPhase, graph)
  }

  /**
   * Test that we can remove installations from an existing environment
   */
  void testRemovingInstallations()
  {
    Environment from = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null)
                                     ])

    // the new environemnt doesn't have media on mes02
    Environment to = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null)
                                     ])
    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph
    def actionsByPhase =
     ['uninstallscript': ["Uninstall script for installation media on mes02.prod"],
      'uninstall': ["Uninstall media from mes02.prod"],
      'unconfigure': ["Unconfigure media on mes02.prod"],
      'stop': ["Stop media on mes02.prod"]]

    checkActions(actionsByPhase, graph)
  }

  /**
   * Test that we can update properties in installations in the environment
   */
  void testUpdateProperties()
  {
    Environment from = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null)
                                     ])

    // the environment is the same, but some properties on media for mes01 are different
    Environment to = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: ['mykey':'myvalue'], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null)
                                     ])
    SimplePlanner planner = getPlanner()
    
    Graph graph = planner.createPlan(from, to).graph

    def actionsByPhase =
     ['stop': ["Stop media on mes01.prod"],
      'unconfigure' : ["Unconfigure media on mes01.prod"],
      'configure' : ["Configure media on mes01.prod"],
      'start': ["Start media on mes01.prod"]]

    checkActions(actionsByPhase, graph)
  }

  /**
   * Test the relationship between the parent installation and its children
   * This describes the case where there are changes in the children and test that
   * the parent is not affected by them
   */
  void testParentInstallation_changeChildren()
  {
    Installation mediaContainer = new Installation(hostname: 'mes01.prod', mount: '/media-container',
                                          name: 'media-container', gluScript: 'ivy:/com.linkedin.jetty/jetty-container/1.0',
                                          props: [:], parent: null)
    Installation mediaBackend = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-backend',
                                          name: 'media-backend', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: mediaContainer)
    Installation mediaAdmin = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-admin',
                                          name: 'media-admin', gluScript: 'ivy:/com.linkedin.media/media-admin/3.4',
                                          props: [:], parent: mediaContainer)

    Environment from = new Environment(name: 'env', installations: [
                                     mediaContainer, mediaBackend, mediaAdmin])


    // this one is the same
    Installation newMediaContainer = new Installation(hostname: 'mes01.prod', mount: '/media-container',
                                          name: 'media-container', gluScript: 'ivy:/com.linkedin.jetty/jetty-container/1.0',
                                          props: [:], parent: null)
    // changing the script for media-backend
    Installation newMediaBackend = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-backend',
                                          name: 'media-backend', gluScript: 'ivy:/com.linkedin.media/media-backend/3.5',
                                          props: [:], parent: newMediaContainer)
    // change some properties in media admin
    Installation newMediaAdmin = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-admin',
                                          name: 'media-admin', gluScript: 'ivy:/com.linkedin.media/media-admin/3.4',
                                          props: ['mykey':'myvalue'], parent: newMediaContainer)

    Environment to = new Environment(name: 'env', installations: [
                                     newMediaContainer, newMediaBackend, newMediaAdmin])
    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph

    def actionsByPhase =
     ['stop': ["Stop media-admin on mes01.prod", "Stop media-backend on mes01.prod"],
      'unconfigure': ["Unconfigure media-backend on mes01.prod","Unconfigure media-admin on mes01.prod"],
      'uninstall': ["Uninstall media-backend from mes01.prod"],
      'uninstallscript': ["Uninstall script for installation media-backend on mes01.prod"],
      'installscript': ["Install script for installation media-backend on mes01.prod"],
      'install' : ["Install media-backend on mes01.prod"],
      'configure' : ["Configure media-admin on mes01.prod", "Configure media-backend on mes01.prod"],
      'start': ["Start media-admin on mes01.prod", "Start media-backend on mes01.prod"]]

    checkActions(actionsByPhase, graph)
  }

  /**
   * Test the relationship between the parent installation and its children.
   * This describes the case where the parent's properties have changed and need to be
   * updated
   */
  void testParentInstallation_updateParent()
  {
    Installation mediaContainer = new Installation(hostname: 'mes01.prod', mount: '/media-container',
                                          name: 'media-container', gluScript: 'ivy:/com.linkedin.jetty/jetty-container/1.0',
                                          props: [:], parent: null)
    Installation mediaBackend = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-backend',
                                          name: 'media-backend', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: mediaContainer)
    Installation mediaAdmin = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-admin',
                                          name: 'media-admin', gluScript: 'ivy:/com.linkedin.media/media-admin/3.4',
                                          props: [:], parent: mediaContainer)

    Environment from = new Environment(name: 'env', installations: [
                                     mediaContainer, mediaBackend, mediaAdmin])


    // changing some properties for the container
    Installation newMediaContainer = new Installation(hostname: 'mes01.prod', mount: '/media-container',
                                          name: 'media-container', gluScript: 'ivy:/com.linkedin.jetty/jetty-container/1.0',
                                          props: ['mykey':'myvalue'], parent: null)

    // the only difference is that this installation is pointing to the new container
    Installation newMediaBackend = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-backend',
                                          name: 'media-backend', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: newMediaContainer)
    // the only difference is that this installation is pointing to the new container
    Installation newMediaAdmin = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-admin',
                                          name: 'media-admin', gluScript: 'ivy:/com.linkedin.media/media-admin/3.4',
                                          props: [:], parent: newMediaContainer)

    Environment to = new Environment(name: 'env', installations: [
                                     newMediaContainer, newMediaBackend, newMediaAdmin])
    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph

    def actionsByPhase =
     ['stop': ["Stop media-container on mes01.prod", "Stop media-admin on mes01.prod", "Stop media-backend on mes01.prod"],
      'unconfigure' : ["Unconfigure media-container on mes01.prod", "Unconfigure media-admin on mes01.prod", "Unconfigure media-backend on mes01.prod"],
      'configure' : ["Configure media-container on mes01.prod", "Configure media-admin on mes01.prod", "Configure media-backend on mes01.prod"],
      'start': ["Start media-container on mes01.prod", "Start media-admin on mes01.prod", "Start media-backend on mes01.prod"]]

    checkActions(actionsByPhase, graph)
  }

  /**
   * Test the relationship between the parent installation and its children.
   * In particular we want to make sure that the operations on the parent are executed
   * before the actions on the children
   */
  void testParentInstallation_actionDependencies()
  {
    Installation mediaContainer = new Installation(hostname: 'mes01.prod', mount: '/media-container',
                                          name: 'media-container', gluScript: 'ivy:/com.linkedin.jetty/jetty-container/1.0',
                                          props: [:], parent: null)
    Installation mediaBackend = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-backend',
                                          name: 'media-backend', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: mediaContainer)
    Installation mediaAdmin = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-admin',
                                          name: 'media-admin', gluScript: 'ivy:/com.linkedin.media/media-admin/3.4',
                                          props: [:], parent: mediaContainer)

    Environment from = new Environment(name: 'env', installations: [
                                     mediaContainer, mediaBackend, mediaAdmin])


    // changing some properties for the container
    Installation newMediaContainer = new Installation(hostname: 'mes01.prod', mount: '/media-container',
                                          name: 'media-container', gluScript: 'ivy:/com.linkedin.jetty/jetty-container/1.0',
                                          props: ['mykey':'myvalue'], parent: null)

    // the only difference is that this installation is pointing to the new container
    Installation newMediaBackend = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-backend',
                                          name: 'media-backend', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: newMediaContainer)
    // the only difference is that this installation is pointing to the new container
    Installation newMediaAdmin = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-admin',
                                          name: 'media-admin', gluScript: 'ivy:/com.linkedin.media/media-admin/3.4',
                                          props: [:], parent: newMediaContainer)

    Environment to = new Environment(name: 'env', installations: [
                                     newMediaContainer, newMediaBackend, newMediaAdmin])
    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph

    // we do our ordinary test
    def actionsByPhase =
     ['stop': ["Stop media-container on mes01.prod", "Stop media-admin on mes01.prod", "Stop media-backend on mes01.prod"],
      'unconfigure' : ["Unconfigure media-container on mes01.prod", "Unconfigure media-admin on mes01.prod", "Unconfigure media-backend on mes01.prod"],
      'configure' : ["Configure media-container on mes01.prod", "Configure media-admin on mes01.prod", "Configure media-backend on mes01.prod"],
      'start': ["Start media-container on mes01.prod", "Start media-admin on mes01.prod", "Start media-backend on mes01.prod"]]

    checkActions(actionsByPhase, graph)

    def seeds
    def subgraph

    // check that we stop the container _after_ we stop the services

    // stop
    subgraph = graph.getSubGraph('phase', 'stop')
    seeds = subgraph.seeds.toArray()
    assertEquals(2, seeds.size())
    assertEquals(new HashSet(["Stop media-admin on mes01.prod", "Stop media-backend on mes01.prod"]),
                 new HashSet(seeds.value.description))
    // the subgraph doesn't contain the dependency on the other phase..
    assertEquals(["Stop media-container on mes01.prod"], subgraph.getOutboundNodes(seeds[0]).value.description)
    assertEquals(["Stop media-container on mes01.prod"], subgraph.getOutboundNodes(seeds[1]).value.description)
    // ..but the graph still does
    assertEquals(new HashSet(['stop', 'unconfigure']),
                 new HashSet(graph.getOutboundNodes(seeds[0]).value.actionName))
    assertEquals(new HashSet(['stop','unconfigure']),
                 new HashSet(graph.getOutboundNodes(seeds[1]).value.actionName))

    // unconfigure
    subgraph = graph.getSubGraph('phase', 'unconfigure')
    seeds = subgraph.seeds.toArray()
    assertEquals(2, seeds.size())
    assertEquals(new HashSet(["Unconfigure media-admin on mes01.prod", "Unconfigure media-backend on mes01.prod"]),
                 new HashSet(seeds.value.description))
    // the subgraph doesn't contain the dependency on the other phase..
    assertEquals(["Unconfigure media-container on mes01.prod"], subgraph.getOutboundNodes(seeds[0]).value.description)
    assertEquals(["Unconfigure media-container on mes01.prod"], subgraph.getOutboundNodes(seeds[1]).value.description)
    // ..but the graph still does
    assertEquals(new HashSet(['unconfigure', 'configure']),
                 new HashSet(graph.getOutboundNodes(seeds[0]).value.actionName))
    assertEquals(new HashSet(['unconfigure','configure']),
                 new HashSet(graph.getOutboundNodes(seeds[1]).value.actionName))

    // configure
    subgraph = graph.getSubGraph('phase', 'configure')
    seeds = subgraph.seeds.toArray()
    assertEquals(1, seeds.size())
    assertEquals(new HashSet(["Configure media-container on mes01.prod"]), new HashSet(seeds.value.description))
    // the subgraph doesn't contain the dependency on the other phase..
    assertEquals(new HashSet(["Configure media-admin on mes01.prod", "Configure media-backend on mes01.prod"]),
                 new HashSet(subgraph.getOutboundNodes(seeds[0]).value.description))
    // ..but the graph still does
    assertEquals(new HashSet(["Configure media-admin on mes01.prod", "Configure media-backend on mes01.prod", "Start media-container on mes01.prod"]),
                 new HashSet(graph.getOutboundNodes(seeds[0]).value.description))

    // start
    subgraph = graph.getSubGraph('phase', 'start')
    seeds = subgraph.seeds.toArray()
    assertEquals(1, seeds.size())
    assertEquals(new HashSet(["Start media-container on mes01.prod"]),
                 new HashSet(seeds.value.description))
    // the subgraph doesn't contain the dependency on the other phase..
    assertEquals(new HashSet(["Start media-admin on mes01.prod", "Start media-backend on mes01.prod"]),
                 new HashSet(subgraph.getOutboundNodes(seeds[0]).value.description))
    // ..but the graph still does (but in this case there is none!)
    assertEquals(new HashSet(["Start media-admin on mes01.prod", "Start media-backend on mes01.prod"]),
                 new HashSet(graph.getOutboundNodes(seeds[0]).value.description))
  }

 /**
  * Test the relationship between the parent installation and its children.
  * This describes the case where the parent needs to be repushed
  */
  void testParentInstallation_repushParent()
  {
    Installation mediaContainer = new Installation(hostname: 'mes01.prod', mount: '/media-container',
                                          name: 'media-container', gluScript: 'ivy:/com.linkedin.jetty/jetty-container/1.0',
                                          props: [:], parent: null)
    Installation mediaBackend = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-backend',
                                          name: 'media-backend', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: mediaContainer)
    Installation mediaAdmin = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-admin',
                                          name: 'media-admin', gluScript: 'ivy:/com.linkedin.media/media-admin/3.4',
                                          props: [:], parent: mediaContainer)

    Environment from = new Environment(name: 'env', installations: [
                                     mediaContainer, mediaBackend, mediaAdmin])


    // changing some properties for the container
    Installation newMediaContainer = new Installation(hostname: 'mes01.prod', mount: '/media-container',
                                          name: 'media-container', gluScript: 'ivy:/com.linkedin.jetty/jetty-container/1.3',
                                          props: [:], parent: null)

    // the only difference is that this installation is pointing to the new container
    Installation newMediaBackend = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-backend',
                                          name: 'media-backend', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: newMediaContainer)
    // the only difference is that this installation is pointing to the new container
    Installation newMediaAdmin = new Installation(hostname: 'mes01.prod', mount: '/media-container/media-admin',
                                          name: 'media-admin', gluScript: 'ivy:/com.linkedin.media/media-admin/3.4',
                                          props: [:], parent: newMediaContainer)

    Environment to = new Environment(name: 'env', installations: [
                                     newMediaContainer, newMediaBackend, newMediaAdmin])
    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph

    // even though we only changed the parent, we expect also the media-admin and media-backend
    // to be repushed
    def actionsByPhase =
    ['stop': ["Stop media-container on mes01.prod", "Stop media-admin on mes01.prod", "Stop media-backend on mes01.prod"],
     'unconfigure' : ["Unconfigure media-container on mes01.prod", "Unconfigure media-admin on mes01.prod", "Unconfigure media-backend on mes01.prod"],            
     'uninstall': ["Uninstall media-container from mes01.prod", "Uninstall media-admin from mes01.prod", "Uninstall media-backend from mes01.prod"],
     'uninstallscript': ["Uninstall script for installation media-container on mes01.prod", "Uninstall script for installation media-admin on mes01.prod", "Uninstall script for installation media-backend on mes01.prod"],
     'installscript': ["Install script for installation media-container on mes01.prod", "Install script for installation media-admin on mes01.prod", "Install script for installation media-backend on mes01.prod"],
     'install' : ["Install media-container on mes01.prod", "Install media-admin on mes01.prod", "Install media-backend on mes01.prod"],
     'configure' : ["Configure media-container on mes01.prod", "Configure media-admin on mes01.prod", "Configure media-backend on mes01.prod"],
     'start': ["Start media-container on mes01.prod", "Start media-admin on mes01.prod", "Start media-backend on mes01.prod"]]

    checkActions(actionsByPhase, graph)
  }

  /**
   * Test that we can add and remove installations at the same time
   */
  void testAddingAndRemovingInstallations()
  {
    Environment from = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null)
                                     ])

    // the new environment add and removes some installations
    Environment to = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes03.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mupld.prod', mount: '/mupld',
                                          name: 'mupld', gluScript: 'ivy:/com.linkedin.mupld/mupld-frontend/1.2',
                                          props: [:], parent: null)
                                     ])
    SimplePlanner planner = getPlanner()

    Graph graph = planner.createPlan(from, to).graph

    def actionsByPhase =
     ['stop': ["Stop media on mes01.prod"],
      'unconfigure': ["Unconfigure media on mes01.prod"],
      'uninstall': ["Uninstall media from mes01.prod"],
      'uninstallscript': ["Uninstall script for installation media on mes01.prod"],
      'installscript': ["Install script for installation media on mes03.prod", "Install script for installation mupld on mupld.prod"],
      'install' : ["Install media on mes03.prod", "Install mupld on mupld.prod"],
      'configure' : ["Configure media on mes03.prod", "Configure mupld on mupld.prod"],
      'start': ["Start mupld on mupld.prod", "Start mupld on mupld.prod"]]

    checkActions(actionsByPhase, graph)
  }

  /**
   * Test a fairly complex changeset in the environment
   */
  void testHappyPath()
  {
    Environment from = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes03.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mupld.prod', mount: '/mupld',
                                          name: 'mupld', gluScript: 'ivy:/com.linkedin.mupld/mupld-frontend/1.2',
                                          props: [:], parent: null)
                                     ])

    // the new environment
    Environment to = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     // changing some properties in media
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: ['mykey':'myvalue'], parent: null),
                                     // change version of mupld script
                                     new Installation(hostname: 'mupld.prod', mount: '/mupld',
                                          name: 'mupld', gluScript: 'ivy:/com.linkedin.mupld/mupld-frontend/1.3',
                                          props: [:], parent: null),
                                     // installing mpr... on mes03.prod
                                     new Installation(hostname: 'mes03.prod', mount: '/mpr',
                                          name: 'mpr', gluScript: 'ivy:/com.linkedin.mpr/mpr-frontend/1.0',
                                          props: [:], parent: null),
                                     // installing captcha
                                     new Installation(hostname: 'captcha.prod', mount: '/captcha',
                                          name: 'captcha', gluScript: 'ivy:/com.linkedin.captcha/captcha-backend/1.0',
                                          props: [:], parent: null)
                                     ])
    SimplePlanner planner = getPlanner()
    Plan plan = planner.createPlan(from, to)
    Graph graph = plan.graph

    def actionsByPhase =
     ['stop': ["Stop media on mes02.prod", "Stop media on mes03.prod", "Stop mupld on mupld.prod"],
      'unconfigure': ["Unconfigure media on mes02.prod", "Unconfigure media on mes03.prod", "Unconfigure mupld on mupld.prod"],
      'uninstall': ["Uninstall media from mes03.prod", "Uninstall mupld from mupld.prod"],
      'uninstallscript': ["Uninstall script for installation media on mes03.prod", "Uninstall script for installation mupld on mupld.prod"],
      'installscript': ["Install script for installation mupld on mupld.prod", "Install script for installation mpr on mes03.prod", "Install script for installation captcha on captcha.prod"],
      'install' : ["Install mupld on mupld.prod", "Install mpr on mes03.prod", "Install captcha on captcha.prod"],
      'configure' : ["Configure media on mes02.prod", "Configure mupld on mupld.prod", "Configure mpr on mes03.prod", "Configure captcha on captcha.prod"],
      'start': ["Start media on mes02.prod", "Start mupld on mupld.prod", "Start mpr on mes03.prod", "Start captcha on captcha.prod"]]

    checkActions(actionsByPhase, graph)

    // check each descriptor has a different id
    assertEquals(plan.actionNodes.id.unique().size(), graph.size())
  }

  /**
   * Test that we detect inconsistencies in the environment
   */
  void testInconsistenEnvironment()
  {
    Installation mediaContainer = new Installation(hostname: 'mes01.prod', mount: '/media-container',
                                          name: 'media-container', gluScript: 'ivy:/com.linkedin.jetty/jetty-container/1.0',
                                          props: [:], parent: null)

    Installation mediaBackend = new Installation(hostname: 'mes02.prod', mount: '/media-container/media-backend',
                                          name: 'media-backend', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: mediaContainer)

    shouldFail(IllegalStateException.class) {
      new Environment(name: 'env', installations: [mediaContainer, mediaBackend])
    }

  }

  /**
   * Test the features of the plan class
   */
  void testPlan()
  {
    Environment from = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes03.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mupld.prod', mount: '/mupld',
                                          name: 'mupld', gluScript: 'ivy:/com.linkedin.mupld/mupld-frontend/1.2',
                                          props: [:], parent: null)
                                     ])

    // the new environment
    Environment to = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     // changing some properties in media
                                     new Installation(hostname: 'mes02.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: ['mykey':'myvalue'], parent: null),
                                     // change version of mupld script
                                     new Installation(hostname: 'mupld.prod', mount: '/mupld',
                                          name: 'mupld', gluScript: 'ivy:/com.linkedin.mupld/mupld-frontend/1.3',
                                          props: [:], parent: null),
                                     // installing mpr... on mes03.prod
                                     new Installation(hostname: 'mes03.prod', mount: '/mpr',
                                          name: 'mpr', gluScript: 'ivy:/com.linkedin.mpr/mpr-frontend/1.0',
                                          props: [:], parent: null),
                                     // installing captcha
                                     new Installation(hostname: 'captcha.prod', mount: '/captcha',
                                          name: 'captcha', gluScript: 'ivy:/com.linkedin.captcha/captcha-backend/1.0',
                                          props: [:], parent: null),
                                     // installing captcha2
                                     new Installation(hostname: 'captcha.prod', mount: '/captcha2',
                                          name: 'captcha2', gluScript: 'ivy:/com.linkedin.captcha/captcha-backend/1.0',
                                          props: [:], parent: null)
                                     ])
    SimplePlanner planner = getPlanner()

    Plan plan= planner.createPlan(from, to)

    // phases are in order of execution
    assertEquals (['stop', 'unconfigure', 'uninstall', 'uninstallscript', 'installscript', 'install', 'configure', 'start'],
                  plan.phases)

    // hosts are ordered alphabetically (note how, even if mes01 is in the environment, it is not part of the plan)
    assertEquals (['captcha.prod', 'mes02.prod', 'mes03.prod', 'mupld.prod'],
                  plan.hosts)

    // The order of the actions in the plan is determined (as opposed to visiting the graph)
    assertEquals (["Stop media on mes02.prod", "Stop media on mes03.prod", "Stop mupld on mupld.prod",
                   "Unconfigure media on mes02.prod", "Unconfigure media on mes03.prod", "Unconfigure mupld on mupld.prod",
                   "Uninstall media from mes03.prod", "Uninstall mupld from mupld.prod",
                   "Uninstall script for installation media on mes03.prod", "Uninstall script for installation mupld on mupld.prod",
                   "Install script for installation captcha on captcha.prod", "Install script for installation captcha2 on captcha.prod", "Install script for installation mpr on mes03.prod", "Install script for installation mupld on mupld.prod",
                   "Install captcha on captcha.prod", "Install captcha2 on captcha.prod", "Install mpr on mes03.prod", "Install mupld on mupld.prod",
                   "Configure captcha on captcha.prod", "Configure captcha2 on captcha.prod", "Configure media on mes02.prod", "Configure mpr on mes03.prod", "Configure mupld on mupld.prod",
                   "Start captcha on captcha.prod", "Start captcha2 on captcha.prod", "Start media on mes02.prod", "Start mpr on mes03.prod", "Start mupld on mupld.prod"
                  ], plan.actionNodes.description)
  }


    /**
   * Test the features of the plan class
   */
  void testStop()
  {
    Environment from = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes02.prod', mount: '/media', state: 'stopped',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mes03.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     new Installation(hostname: 'mupld.prod', mount: '/mupld',
                                          name: 'mupld', gluScript: 'ivy:/com.linkedin.mupld/mupld-frontend/1.2',
                                          props: [:], parent: null)
                                     ])

    // the new environment
    Environment to = new Environment(name: 'env', installations: [
                                     new Installation(hostname: 'mes01.prod', mount: '/media',
                                          name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                                          props: [:], parent: null),
                                     // change version of mupld script
                                     new Installation(hostname: 'mupld.prod', mount: '/mupld',
                                          name: 'mupld', gluScript: 'ivy:/com.linkedin.mupld/mupld-frontend/1.3',
                                          props: [:], parent: null),
                                     // installing mpr... on mes03.prod
                                     new Installation(hostname: 'mes03.prod', mount: '/mpr',
                                          name: 'mpr', gluScript: 'ivy:/com.linkedin.mpr/mpr-frontend/1.0',
                                          props: [:], parent: null),
                                     // installing captcha
                                     new Installation(hostname: 'captcha.prod', mount: '/captcha',
                                          name: 'captcha', gluScript: 'ivy:/com.linkedin.captcha/captcha-backend/1.0',
                                          props: [:], parent: null),
                                     // installing captcha2
                                     new Installation(hostname: 'captcha.prod', mount: '/captcha2',
                                          name: 'captcha2', gluScript: 'ivy:/com.linkedin.captcha/captcha-backend/1.0',
                                          props: [:], parent: null)
                                     ])
    SimplePlanner planner = getPlanner()

    Plan plan= planner.createPlan(from, to)

    // phases are in order of execution
    assertEquals (['stop', 'unconfigure', 'uninstall', 'uninstallscript', 'installscript', 'install', 'configure', 'start'],
                  plan.phases)

    // hosts are ordered alphabetically (note how, even if mes01 is in the environment, it is not part of the plan)
    assertEquals (['captcha.prod', 'mes02.prod', 'mes03.prod', 'mupld.prod'],
                  plan.hosts)

    // The order of the actions in the plan is determined (as opposed to visiting the graph)
    assertEquals (["Stop media on mes03.prod", "Stop mupld on mupld.prod",
                   "Unconfigure media on mes02.prod", "Unconfigure media on mes03.prod", "Unconfigure mupld on mupld.prod",
                   "Uninstall media from mes02.prod", "Uninstall media from mes03.prod", "Uninstall mupld from mupld.prod",
                   "Uninstall script for installation media on mes02.prod", "Uninstall script for installation media on mes03.prod", "Uninstall script for installation mupld on mupld.prod",
                   "Install script for installation captcha on captcha.prod", "Install script for installation captcha2 on captcha.prod", "Install script for installation mpr on mes03.prod", "Install script for installation mupld on mupld.prod",
                   "Install captcha on captcha.prod", "Install captcha2 on captcha.prod", "Install mpr on mes03.prod", "Install mupld on mupld.prod",
                   "Configure captcha on captcha.prod", "Configure captcha2 on captcha.prod", "Configure mpr on mes03.prod", "Configure mupld on mupld.prod",
                   "Start captcha on captcha.prod", "Start captcha2 on captcha.prod", "Start mpr on mes03.prod", "Start mupld on mupld.prod"
                  ], plan.actionNodes.description)
  }

  /**
   * Takes a list of expected actions descriptions and the list of
   * actions returned by the planner.
   * It checks that all (and only) the given actions are present
   */
  private void checkActions(Map<String,List<String>> expectedActionsByPhase,
                            Graph<GraphNode<ActionDescriptor>> graph)
  {
    def phases = graph.getPropertyValues('phase')
    // build the error message, just in case
    def errorMessage = new StringBuilder()
    errorMessage << "EXPECTED\n"
    expectedActionsByPhase.each {key, value ->
      errorMessage << "${key.padRight(10, ' ')} ${value.join(', ')}\n"
    }
    errorMessage << 'ACTUAL\n'
    phases.each {key ->
      errorMessage << "${key.padRight(10, ' ')} ${graph.getNodes('phase', key).value.description?.join(', ')}\n"
    }

    // first check that the number of used phases is correct
    assertEquals("Wrong number of used phases.\n${errorMessage}",
                 expectedActionsByPhase.size(),
                 phases.size())

    // now check that the phases that we are actually using are the right ones
    phases.each { phase ->
      assertTrue("Missing expected phase (${phase}).\n${errorMessage}",
                 expectedActionsByPhase.containsKey(phase))

      assertEquals("Wrong number of actions for phase (${phase}).\n${errorMessage}",
                   expectedActionsByPhase[phase].size(),
                   graph.getNodes('phase', phase).size())
      // we surround the string with | so that all actions (including first and last)
      // are consistently demarked
      String actionsDescriptions = "|" + graph.getNodes('phase', phase).value.description.join('|') + "|"
      expectedActionsByPhase[phase].each { desc ->
        // we add the delimiters to the description so that we know the description is
        // the exact one (and not for example a subset of a different description)
        assertTrue("Missing expected action (${desc}) in phase (${phase}).\n${errorMessage}",
                   actionsDescriptions.contains("|" + desc + "|"))
      }
    }

  }

  public SimplePlanner getPlanner()
  {
    SimplePlanner planner = new SimplePlanner()
    // override the transitions to have different bounce state / reinstall state
    planner.installationTransitions = new InstallationTransitions(new StateMachineImpl(transitions: InstallationTransitions.TRANSITIONS),
      ['stop', 'unconfigure', 'uninstall','uninstallscript'],
      'installed', InstallationTransitions.NO_SCRIPT)
    return planner    
  }
}