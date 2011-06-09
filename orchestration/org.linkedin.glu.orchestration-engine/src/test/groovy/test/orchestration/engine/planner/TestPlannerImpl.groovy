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

package test.orchestration.engine.planner

import org.linkedin.glu.orchestration.engine.planner.impl.PlannerImpl
import org.linkedin.glu.provisioner.plan.api.IStep.Type
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor
import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta
import org.linkedin.glu.orchestration.engine.delta.DeltaMgr
import org.linkedin.glu.orchestration.engine.delta.impl.DeltaMgrImpl
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.glu.orchestration.engine.planner.impl.TransitionPlan
import org.linkedin.glu.orchestration.engine.planner.impl.Transition

/**
 * @author yan@pongasoft.com */
public class TestPlannerImpl extends GroovyTestCase
{
  def PlannerImpl planner = new PlannerImpl()
  def DeltaMgr deltaMgr = new DeltaMgrImpl()

  public void testDeploymentPlanNoDelta()
  {
    assertNull(planner.computeDeploymentPlan(Type.SEQUENTIAL, null))

    [Type.SEQUENTIAL, Type.PARALLEL].each { Type type ->
      Plan<ActionDescriptor> p = plan(type,
                                      delta(m([agent: 'a1', mountPoint: 'm1', script: 's1']),
                                            m([agent: 'a1', mountPoint: 'm1', script: 's1'])))

      assertEquals(type, p.step.type)
      assertEquals(0, p.leafStepsCount)
    }
  }

  /**
   * When delta means deploy (not in current)
   */
  public void testDeploymentPlanDeploy()
  {
    Plan<ActionDescriptor> p = plan(Type.SEQUENTIAL,
                                    delta(m([agent: 'a1', mountPoint: 'm1', script: 's1']),
                                          m()))

    assertEquals(Type.SEQUENTIAL, p.step.type)
    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <sequential agent="a1" mountPoint="m1">
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: install" scriptTransition="install" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: configure" scriptTransition="configure" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: start" scriptTransition="start" />
    </sequential>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(4, p.leafStepsCount)
  }

  /**
   * When delta means undeploy (not in expected)
   */
  public void testDeploymentPlanUnDeploy()
  {
    Plan<ActionDescriptor> p = plan(Type.SEQUENTIAL,
                                    delta(m(),
                                          m([agent: 'a1', mountPoint: 'm1', script: 's1'])))

    assertEquals(Type.SEQUENTIAL, p.step.type)
    assertEquals(4, p.leafStepsCount)
    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <sequential agent="a1" mountPoint="m1">
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: stop" scriptTransition="stop" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
    </sequential>
  </sequential>
</plan>
""", p.toXml())
  }

  /**
   * When delta means to fully undeploy and redeploy
   */
  public void testDeploymentPlanDelta()
  {
    Plan<ActionDescriptor> p = plan(Type.SEQUENTIAL,
                                    delta(m([agent: 'a1', mountPoint: 'm1', script: 's1']),
                                          m([agent: 'a1', mountPoint: 'm1', script: 's2'])))

    assertEquals(Type.SEQUENTIAL, p.step.type)
    assertEquals(8, p.leafStepsCount)
    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <sequential agent="a1" mountPoint="m1">
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: stop" scriptTransition="stop" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: install" scriptTransition="install" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: configure" scriptTransition="configure" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: start" scriptTransition="start" />
    </sequential>
  </sequential>
</plan>
""", p.toXml())
  }

  /**
   * When delta means to fully undeploy and redeploy (starting from installed with an expected
   * final state of stopped)
   */
  public void testDeploymentPlanDeltaWithEntryState()
  {
    Plan<ActionDescriptor> p = plan(Type.SEQUENTIAL,
                                    delta(m([agent: 'a1', mountPoint: 'm1', script: 's1', entryState: 'stopped']),
                                          m([agent: 'a1', mountPoint: 'm1', script: 's2', entryState: 'installed'])))

    assertEquals(Type.SEQUENTIAL, p.step.type)
    assertEquals(5, p.leafStepsCount)
    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <sequential agent="a1" mountPoint="m1">
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: install" scriptTransition="install" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: configure" scriptTransition="configure" />
    </sequential>
  </sequential>
</plan>
""", p.toXml())
  }

  /**
   * When delta means to simply bring the entry to its expected state
   */
  public void testDeploymentPlanUnexpectedState()
  {
    Plan<ActionDescriptor> p = plan(Type.SEQUENTIAL,
                                    delta(m([agent: 'a1', mountPoint: 'm1', script: 's1']),
                                          m([agent: 'a1', mountPoint: 'm1', script: 's1', entryState: 'installed'])))

    assertEquals(Type.SEQUENTIAL, p.step.type)
    assertEquals(2, p.leafStepsCount)
    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <sequential agent="a1" mountPoint="m1">
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: configure" scriptTransition="configure" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: start" scriptTransition="start" />
    </sequential>
  </sequential>
</plan>
""", p.toXml())
  }

  /**
   * Test that when the parent is in delta it triggers a plan which redeploys the child as well
   * (note how the steps are intermingled)
   */
  public void testParentChildDeltaParentDelta()
  {
    Plan<ActionDescriptor> p = plan(Type.PARALLEL,
                                    delta(m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                                            [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']),

                                          m([agent: 'a1', mountPoint: 'p1', script: 's2'],
                                            [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'])))

    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: uninstall" scriptTransition="uninstall" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
    </parallel>
    <parallel depth="5">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
    </parallel>
    <parallel depth="6">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: install" scriptTransition="install" />
    </parallel>
    <parallel depth="7">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: install" scriptTransition="install" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: configure" scriptTransition="configure" />
    </parallel>
    <parallel depth="8">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: configure" scriptTransition="configure" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
    <parallel depth="9">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(16, p.leafStepsCount)
  }

  /**
   * Complex case when the child changes parent and parent changes state...
   */
  public void testParentChildDeltaReparent()
  {
    Plan<ActionDescriptor> p = plan(Type.SEQUENTIAL,
                                    delta(m([agent: 'a1', mountPoint: 'p1', script: 's2'],
                                            [agent: 'a1', mountPoint: 'p2', script: 's1'],
                                            [agent: 'a1', mountPoint: 'c1', parent: 'p2', script: 's1'],
                                            [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1']),

                                          m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                                            [agent: 'a1', mountPoint: 'p2', script: 's1', entryState: 'stopped'],
                                            [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'],
                                            [agent: 'a1', mountPoint: 'c2', parent: 'p1', script: 's1'])))

    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <sequential depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: stop" scriptTransition="stop" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" name="TODO script action: stop" scriptTransition="stop" />
      <leaf agent="a1" fabric="f1" mountPoint="p2" name="TODO script action: start" scriptTransition="start" />
    </sequential>
    <sequential depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: stop" scriptTransition="stop" />
    </sequential>
    <sequential depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
    </sequential>
    <sequential depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: uninstall" scriptTransition="uninstall" />
    </sequential>
    <sequential depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
    </sequential>
    <sequential depth="5">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: install" scriptTransition="install" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
    </sequential>
    <sequential depth="6">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: configure" scriptTransition="configure" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: start" scriptTransition="start" />
      <leaf agent="a1" fabric="f1" mountPoint="c2" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: install" scriptTransition="install" />
    </sequential>
    <sequential depth="7">
      <leaf agent="a1" fabric="f1" mountPoint="c2" name="TODO script action: install" scriptTransition="install" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: configure" scriptTransition="configure" />
    </sequential>
    <sequential depth="8">
      <leaf agent="a1" fabric="f1" mountPoint="c2" name="TODO script action: configure" scriptTransition="configure" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: start" scriptTransition="start" />
    </sequential>
    <sequential depth="9">
      <leaf agent="a1" fabric="f1" mountPoint="c2" name="TODO script action: start" scriptTransition="start" />
    </sequential>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(25, p.leafStepsCount)
  }

  /**
   * Test that when the parent is in delta it triggers a plan which redeploys the child as well
   * (note how the steps are intermingled) even when there is a filter!
   */
  public void testParentChildDeltaWithFilter()
  {
    String filter = "mountPoint='p1'"

    Plan<ActionDescriptor> p = plan(Type.PARALLEL,
                                    delta(m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                                            [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']).filterBy(filter),

                                          m([agent: 'a1', mountPoint: 'p1', script: 's2'],
                                            [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'])))

    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: uninstall" scriptTransition="uninstall" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
    </parallel>
    <parallel depth="5">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
    </parallel>
    <parallel depth="6">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: install" scriptTransition="install" />
    </parallel>
    <parallel depth="7">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: install" scriptTransition="install" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: configure" scriptTransition="configure" />
    </parallel>
    <parallel depth="8">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: configure" scriptTransition="configure" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
    <parallel depth="9">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(16, p.leafStepsCount)
  }

  /**
   * Test for bounce/undeploy/redeploy
   */
  public void testTransitionPlan()
  {
    // bounce
    Plan<ActionDescriptor> p = plan(Type.SEQUENTIAL,
                                    delta(m([agent: 'a1', mountPoint: 'm1', script: 's1']),
                                          m([agent: 'a1', mountPoint: 'm1', script: 's1'])),
                                    ['stopped', '<expected>'])

    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <sequential agent="a1" mountPoint="m1">
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: stop" scriptTransition="stop" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: start" scriptTransition="start" />
    </sequential>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(2, p.leafStepsCount)

    // undeploy
    p = plan(Type.PARALLEL,
             delta(m([agent: 'a1', mountPoint: 'm1', script: 's1']),
                   m([agent: 'a1', mountPoint: 'm1', script: 's1'])),
             [null])

    assertEquals("""<?xml version="1.0"?>
<plan>
  <parallel>
    <sequential agent="a1" mountPoint="m1">
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: stop" scriptTransition="stop" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(4, p.leafStepsCount)

    // redeploy
    p = plan(Type.PARALLEL,
             delta(m([agent: 'a1', mountPoint: 'm1', script: 's1']),
                   m([agent: 'a1', mountPoint: 'm1', script: 's1'])),
             [null, '<expected>'])

    assertEquals("""<?xml version="1.0"?>
<plan>
  <parallel>
    <sequential agent="a1" mountPoint="m1">
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: stop" scriptTransition="stop" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: install" scriptTransition="install" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: configure" scriptTransition="configure" />
      <leaf agent="a1" fabric="f1" mountPoint="m1" name="TODO script action: start" scriptTransition="start" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(8, p.leafStepsCount)
  }

  /**
   * Test for bounce/undeploy/redeploy for parent/child
   */
  public void testTransitionPlanWithParentChild()
  {
    String parentFilter = "mountPoint='p1'"
    String childFilter = "mountPoint='c1'"

    // bounce
    Plan<ActionDescriptor> p = plan(Type.PARALLEL,
                                    delta(m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                                            [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']),

                                          m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                                            [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'])),
                                    ['stopped', '<expected>'])

    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(4, p.leafStepsCount)

    // TODO HIGH YP:  broken test need to fix code
//    // bounce (child only through filter => parent not included)
//    p = plan(Type.PARALLEL,
//             delta(m([agent: 'a1', mountPoint: 'p1', script: 's1'],
//                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']).filterBy(childFilter),
//
//                   m([agent: 'a1', mountPoint: 'p1', script: 's1'],
//                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'])),
//             ['stopped', '<expected>'])
//
//    assertEquals("""<?xml version="1.0"?>
//<plan>
//  <parallel>
//    <sequential agent="a1" mountPoint="c1">
//      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: stop" scriptTransition="stop" />
//      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: start" scriptTransition="start" />
//    </sequential>
//  </parallel>
//</plan>
//""", p.toXml())
//    assertEquals(2, p.leafStepsCount)

    // bounce (child only through filter => parent included because not started)
    p = plan(Type.PARALLEL,
             delta(m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']).filterBy(childFilter),

                   m([agent: 'a1', mountPoint: 'p1', script: 's1', entryState: 'installed'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1', entryState: 'installed'])),
             ['stopped', '<expected>'])

    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: configure" scriptTransition="configure" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: configure" scriptTransition="configure" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(4, p.leafStepsCount)

    // bounce (parent only through filter => child is included anyway)
    p = plan(Type.PARALLEL,
             delta(m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']).filterBy(parentFilter),

                   m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'])),
             ['stopped', '<expected>'])

    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(4, p.leafStepsCount)

    // undeployed (child only through filter => parent not included)
    p = plan(Type.PARALLEL,
             delta(m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']).filterBy(childFilter),

                   m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'])),
             [null])

    assertEquals("""<?xml version="1.0"?>
<plan>
  <parallel>
    <sequential agent="a1" mountPoint="c1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: stop" scriptTransition="stop" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
    </sequential>
  </parallel>
</plan>
""", p.toXml())
    assertEquals(4, p.leafStepsCount)

    // undeploy (parent only through filter => child is included anyway)
    p = plan(Type.PARALLEL,
             delta(m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']).filterBy(parentFilter),

                   m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'])),
             [null])

    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: uninstall" scriptTransition="uninstall" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(8, p.leafStepsCount)

    // TODO HIGH YP:  broken test need to fix code
//    // redeploy (child only through filter => parent not included)
//    p = plan(Type.PARALLEL,
//             delta(m([agent: 'a1', mountPoint: 'p1', script: 's1'],
//                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']).filterBy(childFilter),
//
//                   m([agent: 'a1', mountPoint: 'p1', script: 's1'],
//                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'])),
//             [null, '<expected>'])
//
//    assertEquals("""<?xml version="1.0"?>
//<plan>
//  <parallel>
//    <sequential agent="a1" mountPoint="c1">
//      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: stop" scriptTransition="stop" />
//      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
//      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: uninstall" scriptTransition="uninstall" />
//      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
//    </sequential>
//  </parallel>
//</plan>
//""", p.toXml())
//    assertEquals(8, p.leafStepsCount)

    // redeploy (parent only through filter => child is included anyway)
    p = plan(Type.PARALLEL,
             delta(m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1']).filterBy(parentFilter),

                   m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1'])),
             [null, '<expected>'])

    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: uninstall" scriptTransition="uninstall" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
    </parallel>
    <parallel depth="5">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
    </parallel>
    <parallel depth="6">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: install" scriptTransition="install" />
    </parallel>
    <parallel depth="7">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: install" scriptTransition="install" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: configure" scriptTransition="configure" />
    </parallel>
    <parallel depth="8">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: configure" scriptTransition="configure" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
    <parallel depth="9">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(16, p.leafStepsCount)

    // redeploy (parent only through filter => child is included anyway but should stop at desired state)
    p = plan(Type.PARALLEL,
             delta(m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1', entryState: 'installed']).filterBy(parentFilter),

                   m([agent: 'a1', mountPoint: 'p1', script: 's1'],
                     [agent: 'a1', mountPoint: 'c1', parent: 'p1', script: 's1', entryState: 'installed'])),
             [null, '<expected>'])

    assertEquals("""<?xml version="1.0"?>
<plan>
  <sequential>
    <parallel depth="0">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: uninstall" scriptTransition="uninstall" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: stop" scriptTransition="stop" />
    </parallel>
    <parallel depth="1">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: unconfigure" scriptTransition="unconfigure" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: uninstall" scriptTransition="uninstall" />
    </parallel>
    <parallel depth="2">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script lifecycle: uninstallScript" scriptLifecycle="uninstallScript" />
    </parallel>
    <parallel depth="3">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
    </parallel>
    <parallel depth="4">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script lifecycle: installScript" scriptLifecycle="installScript" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: install" scriptTransition="install" />
    </parallel>
    <parallel depth="5">
      <leaf agent="a1" fabric="f1" mountPoint="c1" name="TODO script action: install" scriptTransition="install" />
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: configure" scriptTransition="configure" />
    </parallel>
    <parallel depth="6">
      <leaf agent="a1" fabric="f1" mountPoint="p1" name="TODO script action: start" scriptTransition="start" />
    </parallel>
  </sequential>
</plan>
""", p.toXml())
    assertEquals(12, p.leafStepsCount)
  }


  private SystemModel m(Map... entries)
  {
    SystemModel model = new SystemModel(fabric: "f1")


    entries.each {
      model.addEntry(SystemEntry.fromExternalRepresentation(it))
    }

    return model
  }

  private SystemModelDelta delta(SystemModel expected, SystemModel current)
  {
    deltaMgr.computeDelta(expected, current)
  }

  private Plan<ActionDescriptor> plan(Type type, SystemModelDelta delta)
  {
    planner.computeDeploymentPlan(type, delta)
  }

  private Plan<ActionDescriptor> plan(Type type, SystemModelDelta delta, Collection<String> states)
  {
    planner.computeTransitionPlan(type, delta, states)
  }

  /**
   * Computes the digraph of the transitions
   * (to render with <code>dot -Tpdf < out of this method</code>)
   */
  private static String digraph(TransitionPlan transitions)
  {
    String graph = new TreeMap(transitions.transitions).values().collect { Transition t ->
      t.executeBefore.sort().collect { String key ->
        "\"${t.key}\" -> \"${key}\""
      }.join('\n')
    }.join('\n')

    "digraph delta {\n${graph}\n}"
  }

  private static String toStringAfter(TransitionPlan transitions)
  {
    JsonUtils.toJSON(new TreeMap(transitions.transitions).values().collect { Transition t ->
      "${t.key} -> ${t.executeAfter.sort()}"
    }).toString(2)
  }

  private static String toStringBefore(TransitionPlan transitions)
  {
    JsonUtils.toJSON(new TreeMap(transitions.transitions).values().collect { Transition t ->
      "${t.key} -> ${t.executeBefore.sort()}"
    }).toString(2)
  }
}