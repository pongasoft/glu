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
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;

import java.util.Collection;

/**
 * @author yan@pongasoft.com
 */
public class MultiStepsSingleEntryTransition extends SingleEntryTransition
{
  private final Collection<Transition> _transitions;

  /**
   * Constructor
   */
  public MultiStepsSingleEntryTransition(TransitionPlan transitionPlan,
                                         String key,
                                         String entryKey,
                                         Collection<Transition> transitions)
  {
    super(transitionPlan, key, entryKey);
    _transitions = transitions;
    for(Transition transition : transitions)
    {
      SkippableTransition skipRootCause = transition.getSkipRootCause();
      if(skipRootCause != null)
      {
        _skipRootCause = skipRootCause;
        break; // get out of the for
      }
    }
  }

  public Collection<Transition> getTransitions()
  {
    return _transitions;
  }

  @Override
  public void addSteps(ICompositeStepBuilder<ActionDescriptor> builder)
  {
    builder = builder.addSequentialSteps();

    builder.setMetadata("agent", getAgent());
    builder.setMetadata("mountPoint", getMountPoint());

    SkippableTransition skipRootCause = getSkipRootCause();
    if(skipRootCause != null)
    {
      builder.addLeafStep(buildStep(skipRootCause.computeActionDescriptor()));
    }
    else
    {
      for(Transition transition : _transitions)
      {
        transition.addSteps(builder);
      }
    }
  }
}
