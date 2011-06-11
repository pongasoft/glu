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

package org.linkedin.glu.orchestration.engine.planner.impl;

import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.NoOpActionDescriptor;
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yan@pongasoft.com
 */
public class MissingAgentTransition extends SingleStepTransition implements SkippableTransition
{
  /**
   * Constructor
   */
  public MissingAgentTransition(TransitionPlan transitionPlan,
                                String key,
                                String entryKey,
                                String action,
                                String toState)
  {
    super(transitionPlan, key, entryKey, action, toState);
    _skipRootCause = this;
  }

  @Override
  public void addSteps(ICompositeStepBuilder<ActionDescriptor> builder)
  {
    builder.addLeafStep(buildStep(computeActionDescriptor()));
  }

  @Override
  public NoOpActionDescriptor computeActionDescriptor()
  {
    NoOpActionDescriptor ad = populateActionDescriptor(new NoOpActionDescriptor());
    ad.setReason("missingAgent");
    return ad;
  }
}
