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
    assertEquals(4, p.leafStepsCount)
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
}