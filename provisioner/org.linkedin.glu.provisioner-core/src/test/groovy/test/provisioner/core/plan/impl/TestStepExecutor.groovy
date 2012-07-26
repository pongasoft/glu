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

package test.provisioner.core.plan.impl

import java.util.concurrent.Executors
import org.linkedin.glu.provisioner.plan.api.LeafStep
import org.linkedin.glu.provisioner.plan.api.ILeafStepExecutor
import org.linkedin.glu.provisioner.plan.api.NoOpPlanExecutionProgressTracker
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.provisioner.plan.api.XmlStepCompletionStatusVisitor
import org.linkedin.glu.provisioner.core.plan.impl.StepBuilder
import org.linkedin.glu.provisioner.plan.impl.StepExecutionContext
import org.linkedin.util.clock.Clock

/**
 * @author ypujante@linkedin.com */
public class TestStepExecutor extends GroovyTestCase
{

  private long now = System.currentTimeMillis();
  private Clock staticClock = [currentTimeMillis: { now }] as Clock
  private def leafStepExecutor = { LeafStep leafStep ->
    if(leafStep.action)
      leafStep.action()
  }
  private StepExecutionContext ctx = new StepExecutionContext( Executors.newCachedThreadPool(),
                                                               Executors.newCachedThreadPool(),
                                                               leafStepExecutor as ILeafStepExecutor,
                                                               NoOpPlanExecutionProgressTracker.instance(),
                                                               staticClock);

  public void testErrorInSequentialSteps()
  {
    def out = []

    def stepBuilder = new StepBuilder().sequential(name: 'S0') {
      leaf(name: 'S0.L1.1', action: { out << "S0.L1.1" })
      leaf(name: 'S0.L1.2', action: { throw new Exception('S0.L1.2') })
      sequential(name: 'S0.S1.3') {
        leaf(name: 'S0.S1.3.L2.1', action: { out << "S0.S1.3.L2.1" })
        leaf(name: 'S0.S1.3.L2.2', action: { out << "S0.S1.3.L2.2" })
      }
      parallel(name: 'S0.P1.4') {
        leaf(name: 'S0.P1.4.L2.1', action: { out << "S0.P1.4.L2.1" })
        leaf(name: 'S0.P1.4.L2.2', action: { out << "S0.P1.4.L2.2" })
      }
    }

    def plan = new Plan(stepBuilder.toStep())

    def executor = ctx.executePlan(plan)
    def status = executor.waitForCompletion()

    assertEquals(IStepCompletionStatus.Status.FAILED, status.status)

    // we make sure that 
    assertEquals([IStepCompletionStatus.Status.COMPLETED,
                  IStepCompletionStatus.Status.FAILED,
                  IStepCompletionStatus.Status.SKIPPED,
                  IStepCompletionStatus.Status.SKIPPED], status.statuses.status)

    assertEquals(["S0.L1.1"], out)
  }

  public void testXml()
  {
    def stepBuilder = new StepBuilder().sequential(name: 'S0') {
      leaf(name: 'S0.L1.1', action: { })
      leaf(name: 'S0.L1.2', action: { })
      sequential(name: 'S0.S1.3') {
        leaf(name: 'S0.S1.3.L2.1', action: { })
        leaf(name: 'S0.S1.3.L2.2', action: { })
      }
      parallel(name: 'S0.P1.4') {
        leaf(name: 'S0.P1.4.L2.1', action: { })
        leaf(name: 'S0.P1.4.L2.2', action: { })
      }
    }

    def plan = new Plan([name: "plan1"], stepBuilder.toStep())
    def executor = ctx.executePlan(plan)
    def status = executor.waitForCompletion()

    def nowAsAString = XmlStepCompletionStatusVisitor.formatTime(now)

    assertEquals("""<?xml version="1.0"?>
<plan name="plan1">
  <sequential name="S0" startTime="${nowAsAString}" endTime="${nowAsAString}" status="COMPLETED">
    <leaf name="S0.L1.1" startTime="${nowAsAString}" endTime="${nowAsAString}" status="COMPLETED" />
    <leaf name="S0.L1.2" startTime="${nowAsAString}" endTime="${nowAsAString}" status="COMPLETED" />
    <sequential name="S0.S1.3" startTime="${nowAsAString}" endTime="${nowAsAString}" status="COMPLETED">
      <leaf name="S0.S1.3.L2.1" startTime="${nowAsAString}" endTime="${nowAsAString}" status="COMPLETED" />
      <leaf name="S0.S1.3.L2.2" startTime="${nowAsAString}" endTime="${nowAsAString}" status="COMPLETED" />
    </sequential>
    <parallel name="S0.P1.4" startTime="${nowAsAString}" endTime="${nowAsAString}" status="COMPLETED">
      <leaf name="S0.P1.4.L2.1" startTime="${nowAsAString}" endTime="${nowAsAString}" status="COMPLETED" />
      <leaf name="S0.P1.4.L2.2" startTime="${nowAsAString}" endTime="${nowAsAString}" status="COMPLETED" />
    </parallel>
  </sequential>
</plan>
""", executor.toXml())
  }

  private void addLeafStep(stepBuilder, String name, Closure closure)
  {
    stepBuilder.addLeafStep(new LeafStep(null, [name: name], closure))
  }

}