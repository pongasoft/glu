/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

package test.orchestration.engine.delta

import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.glu.orchestration.engine.delta.DeltaServiceImpl
import org.linkedin.glu.orchestration.engine.delta.impl.DeltaMgrImpl
import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta.DeltaState

class TestDeltaService extends GroovyTestCase
{
  DeltaMgrImpl deltaMgr = new DeltaMgrImpl()
  DeltaServiceImpl deltaService = new DeltaServiceImpl(deltaMgr: deltaMgr)

  def DEFAULT_INCLUDED_IN_VERSION_MISMATCH = deltaMgr.includedInVersionMismatch

  void testDeltaService()
  {
    // empty
    def current = []
    def expected = []
    assertEqualsIgnoreType([], doComputeDelta(expected, current))

    // notDeployed
    current = []
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'notDeployed',
                            statusInfo: 'NOT deployed',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // notDeployed + cluster (GLU-393)
    current = []
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', cluster: 'cl1']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.cluster': 'cl1',
                            'metadata.container': 'c1',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'notDeployed',
                            statusInfo: 'NOT deployed',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // notExpectedState (with default = running)
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            entryState: 'stopped',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'stopped']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'metadata.currentState': 'stopped',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'notExpectedState',
                            statusInfo: 'running!=stopped',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // notExectedState (with specific state=stopped)
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            entryState: 'configured',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'configured']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            entryState: 'stopped',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'metadata.currentState': 'configured',
                            entryState: 'stopped',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'notExpectedState',
                            statusInfo: 'stopped!=configured',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // notRunning + versionMismatch => default is versionMismatch wins
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            entryState: 'stopped',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'stopped']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w2'],
            metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'metadata.currentState': 'stopped',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'delta',
                            statusInfo: ['entryState:[running!=stopped]',
                                         'initParameters.wars:[w2!=w1]'],
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w2'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // notRunning + versionMismatch => we force notRunning to win
    deltaService.notRunningOverridesVersionMismatch = true
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            entryState: 'stopped',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'stopped']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w2'],
            metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'metadata.currentState': 'stopped',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'notExpectedState',
                            statusInfo: 'running!=stopped',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w2'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // restoring defaults
    deltaService.notRunningOverridesVersionMismatch = false

    // versionMismatch (wars)
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            entryState: 'stopped',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'stopped']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w2'],
            metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'metadata.currentState': 'stopped',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'delta',
                            statusInfo: ['entryState:[running!=stopped]',
                                         'initParameters.wars:[w2!=w1]'],
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w2'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // versionMismatch (config)
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            entryState: 'stopped',
            initParameters: [wars: 'w1', config: 'cnf1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'stopped']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1', config: 'cnf2'],
            metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'initParameters.config': 'cnf2',
                            'metadata.currentState': 'stopped',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'delta',
                            statusInfo: ['entryState:[running!=stopped]',
                                         'initParameters.config:[cnf2!=cnf1]'],
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // versionMismatch (wars & config)
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            entryState: 'stopped',
            initParameters: [wars: 'w1', config: 'cnf1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'stopped']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w2', config: 'cnf2'],
            metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'initParameters.config': 'cnf2',
                            'metadata.currentState': 'stopped',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'delta',
                            statusInfo: ['entryState:[running!=stopped]',
                                         'initParameters.config:[cnf2!=cnf1]',
                                         'initParameters.wars:[w2!=w1]'],
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w2'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // versionMismatch (script)
    current = [
      [
        agent: 'a1', mountPoint: '/m1', script: 's1',
        entryState: 'stopped',
        initParameters: [wars: 'w1', config: 'cnf1'],
        metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'stopped']
      ]
    ]
    expected = [
      [
        agent: 'a1', mountPoint: '/m1', script: 's2',
        initParameters: [wars: 'w1', config: 'cnf1'],
        metadata: [container: 'c1', product: 'p1', version: 'R2']
      ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'initParameters.config': 'cnf1',
                            'metadata.currentState': 'stopped',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's2',
                            state: DeltaState.ERROR,
                            status: 'delta',
                            statusInfo: ['entryState:[running!=stopped]',
                                         'script:[s2!=s1]'],
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // versionMismatch (script) (with includedInVersionMismatch)
    withNewDeltaMgr(['script'], null ) {
      current = [
        [
          agent: 'a1', mountPoint: '/m1', script: 's1',
          initParameters: [wars: 'w1', config: 'cnf1'],
          metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
      ]
      expected = [
        [
          agent: 'a1', mountPoint: '/m1', script: 's2',
          initParameters: [wars: 'w1', config: 'cnf1'],
          metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
      ]
      assertEqualsIgnoreType([
                             [
                              'metadata.container': 'c1',
                              'initParameters.config': 'cnf1',
                              entryState: 'running',
                              key: 'a1:/m1',
                              agent: 'a1',
                              mountPoint: '/m1',
                              'metadata.product': 'p1',
                              script: 's2',
                              state: DeltaState.ERROR,
                              status: 'delta',
                              statusInfo: 'script:[s2!=s1]',
                              'metadata.version': 'R2',
                              'initParameters.wars': 'w1'
                              ]
                             ],
                             doComputeDelta(expected, current))
    }

    // versionMismatch (script) (with includedInVersionMismatch)
    withNewDeltaMgr(['initParameters.wars'], null) {
      current = [
        [
          agent: 'a1', mountPoint: '/m1', script: 's1',
          initParameters: [wars: 'w1', config: 'cnf1'],
          metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
      ]
      expected = [
        [
          agent: 'a1', mountPoint: '/m1', script: 's2',
          initParameters: [wars: 'w1', config: 'cnf1'],
          metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
      ]
      assertEqualsIgnoreType([
                             [
                              'metadata.container': 'c1',
                              'initParameters.config': 'cnf1',
                              entryState: 'running',
                              key: 'a1:/m1',
                              agent: 'a1',
                              mountPoint: '/m1',
                              'metadata.product': 'p1',
                              script: 's1',
                              state: DeltaState.OK,
                              status: 'expectedState',
                              statusInfo: 'running',
                              'metadata.version': 'R2',
                              'initParameters.wars': 'w1'
                              ]
                             ],
                             doComputeDelta(expected, current))
    }

    // versionMismatch (script) (with excludedInVersionMismatch)
    withNewDeltaMgr(DEFAULT_INCLUDED_IN_VERSION_MISMATCH, ['initParameters.wars']) {
      current = [
        [
          agent: 'a1', mountPoint: '/m1', script: 's1',
          initParameters: [wars: 'w1', config: 'cnf1'],
          metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'stopped']
        ]
      ]
      expected = [
        [
          agent: 'a1', mountPoint: '/m1', script: 's2',
          initParameters: [wars: 'w1', config: 'cnf1'],
          metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'stopped']
        ]
      ]
      assertEqualsIgnoreType([
                             [
                             'metadata.container': 'c1',
                             'initParameters.config': 'cnf1',
                             'metadata.currentState': 'stopped',
                             entryState: 'running',
                             key: 'a1:/m1',
                             agent: 'a1',
                             mountPoint: '/m1',
                             'metadata.product': 'p1',
                             script: 's2',
                             state: DeltaState.ERROR,
                             status: 'delta',
                             statusInfo: 'script:[s2!=s1]',
                             'metadata.version': 'R2',
                             'initParameters.wars': 'w1'
                             ]
                             ],
                             doComputeDelta(expected, current))
    }

    // versionMismatch (script) (with excludedInVersionMismatch)
    withNewDeltaMgr(DEFAULT_INCLUDED_IN_VERSION_MISMATCH, ['script']) {
      current = [
        [
          agent: 'a1', mountPoint: '/m1', script: 's1',
          initParameters: [wars: 'w1', config: 'cnf1'],
          metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
      ]
      expected = [
        [
          agent: 'a1', mountPoint: '/m1', script: 's2',
          initParameters: [wars: 'w1', config: 'cnf1'],
          metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
      ]
      assertEqualsIgnoreType([
                             [
                              'metadata.container': 'c1',
                              'initParameters.config': 'cnf1',
                              entryState: 'running',
                              key: 'a1:/m1',
                              agent: 'a1',
                              mountPoint: '/m1',
                              'metadata.product': 'p1',
                              script: 's1',
                              state: DeltaState.OK,
                              status: 'expectedState',
                              statusInfo: 'running',
                              'metadata.version': 'R2',
                              'initParameters.wars': 'w1'
                              ]
                             ],
                             doComputeDelta(expected, current))
    }

    // unexpected
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
    ]
    expected = [
        [
            agent: 'a2', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
    ]

    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'unexpected',
                            statusInfo: 'should NOT be deployed',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ],
                           [
                            'metadata.container': 'c1',
                            entryState: 'running',
                            key: 'a2:/m1',
                            agent: 'a2',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'notDeployed',
                            statusInfo: 'NOT deployed',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // error
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'running', error: 'in error']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'metadata.currentState': 'running',
                            'metadata.error': 'in error',
                             error: 'in error',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'error',
                            statusInfo: 'in error',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // ok
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            entryState: 'running',
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'running'],
            tags: ['ec:1', 'ec:2']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2'],
            tags: ['ee:1']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'metadata.currentState': 'running',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.OK,
                            status: 'expectedState',
                            statusInfo: 'running',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1',
                            'tags.ee:1': 'a1:/m1',
                             tags: ['ee:1']
                            ]
                            ],
                           doComputeDelta(expected, current))

    // ok (with cluster)
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            entryState: 'running',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'running']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', cluster: 'cl1', product: 'p1', version: 'R2']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'metadata.cluster': 'cl1',
                            'metadata.currentState': 'running',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.OK,
                            status: 'expectedState',
                            statusInfo: 'running',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // ok (with cluster)
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            entryState: 'running',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', cluster: 'cl1', product: 'p1', version: 'R2', currentState: 'running']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', cluster: 'cl2', product: 'p1', version: 'R2']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'metadata.cluster': 'cl1',
                            'metadata.currentState': 'running',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.OK,
                            status: 'expectedState',
                            statusInfo: 'running',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ]
                           ],
                           doComputeDelta(expected, current))

    // (system) tags
    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            entryState: 'running',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'running'],
            tags: ['ec:1', 'ec:2']
        ]
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2'],
            tags: ['ee:1']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'metadata.currentState': 'running',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.OK,
                            status: 'expectedState',
                            statusInfo: 'running',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1',
                            'tags.ee:1': 'a1:/m1',
                            'tags.a:2': 'a1:/m1',
                             tags: ['a:2', 'ee:1']
                            ]
                           ],
                           doComputeDelta(expected, current, { SystemModel cs, SystemModel es ->
                             cs.addAgentTags('a1', ['a:1'])
                             es.addAgentTags('a1', ['a:2'])
                             [cs, es]
                           }))

    current = [
    ]
    expected = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2'],
            tags: ['ee:1']
        ]
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'notDeployed',
                            statusInfo: 'NOT deployed',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1',
                            'tags.ee:1': 'a1:/m1',
                            'tags.a:2': 'a1:/m1',
                             tags: ['a:2', 'ee:1']
                            ]
                           ],
                           doComputeDelta(expected, current, { SystemModel cs, SystemModel es ->
                             cs.addAgentTags('a1', ['a:1'])
                             es.addAgentTags('a1', ['a:2'])
                             [cs, es]
                           }))

    current = [
        [
            agent: 'a1', mountPoint: '/m1', script: 's1',
            initParameters: [wars: 'w1'],
            metadata: [container: 'c1', product: 'p1', version: 'R2', currentState: 'running'],
            tags: ['ec:1', 'ec:2']
        ]
    ]
    expected = [
    ]
    assertEqualsIgnoreType([
                           [
                            'metadata.container': 'c1',
                            'metadata.currentState': 'running',
                            entryState: 'running',
                            key: 'a1:/m1',
                            agent: 'a1',
                            mountPoint: '/m1',
                            'metadata.product': 'p1',
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'unexpected',
                            statusInfo: 'should NOT be deployed',
                            'metadata.version': 'R2',
                            'initParameters.wars': 'w1'
                            ]
                           ],
                           doComputeDelta(expected, current, { SystemModel cs, SystemModel es ->
                             cs.addAgentTags('a1', ['a:1'])
                             es.addAgentTags('a1', ['a:2'])
                             [cs, es]
                           }))
  }

  public void testEmptyAgent()
  {
    def current
    def expected

    // nothing deployed on the agent at all
    current = [
      [
        agent: 'a1', entryState: 'NA', metadata: [emptyAgent: true, currentState: 'NA']
      ]
    ]
    expected = [
    ]

    assertEqualsIgnoreType([
                           [
                            'metadata.currentState': 'NA',
                            'metadata.emptyAgent': true,
                            entryState: 'NA',
                            key: 'a1:null',
                            agent: 'a1',
                            state: DeltaState.NA,
                            status: 'NA',
                            statusInfo: 'NA'
                            ]
                           ],
                           doComputeDelta(expected, current))

    current = [
      [
        agent: 'a1', entryState: 'NA', metadata: [emptyAgent: true, currentState: 'NA']
      ]
    ]
    expected = [
    ]

    assertEqualsIgnoreType([
                           [
                            'metadata.currentState': 'NA',
                            'metadata.emptyAgent': true,
                            entryState: 'NA',
                            key: 'a1:null',
                            agent: 'a1',
                            state: DeltaState.NA,
                            status: 'NA',
                            statusInfo: 'NA',
                            tags: ['a:2'],
                             "tags.a:2": "a1:null"
                            ]
                            ],
                           doComputeDelta(expected, current, { SystemModel cs, SystemModel es ->
                             cs.addAgentTags('a1', ['a:2'])
                             [cs, es]
                           }))
  }

  /**
   * Specific tests for parent/child relationship
   */
  void testParentChild()
  {
    def current
    def expected

    // parentDelta (parent needs redeploy => child needs redeploy)
    current = [
      [
        agent: 'a1', mountPoint: '/p1', script: 's1'
      ],
      [
        agent: 'a1', mountPoint: '/c1', script: 's1', parent: '/p1'
      ]
    ]
    expected = [
      [
        agent: 'a1', mountPoint: '/p1', script: 's2'
      ],
      [
        agent: 'a1', mountPoint: '/c1', script: 's1', parent: '/p1'
      ]
    ]
    assertEqualsIgnoreType([
                           [
                            entryState: 'running',
                            key: 'a1:/c1',
                            agent: 'a1',
                            mountPoint: '/c1',
                            parent: "/p1",
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'parentDelta',
                            statusInfo: 'needs redeploy (parent delta)'
                           ],
                           [
                            entryState: 'running',
                            key: 'a1:/p1',
                            agent: 'a1',
                            mountPoint: '/p1',
                            script: 's2',
                            state: DeltaState.ERROR,
                            status: 'delta',
                            statusInfo: 'script:[s2!=s1]'
                           ],
                           ],
                           doComputeDelta(expected, current))

    // when the filter, filters out the child, it still need to be present otherwise the plan
    // will be wrong!
    assertEqualsIgnoreType([
                           [
                            entryState: 'running',
                            key: 'a1:/c1',
                            agent: 'a1',
                            mountPoint: '/c1',
                            parent: "/p1",
                            script: 's1',
                            state: DeltaState.ERROR,
                            status: 'parentDelta',
                            statusInfo: 'needs redeploy (parent delta)'
                           ],
                           [
                            entryState: 'running',
                            key: 'a1:/p1',
                            agent: 'a1',
                            mountPoint: '/p1',
                            script: 's2',
                            state: DeltaState.ERROR,
                            status: 'delta',
                            statusInfo: 'script:[s2!=s1]'
                           ],
                           ],
                           deltaF(current, expected, "mountPoint='/p1'"))
  }

  // Testing for use case where metadata changes (version in this case)
  // entry | current | expected
  // e1    | null    | null
  // e2    | null    | R1016
  // e3    | null    | R1036
  // e4    | R1016   | null
  // e5    | R1016   | R1016
  // e6    | R1016   | R1036
  // e7    | R1036   | null
  // e8    | R1036   | R1016
  // e9    | R1036   | R1036
  def static CURRENT =
    [
        'm1': null, 'm2': null, 'm3': null,
        'm4': 'R1016', 'm5': 'R1016', 'm6': 'R1016',
        'm7': 'R1036', 'm8': 'R1036', 'm9': 'R1036'
    ]

  // building expected
  def static EXPECTED =
    [
        'm1': null, 'm2': 'R1016', 'm3': 'R1036',
        'm4': null, 'm5': 'R1016', 'm6': 'R1036',
        'm7': null, 'm8': 'R1016', 'm9': 'R1036'
    ]

  public void testMetadataChanges()
  {
    // Testing for use case where metadata changes (version in this case)
    // e1 | null  | null
    // e2 | null  | R1016
    // e3 | null  | R1036
    // e4 | R1016 | null
    // e5 | R1016 | R1016
    // e6 | R1016 | R1036
    // e7 | R1036 | null
    // e8 | R1036 | R1016
    // e9 | R1036 | R1036

    // full system computeDelta (everything up and running)
    def currentSystem = toSystem(CURRENT, 'running')
    def expectedSystem = toSystem(EXPECTED, null)
    doDeltaAndCheck(currentSystem, expectedSystem, CURRENT.keySet(), 'running')

    // full system computeDelta (everything stopped)
    currentSystem = toSystem(CURRENT, 'stopped')
    doDeltaAndCheck(currentSystem, expectedSystem, CURRENT.keySet(), 'stopped')

    // expectedSystem filtered by R1036 (everything up and running)
    currentSystem = toSystem(CURRENT, 'running')
    expectedSystem = expectedSystem.filterByMetadata('version', 'R1036')

    def expectedMountPoints = new TreeSet()
    expectedMountPoints.addAll(CURRENT.findAll { k,v -> v == 'R1036'}.collect { k,v -> k })
    expectedMountPoints.addAll(EXPECTED.findAll { k,v -> v == 'R1036'}.collect { k,v -> k })

    doDeltaAndCheck(currentSystem, expectedSystem, expectedMountPoints, 'running')

    // expectedSystem filtered by R1036 (everything stopped)
    currentSystem = toSystem(CURRENT, 'stopped')
    doDeltaAndCheck(currentSystem, expectedSystem, expectedMountPoints, 'stopped')
  }

  private void doDeltaAndCheck(SystemModel currentSystem,
                               SystemModel expectedSystem,
                               def expectedMountPoints,
                               String state)
  {
    def expectedDelta = []

    expectedMountPoints.each { mountPoint ->
      def entry =
      [
          agent: 'a1',
          entryState: 'running',
          mountPoint: "/${mountPoint}".toString(),
          script: 's1',
          'initParameters.wars': 'w1',
          'metadata.currentState': state,
          key: "a1:/${mountPoint}".toString(),
          status: state == 'running' ? 'expectedState' : 'notExpectedState',
          statusInfo: state == 'running' ? 'running' : 'running!=stopped',
          state: state == 'running' ? DeltaState.OK : DeltaState.ERROR
      ]

      // when not in error, then priority comes from 'current'
      if(state == 'running')
      {
        if(CURRENT[mountPoint])
          entry['metadata.version'] = CURRENT[mountPoint]
        else
        {
          if(EXPECTED[mountPoint])
            entry['metadata.version'] = EXPECTED[mountPoint]
        }
      }
      else
      {
        // when in error, then priority comes from 'expected'
        if(EXPECTED[mountPoint])
          entry['metadata.version'] = EXPECTED[mountPoint]
        else
        {
          if(CURRENT[mountPoint])
            entry['metadata.version'] = CURRENT[mountPoint]
        }
      }

      expectedDelta << entry
    }

    assertEqualsIgnoreType(expectedDelta, deltaService.computeDelta(expectedSystem, currentSystem))
  }

  private SystemModel toSystem(Map system, String currentState)
  {
    def entries = []

    system.each { mountPoint, version ->

      def entry =
      [
          agent: 'a1', mountPoint: "/${mountPoint}".toString(), script: 's1',
          initParameters: [wars: 'w1'],
          metadata: [:]
      ]

      if(currentState)
      {
        entry.metadata.currentState = currentState
        entry.entryState = currentState
      }

      if(version)
        entry.metadata.version = version

      entries << entry
    }

    return toSystem(entries)
  }

  private def deltaF(def current, def expected, def filter)
  {
    doComputeDelta(expected, current, null) { SystemModel cs, SystemModel es ->
      [cs, es.filterBy(filter)]
    }
  }

  private def doComputeDelta(def expected, def current)
  {
    doComputeDelta(expected, current, null, null)
  }

  private def doComputeDelta(def expected, def current, Closure closure)
  {
    doComputeDelta(expected, current, closure, null)
  }

  private def doComputeDelta(def expected, def current, Closure beforeEntries, Closure afterEntries)
  {
    SystemModel currentSystem = createEmptySystem(current)
    SystemModel expectedSystem = createEmptySystem(expected)

    if(beforeEntries)
      (currentSystem, expectedSystem) = beforeEntries(currentSystem, expectedSystem)

    addEntries(currentSystem, current)
    addEntries(expectedSystem, expected)

    if(afterEntries)
      (currentSystem, expectedSystem) = afterEntries(currentSystem, expectedSystem)

    return deltaService.computeDelta(expectedSystem, currentSystem)
  }

  private SystemModel toSystem(def system)
  {
    SystemModel res = createEmptySystem(system)
    addEntries(res, system)
    return res
  }

  private SystemModel createEmptySystem(def system)
  {
    system != null ? new SystemModel(fabric: 'f1') : null
  }

  private void addEntries(SystemModel model, def entries)
  {
    entries?.each { e ->
      model.addEntry(SystemEntry.fromExternalRepresentation(e))
    }
  }

  private void withNewDeltaMgr(def includedInVersionMismatch,
                               def excludedInVersionMismatch,
                               Closure closure)
  {
    def oldi = deltaMgr.includedInVersionMismatch
    def olde = deltaMgr.excludedInVersionMismatch
    deltaMgr.includedInVersionMismatch = includedInVersionMismatch as Set
    deltaMgr.excludedInVersionMismatch = excludedInVersionMismatch as Set
    try
    {
      closure()
    }
    finally
    {
      deltaMgr.excludedInVersionMismatch = olde
      deltaMgr.includedInVersionMismatch = oldi
    }
  }
  
  /**
   * Convenient call to compare and ignore type
   */
  void assertEqualsIgnoreType(o1, o2)
  {
    assertEquals(JsonUtils.toJSON(o1).toString(2), JsonUtils.toJSON(o2).toString(2))
    assertTrue("expected <${o1}> but was <${o2}>", GroovyCollectionsUtils.compareIgnoreType(o1, o2))
  }

}
