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

/**
 * @author yan@pongasoft.com */
public interface PlannerService
{
  static final String AGENT_SELF_UPGRADE_MOUNT_POINT = "/self/upgrade"

  /**
   * @param params.system the 'expected' system (with filters)
   * @param params.name name of the plan created
   * @param params.type plan types (<code>null</code> means both types, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   * @return the plans (0, 1 or 2) depending on whether there is a plan at all or if more than 1 type
   */
  Collection<Plan<ActionDescriptor>> computeDeployPlans(params, def metadata)

  /**
   * @param params.system the 'expected' system (with filters)
   * @param params.name name of the plan created
   * @param params.type plan types (<code>null</code> means sequential, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   * @return the plans (0, 1 or 2) depending on whether there is a plan at all or if more than 1 type
   */
  Plan<ActionDescriptor> computeDeployPlan(params, def metadata)

  /**
   * Computes a transition plan.
   * @param params.type plan types (<code>null</code> means both types, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeTransitionPlans(params, def metadata)

  /**
   * Computes a transition plan.
   * @param params.type plan types (<code>null</code> means sequential, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   */
  Plan<ActionDescriptor> computeTransitionPlan(params, def metadata)

  /**
   * Compute a bounce plan to bounce (= stop/start) containers.
   * @param params.type plan types (<code>null</code> means both types, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeBouncePlans(params, def metadata)

  /**
   * Compute a bounce plan to bounce (= stop/start) containers.
   * @param params.type plan types (<code>null</code> means sequential, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   */
  Plan<ActionDescriptor> computeBouncePlan(params, def metadata)

  /**
   * Compute an undeploy plan.
   * @param params.type plan types (<code>null</code> means both types, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeUndeployPlans(params, def metadata)

  /**
   * Compute an undeploy plan.
   * @param params.type plan types (<code>null</code> means sequential, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   */
  Plan<ActionDescriptor> computeUndeployPlan(params, def metadata)

  /**
   * Compute a redeploy plan (= undeploy/deploy).
   * @param params.type plan types (<code>null</code> means both types, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   */
  Collection<Plan<ActionDescriptor>> computeRedeployPlans(params, def metadata)

  /**
   * Compute a redeploy plan (= undeploy/deploy).
   * @param params.type plan types (<code>null</code> means sequential, otherwise the type you want)
   * @param metadata any metadata to add to the plan(s)
   */
  Plan<ActionDescriptor> computeRedeployPlan(params, def metadata)

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
}