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

package test.provisioner.impl.planner

import org.linkedin.glu.provisioner.impl.planner.InstallationTransitions
import org.linkedin.groovy.util.state.StateMachineImpl

/**
 * Test the {@link org.linkedin.glu.provisioner.impl.planner.InstallationTransitions} class
 *
 * author:  Riccardo Ferretti
 * created: Sep 14, 2009
 */
public class TestInstallationTransitions extends GroovyTestCase
{

  void testThroughState()
  {
    InstallationTransitions it = new InstallationTransitions(new StateMachineImpl(transitions: InstallationTransitions.TRANSITIONS),
          ['stop', 'unconfigure', 'uninstall','uninstallscript'],
          'installed', InstallationTransitions.NO_SCRIPT)

    // check that noscript has higher priority than installed
    assertEquals(InstallationTransitions.NO_SCRIPT, it.getThroughState([InstallationTransitions.NO_SCRIPT, 'running', 'installed']))
    // check that the position in the array doesn't affect the results
    assertEquals(InstallationTransitions.NO_SCRIPT, it.getThroughState(['running', 'installed', InstallationTransitions.NO_SCRIPT]))
    // check that installed has higher priority than other states
    assertEquals('installed', it.getThroughState(['running', 'installed', 'stopped']))
    // check that if no state is considered a through state, it will return null
    assertNull(it.getThroughState(['running', 'stopped']))

    // check the other signature for the method
    assertEquals(InstallationTransitions.NO_SCRIPT, it.getThroughState(['running', 'installed'], InstallationTransitions.NO_SCRIPT))
  }

  void testTransitions()
  {
    InstallationTransitions it = new InstallationTransitions()

    // check that a null through state doesn't cause an error and doesn't affect the path
    assertEquals(it.findShortestPath('running', 'stopped', null),
                 ['stop'])

    // check the same transition, this time using a through state
    assertEquals(it.findShortestPath('running', 'stopped', 'NONE'),
                 ['stop', 'unconfigure', 'uninstall', 'install', 'configure'])
  }

}