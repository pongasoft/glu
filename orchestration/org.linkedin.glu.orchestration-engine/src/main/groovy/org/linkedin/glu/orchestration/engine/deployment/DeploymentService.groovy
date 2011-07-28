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

  Collection<Plan<ActionDescriptor>> getPlans(String fabric)

  Collection<CurrentDeployment> getDeployments(String fabric)

  /**
   * Returns all the deployments matching the closure
   */
  Collection<CurrentDeployment> getDeployments(String fabric, Closure closure)

  /**
   * Returns all the deployments for the current plan
   */
  Collection<CurrentDeployment> getDeployments(String fabric, String planId)

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

  /**
   * params can be what grails accept for paginating queries: <code>max</code>,
   * <code>offset</code>, <code>sort</code>, <code>order</code>
   * @return a map with deployments: the list of archived deployments and
   *         count: the total number of entries
   */
  Map getArchivedDeployments(String fabric,
                             boolean includeDetails,
                             params)

  /**
   * @return number of archived deployments in this fabric
   */
  int getArchivedDeploymentsCount(String fabric)

  /**
   * If the deployment is not archived yet, then simply return it otherwise return the archived
   * version
   */
  Deployment getCurrentOrArchivedDeployment(String id)

  boolean isExecutingDeploymentPlan(String fabric)

  CurrentDeployment executeDeploymentPlan(SystemModel system, Plan plan)

  CurrentDeployment executeDeploymentPlan(SystemModel system,
                                          Plan plan,
                                          String description,
                                          IPlanExecutionProgressTracker progressTracker)
}

