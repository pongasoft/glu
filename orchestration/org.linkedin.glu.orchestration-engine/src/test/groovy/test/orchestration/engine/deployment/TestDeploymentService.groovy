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
import org.linkedin.glu.orchestration.engine.deployment.DeployerImpl
import org.linkedin.glu.provisioner.core.plan.impl.StepBuilder
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.deployment.CurrentDeployment
import org.linkedin.util.clock.SettableClock
import org.linkedin.util.clock.Timespan
import org.linkedin.util.concurrent.ThreadControl
import org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils
import org.linkedin.util.clock.SystemClock

/**
 * @author yan@pongasoft.com */
public class TestDeploymentService extends GroovyTestCase
{
  long now = System.currentTimeMillis();
  SettableClock staticClock = new SettableClock(now)

  def leafStepExecutor = { LeafStep leafStep ->
    if(leafStep.action)
      leafStep.action()
  }

  PlanExecutor planExecutor = new PlanExecutor(Executors.newCachedThreadPool(),
                                               leafStepExecutor as ILeafStepExecutor)
  DeploymentStorage deploymentStorage = new InMemoryDeploymentStorage(clock: staticClock)

  DeploymentServiceImpl deploymentService =
    new DeploymentServiceImpl(deploymentStorage: deploymentStorage,
                              deployer: new DeployerImpl(planExecutor: planExecutor),
                              clock: staticClock)

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

  public void testAutoArchive()
  {
    ThreadControl tc = new ThreadControl(Timespan.parse('30s'))

    // make sure it is set to 30m
    deploymentService.autoArchiveTimeout = Timespan.parse("30m")

    // p0 starts at 0
    Plan p0 = createPlan("f1", "p0") {
      tc.block('p0')
    }
    deploymentService.savePlan(p0)
    CurrentDeployment dp0 =
      deploymentService.executeDeploymentPlan(new SystemModel(id: 'sma', fabric: 'f1'), p0)

    // p1 starts at +10m
    staticClock.addDuration(Timespan.parse('10m'))
    Plan p1 = createPlan("f1", "p1") {
      tc.block('p1')
    }
    deploymentService.savePlan(p1)
    CurrentDeployment dp1 =
      deploymentService.executeDeploymentPlan(new SystemModel(id: 'sma', fabric: 'f1'), p1)

    // p1 starts at +20m
    staticClock.addDuration(Timespan.parse('10m'))
    Plan p2 = createPlan("f1", "p2") {
      tc.block('p2')
    }
    deploymentService.savePlan(p2)
    CurrentDeployment dp2 =
      deploymentService.executeDeploymentPlan(new SystemModel(id: 'sma', fabric: 'f1'), p2)

    // we make sure all plans are 'running'
    ['p0', 'p1', 'p2'].each { tc.waitForBlock(it) }

    // we are now at +25m
    staticClock.addDuration(Timespan.parse('5m'))

    // we let p1 end (at +25m)
    tc.unblock('p1')
    GroovyConcurrentUtils.waitForCondition(SystemClock.INSTANCE, '10s', '100') {
      dp1.planExecution.isCompleted()
    }

    // we are now at +40m (15m after p1 ended)
    staticClock.addDuration(Timespan.parse('15m'))

    // nothing should have changed
    assertEquals(3, deploymentService.getDeployments('f1').size())
    // we run the closure
    deploymentService.autoArchiveClosure()
    // nothing should have changed
    assertEquals(3, deploymentService.getDeployments('f1').size())

    // we let p0 and p2 end (at +40m)
    tc.unblock('p0')
    GroovyConcurrentUtils.waitForCondition(SystemClock.INSTANCE, '10s', '100') {
      dp0.planExecution.isCompleted()
    }
    tc.unblock('p2')
    GroovyConcurrentUtils.waitForCondition(SystemClock.INSTANCE, '10s', '100') {
      dp2.planExecution.isCompleted()
    }

    // we are now at +54m59 (29m59s after p1 ended)
    staticClock.addDuration(Timespan.parse('14m59s'))

    // nothing should have changed
    assertEquals(3, deploymentService.getDeployments('f1').size())
    // we run the closure
    deploymentService.autoArchiveClosure()
    // nothing should have changed
    assertEquals(3, deploymentService.getDeployments('f1').size())

    // we are now at +55m (30m after p1 ended)
    staticClock.addDuration(Timespan.parse('1s'))
    // nothing should have changed (closure not ran yet)
    assertEquals(3, deploymentService.getDeployments('f1').size())
    assertTrue(deploymentService.getDeployments('f1').find { it.id == dp1.id} != null)
    // we run the closure
    deploymentService.autoArchiveClosure()
    // p1 is now automatically archived
    assertEquals(2, deploymentService.getDeployments('f1').size())
    assertTrue(deploymentService.getDeployments('f1').find { it.id == dp1.id} == null)

    // we are now at +1h09m59s (29m59s after p0 and p2 ended)
    staticClock.addDuration(Timespan.parse('14m59s'))
    // nothing should have changed
    assertEquals(2, deploymentService.getDeployments('f1').size())
    // we run the closure
    deploymentService.autoArchiveClosure()
    // nothing should have changed
    assertEquals(2, deploymentService.getDeployments('f1').size())

    // we are now at +1h10m (30m after p0 and p2 ended)
    staticClock.addDuration(Timespan.parse('1s'))
    // nothing should have changed
    assertEquals(2, deploymentService.getDeployments('f1').size())
    // we run the closure
    deploymentService.autoArchiveClosure()
    // nothing should have changed
    assertEquals(0, deploymentService.getDeployments('f1').size())

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