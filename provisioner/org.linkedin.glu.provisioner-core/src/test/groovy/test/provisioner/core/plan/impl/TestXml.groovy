/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2014 Yan Pujante
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

import org.linkedin.glu.provisioner.plan.api.IPlanBuilder
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.provisioner.core.plan.impl.StepBuilder

/**
 * @author ypujante@linkedin.com */
class TestXml extends GroovyTestCase
{
  public void testPlanXml()
  {
    def stepBuilder = new StepBuilder().sequential(name: 'S0', k: 'K0') {
      leaf(name: 'S0.L1.1', k: 'K0.L1.1', action: { out << "S0.L1.1" })
      leaf(name: 'S0.L1.2', action: { throw new Exception('S0.L1.2') })
      sequential(name: 'S0.S1.3') {
        leaf(name: 'S0.S1.3.L2.1', action: { out << "S0.S1.3.L2.1" })
        leaf(name: 'S0.S1.3.L2.2', action: { out << "S0.S1.3.L2.2" })
      }
      parallel(name: 'S0.P1.4', k: 'K0.P1.4') {
        leaf(name: 'S0.P1.4.L2.1', action: { out << "S0.P1.4.L2.1" })
        leaf(name: 'S0.P1.4.L2.2', action: { out << "S0.P1.4.L2.2" })
      }
    }

    def plan = new Plan([name: "plan1", k1: 'v1'], stepBuilder.toStep())

    assertEquals("""<?xml version="1.0"?>
<plan name="plan1" k1="v1">
  <sequential name="S0" k="K0">
    <leaf name="S0.L1.1" k="K0.L1.1" />
    <leaf name="S0.L1.2" />
    <sequential name="S0.S1.3">
      <leaf name="S0.S1.3.L2.1" />
      <leaf name="S0.S1.3.L2.2" />
    </sequential>
    <parallel name="S0.P1.4" k="K0.P1.4">
      <leaf name="S0.P1.4.L2.1" />
      <leaf name="S0.P1.4.L2.2" />
    </parallel>
  </sequential>
</plan>
""", plan.toXml())

    plan = plan.toPlanBuilder().toPlan()

    assertEquals("""<?xml version="1.0"?>
<plan name="plan1" k1="v1">
  <sequential name="S0" k="K0">
    <leaf name="S0.L1.1" k="K0.L1.1" />
    <leaf name="S0.L1.2" />
    <sequential name="S0.S1.3">
      <leaf name="S0.S1.3.L2.1" />
      <leaf name="S0.S1.3.L2.2" />
    </sequential>
    <parallel name="S0.P1.4" k="K0.P1.4">
      <leaf name="S0.P1.4.L2.1" />
      <leaf name="S0.P1.4.L2.2" />
    </parallel>
  </sequential>
</plan>
""", plan.toXml())
  }

  public void testMaxParallelStepsCount()
  {
    def stepBuilder = new StepBuilder(new IPlanBuilder.Config(maxParallelStepsCount: 2)).
      parallel(name: 'P0', k: 'K0') {
      leaf(name: 'P0.L1.1', k: 'K0.L1.1', action: { out << "P0.L1.1" })
      leaf(name: 'P0.L1.2', action: { throw new Exception('P0.L1.2') })
      sequential(name: 'S0.S1.3') {
        leaf(name: 'P0.S1.3.L2.1', action: { out << "P0.S1.3.L2.1" })
        leaf(name: 'P0.S1.3.L2.2', action: { out << "P0.S1.3.L2.2" })
      }
      parallel(name: 'P0.P1.4', k: 'K0.P1.4') {
        leaf(name: 'P0.P1.4.L2.1', action: { out << "P0.P1.4.L2.1" })
        leaf(name: 'P0.P1.4.L2.2', action: { out << "P0.P1.4.L2.2" })
        leaf(name: 'P0.P1.4.L2.3', action: { out << "P0.P1.4.L2.3" })
      }
    }

    def plan = new Plan([name: "plan1", k1: 'v1'], stepBuilder.toStep())

    assertEquals("""<?xml version="1.0"?>
<plan name="plan1" k1="v1">
  <sequential name="P0" k="K0" maxParallelStepsCount="2" parallelStepsCount="4">
    <parallel name="P0" k="K0" sequentialIndex="0">
      <leaf name="P0.L1.1" k="K0.L1.1" />
      <leaf name="P0.L1.2" />
    </parallel>
    <parallel name="P0" k="K0" sequentialIndex="1">
      <sequential name="S0.S1.3">
        <leaf name="P0.S1.3.L2.1" />
        <leaf name="P0.S1.3.L2.2" />
      </sequential>
      <sequential name="P0.P1.4" k="K0.P1.4" maxParallelStepsCount="2" parallelStepsCount="3">
        <parallel name="P0.P1.4" k="K0.P1.4" sequentialIndex="0">
          <leaf name="P0.P1.4.L2.1" />
          <leaf name="P0.P1.4.L2.2" />
        </parallel>
        <parallel name="P0.P1.4" k="K0.P1.4" sequentialIndex="1">
          <leaf name="P0.P1.4.L2.3" />
        </parallel>
      </sequential>
    </parallel>
  </sequential>
</plan>
""", plan.toXml())

  }
}
