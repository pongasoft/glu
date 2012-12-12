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

package org.linkedin.glu.orchestration.engine.planner;

import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor;
import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.Plan;

import java.util.Collection;

/**
 * @author yan@pongasoft.com
 */
public interface Planner
{
  /**
   * Compute the deployment plan to 'fix' the delta
   * @return a plan with the main step of type <code>type</code>
   */
  Plan<ActionDescriptor> computeDeploymentPlan(IStep.Type type, SystemModelDelta systemModelDelta);

  /**
   * Compute the transition plan to 'fix' the delta
   * @return a transition plan to create a plan of the type you want
   */
  TransitionPlan<ActionDescriptor> computeTransitionPlan(SystemModelDelta systemModelDelta);

  /**
   * Compute the transition plan to 'fix' the deltas given a list of deltas (note that the order
   * of the deltas is very important and that
   * <code>deltas[n].currentModel == deltas[n-1].expectedModel</code> must be <code>true</code>.
   * @return a transition plan to create a plan of the type you want
   */
  TransitionPlan<ActionDescriptor> computeTransitionPlan(Collection<SystemModelDelta> deltas);

  /**
   * Computes the transition plan from the current state to the states provided in the collection
   * @return a plan with the main step of type <code>type</code>
   */
  Plan<ActionDescriptor> computeTransitionPlan(IStep.Type type,
                                               SystemModelDelta systemModelDelta,
                                               Collection<String> toStates);
}