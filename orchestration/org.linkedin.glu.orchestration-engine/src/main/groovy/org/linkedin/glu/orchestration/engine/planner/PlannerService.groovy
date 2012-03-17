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

package org.linkedin.glu.orchestration.engine.planner

import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.orchestration.engine.delta.DeltaSystemModelFilter

/**
 * @author yan@pongasoft.com */
public interface PlannerService
{
  static final String AGENT_SELF_UPGRADE_MOUNT_POINT = "/self/upgrade"

  /**
   * Computes the deployment plan for upgrading agents
   * @param params.type plan types (<code>null</code> means sequential, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   */
  Plan<ActionDescriptor> computeAgentsUpgradePlan(params, def metadata)

  /**
   * Computes the deployment plan for cleaning any upgrade that failed
   * @param params.type plan types (<code>null</code> means sequential, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   */
  Plan<ActionDescriptor> computeAgentsCleanupUpgradePlan(params, def metadata)

  /**
   * Generic call which defines which plan to create with the <code>params.planType</code> parameter
   * and allow for a plugin to take over entirely.
   *
   * @param params.planType the type of plan to create (deploy, undeploy, ...)
   * @param params.system the 'expected' system (with filters)
   * @param params.name name of the plan created
   * @param params.stepType plan types (<code>null</code> means both types, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   * @return the plans (0, 1 or 2) depending on whether there is a plan at all or if more than 1 type
   */
  Collection<Plan<ActionDescriptor>> computePlans(params, def metadata)

  /**
   * Generic call which defines which plan to create with the <code>params.planType</code> parameter
   * and allow for a plugin to take over entirely.
   *
   * @param params.planType the type of plan to create (deploy, undeploy, ...)
   * @param params.stepType the type of steps to create (sequential/parallel) (default to sequential)
   */
  Plan<ActionDescriptor> computePlan(params, def metadata)

  /**
   * Compute deployment plans by doing the following:
   * <ol>
   *   <li>compute delta between expected model (params.system) and current model (computed)
   *   <li>compute the deployment plan(s) (closure callback) (use params.type if a given type only
   *       is required
   *   <li>set various metadata on the plan(s) as well as the name
   * </ol>
   * @param params.system the 'expected' system (with filters)
   * @param params.name name of the plan created
   * @param params.stepType plan types (<code>null</code> means both types, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   * @return a collection of plans (<code>null</code> if no expected model) which may be empty
   */
  public Collection<Plan<ActionDescriptor>> computeDeploymentPlans(params,
                                                                   def metadata,
                                                                   def expectedModelFilter,
                                                                   def currentModelFiter,
                                                                   DeltaSystemModelFilter filter,
                                                                   Collection<String> toStates)
}