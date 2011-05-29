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
import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor

/**
 * System service.
 *
 * @author ypujante@linkedin.com */
interface DeploymentService
{
  /**
   * @param params.system the 'expected' system (with filters)
   * @param params.fabric the fabric (object)
   * @param params.name name of the plan created
   * @param params.type plan types (<code>null</code> means both types, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   * @return the plans (0, 1 or 2) depending on whether there is a plan at all or if more than 1 type
   */
  Collection<Plan<ActionDescriptor>> computeDeployPlans(params, def metadata)

  /**
   * Computes a transition plan.
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeTransitionPlans(params, def metadata)

  /**
   * Compute a bounce plan to bounce (= stop/start) containers.
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeBouncePlans(params, def metadata)

  /**
   * Compute an undeploy plan.
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeUndeployPlans(params, def metadata)

  /**
   * Compute a redeploy plan (= undeploy/deploy).
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeRedeployPlans(params, def metadata)

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

