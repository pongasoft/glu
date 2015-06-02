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

package org.linkedin.glu.console.domain

import org.linkedin.glu.console.provisioner.services.storage.DeploymentStorageImpl
import org.linkedin.glu.orchestration.engine.deployment.ArchivedDeployment
import org.linkedin.glu.provisioner.plan.api.LeafStepCompletionStatus
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SettableClock
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus.Status
import org.linkedin.glu.orchestration.engine.deployment.DeploymentService
import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin

/**
 * @author yan@pongasoft.com */
@TestMixin(IntegrationTestMixin)
public class DbDeploymentIntegrationTests extends GroovyTestCase
{
  Clock clock = new SettableClock()

  DeploymentService deploymentService

  DeploymentStorageImpl getDeploymentStorage()
  {
    deploymentService.deploymentService.deploymentStorage
  }

  public void testIncludeDetails()
  {
    ArchivedDeployment deployment = deploymentStorage.startDeployment("d1",
                                                                      "f1",
                                                                      "u1",
                                                                      "d1")

    deploymentStorage.endDeployment(deployment.id,
                                    new LeafStepCompletionStatus(null,
                                                                 Timespan.parse('5s').pastTimeMillis(clock),
                                                                 clock.currentTimeMillis(),
                                                                 Status.COMPLETED,
                                                                 null),
                                    "d2")

    // with details
    Map res = deploymentStorage.getArchivedDeployments("f1", true, [:])
    assertEquals(1, res.count)
    assertEquals("d2", res.deployments.iterator().next().details)

    // without details
    res = deploymentStorage.getArchivedDeployments("f1", false, [:])
    assertEquals(1, res.count)
    assertNull(res.deployments.iterator().next().details)
  }
}