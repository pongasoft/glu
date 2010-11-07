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

package test.provisioner.core.fabric

import org.linkedin.glu.provisioner.core.environment.Environment
import org.linkedin.glu.provisioner.core.environment.Installation
import org.linkedin.glu.provisioner.core.fabric.SoftwareDefinition
import org.linkedin.glu.provisioner.core.fabric.InstallationDefinition
import org.linkedin.glu.provisioner.core.fabric.FabricDefinition

/**
 * Tests for the the {@link org.linkedin.glu.provisioner.core.fabric.FabricDefinition}
 *
 * author:  Riccardo Ferretti
 * created: Aug 20, 2009
 */
public class TestFabricDefinition extends GroovyTestCase
{

  void testEnvironmentCreation()
  {
    def softwares = [
      new SoftwareDefinition(id: 's1', gluScript: new URI('glu1'), props: ['s1p1': '1', 's1p2': '2']),
      new SoftwareDefinition(id: 's2', gluScript: new URI('glu2'), props: ['s2p1': '1', 's2p2': '2']),
    ]
    def level1 = [
      new InstallationDefinition(name: 'l1a', hostname: 'localhost', mount: 'l1a',
                                 software: softwares[0], instanceProperties: [:]),
      new InstallationDefinition(name: 'l1b', hostname: 'localhost', mount: 'l1b',
                                 software: softwares[1], instanceProperties: ['l1bp1': '1']),
      new InstallationDefinition(name: 'l1c', hostname: 'localhost', mount: 'l1c',
                                 software: softwares[0], instanceProperties: ['l1cp1': '1', 's1p1': '10']),
    ]
    def level2 = [
      new InstallationDefinition(name: 'l2a', hostname: 'localhost', mount: 'l2a', parent: level1[0],
                                 software: softwares[0], instanceProperties: [:]),
      new InstallationDefinition(name: 'l2b', hostname: 'localhost', mount: 'l2b', parent: level1[0],
                                 software: softwares[0], instanceProperties: ['l2bp1': '1', 's1p1': '10']),
      new InstallationDefinition(name: 'l2c', hostname: 'localhost', mount: 'l2c', parent: level1[1],
                                 software: softwares[0], instanceProperties: [:]),
    ]
    def level3 = [
      new InstallationDefinition(name: 'l3a', hostname: 'localhost', mount: 'l3a', parent: level2[0],
                                 software: softwares[1], instanceProperties: ['l3ap1': '1', 's4p1': '10']),
    ]

    def fd = new FabricDefinition(name: 'test', installations: level1 + level2 + level3)

    Environment env = fd.toEnvironment()

    def expected1 = [
        new Installation(name: 'l1a', hostname: 'localhost', mount: 'l1a', gluScript: new URI('glu1'), props: ["glu.installation.name": 'l1a', 's1p1': '1', 's1p2': '2']),
        new Installation(name: 'l1b', hostname: 'localhost', mount: 'l1b', gluScript: new URI('glu2'), props: ["glu.installation.name": 'l1b', 's2p1': '1', 's2p2': '2', 'l1bp1': '1']),
        new Installation(name: 'l1c', hostname: 'localhost', mount: 'l1c', gluScript: new URI('glu1'), props: ["glu.installation.name": 'l1c', 'l1cp1': '1', 's1p1': '10', 's1p2': '2']),
    ]

    def expected2 = [
        new Installation(name: 'l2a', hostname: 'localhost', mount: 'l2a', gluScript: new URI('glu1'), parent: expected1[0], props: ["glu.installation.name": 'l2a', 's1p1': '1', 's1p2': '2']),
        new Installation(name: 'l2b', hostname: 'localhost', mount: 'l2b', gluScript: new URI('glu1'), parent: expected1[0], props: ["glu.installation.name": 'l2b', 'l2bp1': '1', 's1p1': '10', 's1p2': '2']),
        new Installation(name: 'l2c', hostname: 'localhost', mount: 'l2c', gluScript: new URI('glu1'), parent: expected1[1], props: ["glu.installation.name": 'l2c', 's1p1': '1', 's1p2': '2']),
    ]

    def expected3 = [
        new Installation(name: 'l3a', hostname: 'localhost', mount: 'l3a', gluScript: new URI('glu2'), parent: expected2[0],props: ["glu.installation.name": 'l3a', 'l3ap1': '1', 's4p1': '10', 's2p1': '1', 's2p2': '2']),
    ]
    Environment expected = new Environment (name: 'test',
                                            installations: expected1 + expected2 + expected3)

    env.installations.each {inst ->
      def found = false
      expected.installations.each { exp ->
        if (inst.id == exp.id)
        {
          assertEquals(exp.id, exp, inst)
          found = true
        }
      }
      if (!found)
      {
        fail ("Installation ${inst.id} not found")
      }
    }
    assertEquals(expected, env)
  }

}