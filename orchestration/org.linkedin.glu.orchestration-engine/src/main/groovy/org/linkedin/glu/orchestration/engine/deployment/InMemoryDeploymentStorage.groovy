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

package org.linkedin.glu.orchestration.engine.deployment

import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock

/**
 * Basic implementation for in memory storage.. can be used for mocking. Not thread safe.
 *
 * @author yan@pongasoft.com */
public class InMemoryDeploymentStorage implements DeploymentStorage
{
  Clock clock = SystemClock.instance()
  int lastDeploymentId = 0
  Map<String, ArchivedDeployment> deployments = [:]

  @Override
  ArchivedDeployment getArchivedDeployment(String id)
  {
    deployments[id]
  }

  @Override
  Map getArchivedDeployments(String fabric, boolean includeDetails, params)
  {
    // TODO MED YP: currently ignoring params entirely!!
    [
      deployments: deployments.values().findAll { it.fabric == fabric },
      count: getArchivedDeploymentsCount(fabric)
    ]
  }

  @Override
  int getArchivedDeploymentsCount(String fabric)
  {
    deployments.values().findAll { it.fabric == fabric }.size()
  }

  @Override
  ArchivedDeployment startDeployment(String description, String fabric, String username, String details)
  {
    lastDeploymentId++

    ArchivedDeployment deployment = new ArchivedDeployment(id: lastDeploymentId.toString(),
                                                           username: username,
                                                           fabric: fabric,
                                                           startDate: clock.currentDate(),
                                                           description: description,
                                                           details: details)
    deployments[deployment.id] = deployment
    return deployment
  }

  @Override
  ArchivedDeployment endDeployment(String id, IStepCompletionStatus status, String details)
  {
    ArchivedDeployment deployment = deployments[id]

    deployment.startDate = new Date(status.startTime)
    deployment.endDate = new Date(status.endTime)
    deployment.status = status.status.name()
    deployment.details = details

    return deployment
  }
}