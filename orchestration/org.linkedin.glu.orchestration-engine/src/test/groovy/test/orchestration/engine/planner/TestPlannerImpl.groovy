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

import org.linkedin.glu.orchestration.engine.planner.PlannerImpl
import org.linkedin.glu.provisioner.plan.api.IStep.Type
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.orchestration.engine.planner.ActionDescriptor
import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta
import org.linkedin.glu.orchestration.engine.delta.DeltaMgr
import org.linkedin.glu.orchestration.engine.delta.DeltaMgrImpl

/**
 * @author yan@pongasoft.com */
public class TestPlannerImpl extends GroovyTestCase
{
  def PlannerImpl planner = new PlannerImpl()
  def DeltaMgr deltaMgr = new DeltaMgrImpl()

  public void testDeploymentPlanNoDelta()
  {
    assertNull(planner.computeDeploymentPlan(Type.SEQUENTIAL, null))

    Plan<ActionDescriptor> plan = plan(Type.SEQUENTIAL,
                                       delta(m([agent: 'a1', mountPoint: 'm1', script: 's1']),
                                             m([agent: 'a1', mountPoint: 'm1', script: 's1'])))

    println plan.toXml()
  }

  /**
   * When delta means deploy (not in current)
   */
  public void testDeploymentPlanDeploy()
  {
    Plan<ActionDescriptor> plan = plan(Type.SEQUENTIAL,
                                       delta(m([agent: 'a1', mountPoint: 'm1', script: 's1']),
                                             m()))

    println plan.toXml()
  }

  /**
   * When delta means undeploy (not in expected)
   */
  public void testDeploymentPlanUnDeploy()
  {
    Plan<ActionDescriptor> plan = plan(Type.SEQUENTIAL,
                                       delta(m(),
                                             m([agent: 'a1', mountPoint: 'm1', script: 's1'])))

    println plan.toXml()
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