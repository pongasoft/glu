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

package test.orchestration.engine.deployment

import java.util.concurrent.Executors
import org.linkedin.glu.orchestration.engine.deployment.DeploymentServiceImpl
import org.linkedin.glu.orchestration.engine.deployment.DeploymentStorage
import org.linkedin.glu.orchestration.engine.deployment.InMemoryDeploymentStorage
import org.linkedin.glu.provisioner.plan.api.ILeafStepExecutor
import org.linkedin.glu.provisioner.plan.api.LeafStep
import org.linkedin.glu.provisioner.plan.impl.PlanExecutor
import org.linkedin.util.clock.Clock
import org.linkedin.glu.orchestration.engine.deployment.DeployerImpl
import org.linkedin.glu.provisioner.core.plan.impl.StepBuilder
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.deployment.CurrentDeployment

/**
 * @author yan@pongasoft.com */
public class TestDeploymentService extends GroovyTestCase
{
  long now = System.currentTimeMillis();
  Clock staticClock = [currentTimeMillis: { now }, currentDate: { new Date(now) }] as Clock

  def leafStepExecutor = { LeafStep leafStep ->
    if(leafStep.action)
      leafStep.action()
  }

  PlanExecutor planExecutor = new PlanExecutor(Executors.newCachedThreadPool(),
                                               leafStepExecutor as ILeafStepExecutor)
  DeploymentStorage deploymentStorage = new InMemoryDeploymentStorage(clock: staticClock)

  DeploymentServiceImpl deploymentService =
    new DeploymentServiceImpl(deploymentStorage: deploymentStorage,
                              deployer: new DeployerImpl(planExecutor: planExecutor))

  @Override
  protected void setUp()
  {
    super.setUp()
    planExecutor.clock = staticClock
  }

  /**
   * Test the methods related to plans
   */
  public void testPlans()
  {
    def out = []

    // 3 plans in fabric f1
    Map<String, Plan> plans = GroovyCollectionsUtils.toMapKey(['a', 'b', 'c']) { String planId ->
      createPlan('f1', planId) {
        out << "${planId}.S0.L0"
      }
    }

    // 1 plan in fabric f2
    plans['d'] = createPlan('f2', 'd') {
      out << "d.S0.L0"
    }

    plans.values().each { plan -> deploymentService.savePlan(plan) }

    // we make sure that getPlan works properly
    ['a', 'b', 'c', 'd'].each {
      assertTrue(deploymentService.getPlan(it).is(plans[it]))
    }

    // we make sure that getPlans(fabric) works properly
    assertEquals(['a', 'b', 'c'], deploymentService.getPlans('f1').collect { it.id }.sort())
    assertEquals(['d'], deploymentService.getPlans('f2').collect { it.id }.sort())

    // no deployments (need to execute plan for that)
    assertTrue(deploymentService.getDeployments('f1').isEmpty())

    CurrentDeployment deploymentA =
      deploymentService.executeDeploymentPlan(new SystemModel(id: 'sma', fabric: 'f1'), plans['a'])

    // should contain deploymentA at this time
    assertEquals([deploymentA], deploymentService.getDeployments('f1'))
    assertEquals([deploymentA], deploymentService.getDeployments('f1', 'a'))
  }

  private Plan createPlan(String fabric, String planId, Closure action)
  {
    def stepBuilder = new StepBuilder().sequential(name: "${planId}.S0".toString()) {
      leaf(name: "${planId}.S0.L0".toString(), action: action)
    }
    def plan = new Plan(stepBuilder.toStep())
    plan.id = planId
    plan.setMetadata('fabric', fabric)
    return plan
  }
}