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

package test.provisioner.impl.deployment

import org.linkedin.glu.provisioner.api.planner.Plan
import org.linkedin.glu.provisioner.core.action.IActionFactory
import org.linkedin.glu.provisioner.core.environment.Environment
import org.linkedin.glu.provisioner.core.environment.Installation
import org.linkedin.glu.provisioner.core.touchpoint.TouchpointActionFactory
import test.provisioner.impl.mocks.NoOpTouchpoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import org.linkedin.glu.provisioner.deployment.impl.DeploymentManager
import org.linkedin.glu.provisioner.deployment.api.IDeploymentManager
import org.linkedin.glu.provisioner.plan.api.IPlanExecutor
import org.linkedin.glu.provisioner.plan.impl.PlanExecutor
import org.linkedin.glu.provisioner.deployment.impl.ActionDescriptorStepExecutor
import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.plan.api.IStep
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus
import org.linkedin.glu.provisioner.core.graph.DepFirstVisitor
import org.linkedin.glu.provisioner.impl.planner.SimplePlanner

/**
 * Tests for the deployment plan class
 */
public class TestDeploymentPlan extends GroovyTestCase
{

  /**
   * Test that 'seeding' the deployment plan with the plan works
   */
  void testFromPlanToDeploymentPlan()
  {
    // the environments
    def from = getFromEnvironment()
    def to = getToEnvironment()

    // get the plan
    SimplePlanner planner = new SimplePlanner()
    Plan plan = planner.createPlan(from, to)

    // get the deployment manager
    IActionFactory actionFactory = new TouchpointActionFactory(['agent':new NoOpTouchpoint()]) //mock
    ExecutorService executor = Executors.newCachedThreadPool()

    IPlanExecutor planExecutor = new PlanExecutor(executorService: executor,
                                                  leafStepExecutor: new ActionDescriptorStepExecutor(actionFactory: actionFactory))

    IDeploymentManager mgr = new DeploymentManager(planner: planner, planExecutor: planExecutor)

    // get the deployment plan
    def dplan = mgr.createPlan('test', from, to, null)

    // first: check the deployment plan matches the plan
    List<ActionDescriptor> an = []

    new DepFirstVisitor(plan.graph).accept { node -> an << node.value }

    dplan.leafSteps.eachWithIndex {IStep step, i ->
      assertEquals(an[i], step.action)
    }

    // execute the plan and wait for it
    def planExecution = mgr.executePlan(dplan, null)
    planExecution.waitForCompletion()

    // second: double check the plan execution
    assertTrue(planExecution.isCompleted())
    assertFalse(planExecution.isPaused())
    assertFalse(planExecution.isCancelled())

    assertEquals(IStepCompletionStatus.Status.COMPLETED, planExecution.completionStatus.status)
    assertEquals(dplan.step, planExecution.completionStatus.step)

    planExecution.completionStatus.statuses.eachWithIndex { status, idx ->
      assertEquals(IStepCompletionStatus.Status.COMPLETED, status.status)
    }
  }

  Environment getFromEnvironment()
  {

    return new Environment(name: 'env', installations: [
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
  }

  Environment getToEnvironment()
  {
    // the new environment
    return new Environment(name: 'env', installations: [
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
  }
}