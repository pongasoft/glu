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

package org.linkedin.glu.orchestration.engine.deployment

import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.plan.api.IPlanExecutionProgressTracker
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor

/**
 * System service.
 *
 * @author ypujante@linkedin.com */
interface DeploymentService
{
  Plan<ActionDescriptor> getPlan(String id)

  void savePlan(Plan<ActionDescriptor> plan)

  Collection<CurrentDeployment> getDeployments(String fabric)

  /**
   * Returns all the deployments matching the closure
   */
  Collection<CurrentDeployment> getDeployments(String fabric, Closure closure)

  /**
   * @return <code>true</code> if the deployment was archived, <code>false</code> if there is
   * no such deployment
   * @throws IllegalStateException if the deployment is running (cannot be archived while still
   * running!)
   */
  boolean archiveDeployment(String id)

  /**
   * Archive all deployments (that are completed of course)
   * @return the number of archived deployments
   */
  int archiveAllDeployments(String fabric)

  CurrentDeployment getDeployment(String id)

  ArchivedDeployment getArchivedDeployment(String id)

  boolean isExecutingDeploymentPlan(String fabric)

  CurrentDeployment executeDeploymentPlan(SystemModel system, Plan plan)

  CurrentDeployment executeDeploymentPlan(SystemModel system,
                                          Plan plan,
                                          String description,
                                          IPlanExecutionProgressTracker progressTracker)
}

