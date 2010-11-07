/*
 * Copyright 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.provisioner.api.planner

import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.provisioner.core.action.IDescriptionProvider
import org.linkedin.glu.provisioner.core.environment.Installation

/**
 * @author ypujante@linkedin.com */
interface IAgentPlanner
{
  /**
   * Create a plan to go to the provided list of states (one after the other) for the given
   * installation. Note that there is no validation whether it is possible or not.
   *
   * @param filter will be called for each step to filter it in or out of the plan
   * (return <code>true</code> to keep, <code>false</code> to reject)
   */
  Plan<ActionDescriptor> createTransitionPlan(Collection<Installation> installations,
                                              Collection<String> toStates,
                                              IDescriptionProvider descriptionProvider,
                                              Closure filter)

  /**
   * Create a plan to go from state <code>fromState</code> to state <code>toState</code>. Note that
   * there is no validation whether it is possible or not.
   */
  Plan<ActionDescriptor> createTransitionPlan(String agentName,
                                              String mountPoint,
                                              URI agentURI,
                                              String fromState,
                                              String toState,
                                              IDescriptionProvider descriptionProvider)

  /**
   * Create a plan to go to the provided list of states (one after the other). Note that
   * there is no validation whether it is possible or not.
   */
  Plan<ActionDescriptor> createTransitionPlan(String agentName,
                                              String mountPoint,
                                              URI agentURI,
                                              String fromState,
                                              Collection<String> toStates,
                                              IDescriptionProvider descriptionProvider)

  /**
   * Create the plan to upgrade a set of agents to the new version. The new agent code is located
   * at the provided coordinates. 
   */
  Plan<ActionDescriptor> createUpgradePlan(agents, String version, String coordinates)

  /**
   * Create the plan to cleanup if for any reason the upgrade failed. To easily restart 
   * from scratch.
   */
  Plan<ActionDescriptor> createCleanupPlan(agents, String version)
}