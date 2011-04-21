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

import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.orchestration.engine.audit.AuditLogService
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.provisioner.plan.api.IPlanExecutionProgressTracker

/**
 * @author yan@pongasoft.com */
public class AuditedDeploymentService implements DeploymentService
{
  @Delegate
  @Initializable(required = true)
  DeploymentService deploymentService

  @Initializable
  AuditLogService auditLogService

  @Override
  CurrentDeployment executeDeploymentPlan(SystemModel system, Plan plan)
  {
    CurrentDeployment deployment = deploymentService.executeDeploymentPlan(system, plan)
    auditExecuteDeploymentPlan(deployment)
    return deployment
  }

  @Override
  CurrentDeployment executeDeploymentPlan(SystemModel system,
                                          Plan plan,
                                          String description,
                                          IPlanExecutionProgressTracker progressTracker)
  {
    CurrentDeployment deployment =
      deploymentService.executeDeploymentPlan(system, plan, description, progressTracker)
    auditExecuteDeploymentPlan(deployment)
    return deployment
  }

  protected void auditExecuteDeploymentPlan(CurrentDeployment deployment)
  {
    auditLogService?.audit('plan.execute', "plan: ${deployment.id}, desc: ${deployment.description}, systemId: ${deployment.systemId}")
  }

  int archiveAllDeployments(String fabric)
  {
    int count = deploymentService.archiveAllDeployments(fabric)
    if(count > 0)
    {
      auditLogService?.audit('plan.archive', "fabric: ${fabric}, count: ${count}")
    }

    return count
  }

  boolean archiveDeployment(String id)
  {
    if(deploymentService.archiveDeployment(id))
    {
      auditLogService?.audit('plan.archive', "plan: ${id}")
      return true
    }
    else
      return false
  }
  
}