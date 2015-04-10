/*
 * Copyright (c) 2011-2014 Yan Pujante
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
import org.linkedin.glu.orchestration.engine.planner.TransitionPlan;
import org.linkedin.glu.provisioner.plan.api.IPlanBuilder;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.Plan;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class MultiDeltaTransitionPlan implements TransitionPlan<ActionDescriptor>
{
  private final Collection<SingleDeltaTransitionPlan> _transitionPlans;

  private TransitionPlan<ActionDescriptor> _transitionPlan;

  /**
   * Constructor
   */
  public MultiDeltaTransitionPlan(Collection<SingleDeltaTransitionPlan> transitionPlans)
  {
    _transitionPlans = transitionPlans;
  }

  public Collection<SingleDeltaTransitionPlan> getTransitionPlans()
  {
    return _transitionPlans;
  }

  public TransitionPlan<ActionDescriptor> getTransitionPlan()
  {
    if(_transitionPlan == null)
      _transitionPlan = buildTransitionPlan();
    return _transitionPlan;
  }


  @Override
  public Plan<ActionDescriptor> buildPlan(IStep.Type type, IPlanBuilder.Config config)
  {
    TransitionPlan<ActionDescriptor> transitionPlan = getTransitionPlan();
    if(transitionPlan == null)
      return null;
    else
      return transitionPlan.buildPlan(type, config);
  }

  protected TransitionPlan<ActionDescriptor> buildTransitionPlan()
  {
    if(_transitionPlans == null)
      return null;

    // only 1 => easy
    if(_transitionPlans.size() == 1)
      return _transitionPlans.iterator().next().getTransitionPlan();

    // 2 or more... need to 'connect' the transitions from one to the next
    Map<String, SingleEntryTransition> lastTransitions = null;
    Iterator<SingleDeltaTransitionPlan> iter = _transitionPlans.iterator();
    while(iter.hasNext())
    {
      SingleDeltaTransitionPlan transitionPlan = iter.next();

      if(lastTransitions == null)
        lastTransitions = transitionPlan.getLastTransitions();
      else
      {
        Map<String, SingleEntryTransition> firstTransitions = transitionPlan.getFirstTransitions();

        for(SingleEntryTransition set : lastTransitions.values())
        {
          SingleEntryTransition transition = firstTransitions.get(set.getEntryKey());
          if(transition != null)
            transition.executeAfter(set);
        }

        if(iter.hasNext())
          lastTransitions = transitionPlan.getLastTransitions();
      }
    }

    Set<Transition> transitions = new HashSet<Transition>();

    for(SingleDeltaTransitionPlan transitionPlan : _transitionPlans)
    {
      transitions.addAll(transitionPlan.getTransitions().values());
    }

    return new TransitionPlanImpl(transitions);
  }
}
