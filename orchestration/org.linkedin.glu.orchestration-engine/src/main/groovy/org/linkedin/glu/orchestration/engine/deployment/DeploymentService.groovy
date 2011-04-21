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

import org.linkedin.glu.provisioner.core.environment.Environment
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.plan.api.IPlanExecutionProgressTracker
import org.linkedin.glu.provisioner.plan.api.IStep
import org.linkedin.glu.provisioner.plan.api.Plan

/**
 * System service.
 *
 * @author ypujante@linkedin.com */
interface DeploymentService
{
  Collection<String> getHostsWithDeltas(params)

  Plan computeDeploymentPlan(params)

  Plan computeDeploymentPlan(params, Closure closure)

  /**
   * Computes a transition plan. The closure is meant to
   * filter out the installations (<code>Installation</code>) and should return
   * <code>true</code> for all installation  that need to be part of the plan.
   */
  Plan computeTransitionPlan(params, Closure filter)

  /**
   * Compute a bounce plan to bounce (= stop/start) containers. The closure is meant to
   * filter out the installations (<code>Installation</code>) and should return
   * <code>true</code> for all installation  that need to be part of the plan.
   */
  Plan computeBouncePlan(params, Closure filter)

  /**
   * Compute an undeploy plan. The closure is meant to
   * filter out the installations (<code>Installation</code>) and should return
   * <code>true</code> for all installation  that need to be part of the plan.
   */
  Plan computeUndeployPlan(params, Closure filter)

  /**
   * Compute a redeploy plan (= undeploy/deploy). The closure is meant to
   * filter out the installations (<code>Installation</code>) and should return
   * <code>true</code> for all installation  that need to be part of the plan.
   */
  Plan computeRedeployPlan(params, Closure filter)

  Plan createPlan(String name,
                  Environment currentEnvironment,
                  Environment expectedEnvironment,
                  Closure closure)

  /**
   * Shortcut to group the plan by hostname first, then mountpoint in both sequential and parallel
   * types.
   */
  Map<IStep.Type, Plan> groupByHostnameAndMountPoint(Plan plan)

  /**
   * Create a new plan of the given type where the entries are grouped by hostname first, then
   * mountpoint and call the closure to filter all leaves.
   */
  Plan groupByHostnameAndMountPoint(Plan plan, IStep.Type type)

  /**
   * Shortcut to group the plan by instance in both sequential and parallel types.
   */
  Collection<Plan> groupByInstance(Plan plan, def metadata)

  /**
   * Create a new plan of the given type where the entries going to be grouped by instance.
   */
  Plan groupByInstance(Plan plan, IStep.Type type, def metadata)

  Plan getPlan(String id)

  void savePlan(Plan plan)

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

