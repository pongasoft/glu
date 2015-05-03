/*
 * Copyright (c) 2011-2015 Yan Pujante
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

import org.linkedin.glu.provisioner.core.plan.impl.StepBuilder
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.groovy.util.json.JsonUtils

/**
 * @author ypujante@linkedin.com */
class TestJson extends GroovyTestCase
{
  public void testPlanJson()
  {
    def stepBuilder = new StepBuilder().sequential(name: 'S0', k: 'K0') {
      leaf(name: 'S0.L1.1', k: 'K0.L1.1', action: { out << "S0.L1.1" })
      leaf(name: 'S0.L1.2', action: { throw new Exception('S0.L1.2') })
      sequential(name: 'S0.S1.3') {
        leaf(name: 'S0.S1.3.L2.1', action: { out << "S0.S1.3.L2.1" })
        leaf(name: 'S0.S1.3.L2.2', action: { out << "S0.S1.3.L2.2" })
      }
      sequential(name: 'S0.S2.4') {
        leaf(name: 'S0.S2.4.L2.1', action: { out << "S0.S2.4.L2.1" })
      }
      parallel(name: 'S0.P1.5', k: 'K0.P1.5') {
        leaf(name: 'S0.P1.5.L2.1', action: { out << "S0.P1.5.L2.1" })
        leaf(name: 'S0.P1.5.L2.2', action: { out << "S0.P1.5.L2.2" })
      }
    }

    def plan = new Plan([name: "plan1", k1: 'v1'], stepBuilder.toStep())

    assertEquals("""{
  "metadata": {
    "k1": "v1",
    "name": "plan1"
  },
  "steps": {
    "metadata": {
      "k": "K0",
      "name": "S0"
    },
    "steps": [
      {
        "metadata": {
          "k": "K0.L1.1",
          "name": "S0.L1.1"
        },
        "type": "leaf"
      },
      {
        "metadata": {
          "name": "S0.L1.2"
        },
        "type": "leaf"
      },
      {
        "metadata": {
          "name": "S0.S1.3"
        },
        "steps": [
          {
            "metadata": {
              "name": "S0.S1.3.L2.1"
            },
            "type": "leaf"
          },
          {
            "metadata": {
              "name": "S0.S1.3.L2.2"
            },
            "type": "leaf"
          }
        ],
        "type": "sequential"
      },
      {
        "metadata": {
          "name": "S0.S2.4"
        },
        "steps": {
          "metadata": {
            "name": "S0.S2.4.L2.1"
          },
          "type": "leaf"
        },
        "type": "sequential"
      },
      {
        "metadata": {
          "k": "K0.P1.5",
          "name": "S0.P1.5"
        },
        "steps": [
          {
            "metadata": {
              "name": "S0.P1.5.L2.1"
            },
            "type": "leaf"
          },
          {
            "metadata": {
              "name": "S0.P1.5.L2.2"
            },
            "type": "leaf"
          }
        ],
        "type": "parallel"
      }
    ],
    "type": "sequential"
  }
}""", JsonUtils.prettyPrint(plan.toJson()))
  }
}
