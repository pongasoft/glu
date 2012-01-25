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
import org.linkedin.glu.orchestration.engine.planner.TransitionPlan;
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.glu.provisioner.plan.api.PlanBuilder;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * @author yan@pongasoft.com
 */
public class TransitionPlanImpl implements TransitionPlan<ActionDescriptor>
{
  private final Collection<Transition> _transitions;
  private final Map<String, MultiStepsSingleEntryTransition> _mulitStepsOnly;

  /**
   * Constructor
   */
  public TransitionPlanImpl(Collection<Transition> transitions)
  {
    filterVirtual(transitions);
    _mulitStepsOnly = optimizeMultiSteps(transitions);
    _transitions = transitions;
  }

  public Plan<ActionDescriptor> buildPlan(IStep.Type type)
  {
    PlanBuilder<ActionDescriptor> builder = new PlanBuilder<ActionDescriptor>();

    if(_mulitStepsOnly != null)
    {
      ICompositeStepBuilder<ActionDescriptor> stepBuilder = builder.addCompositeSteps(type);
      for(MultiStepsSingleEntryTransition transition : _mulitStepsOnly.values())
      {
        transition.addSteps(stepBuilder);
      }
    }
    else
    {
      ICompositeStepBuilder<ActionDescriptor> stepBuilder = builder.addSequentialSteps();

      Set<Transition> roots =
        findRoots(new TreeSet<Transition>(Transition.TransitionComparator.INSTANCE));

      addSteps(stepBuilder, type, roots, 0, new HashSet<Transition>());
    }


    return builder.toPlan();
  }

  public Set<Transition> findRoots(Set<Transition> roots)
  {
    return findRoots(_transitions, roots);
  }

  private void addSteps(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                        IStep.Type type,
                        Set<Transition> transitions,
                        int depth,
                        Set<Transition> alreadyProcessed)
  {
    if(transitions.size() == 0)
      return;

    ICompositeStepBuilder<ActionDescriptor> depthBuilder = stepBuilder.addCompositeSteps(type);
    depthBuilder.setMetadata("depth", depth);

    Set<Transition> nextTransitions =
      new TreeSet<Transition>(Transition.TransitionComparator.INSTANCE);

    // we need to make sure that we only add the ones at the current depth!
    Set<Transition> thisDepthTransitions = new HashSet<Transition>();

    for(Transition transition : transitions)
    {
      if(!alreadyProcessed.contains(transition))
      {
        // if transition should be skipped then all following transitions will be skipped as well
        if(transition.shouldSkip())
        {
          transition.addSteps(depthBuilder);
          thisDepthTransitions.add(transition);
        }
        else
        {
          // make sure that all 'before' steps have been completed
          if(checkExecuteBefore(transition, alreadyProcessed))
          {
            thisDepthTransitions.add(transition);
            transition.addSteps(depthBuilder);
            for(Transition t : transition.getExecuteBefore())
            {
              nextTransitions.add(t);
            }
          }
          else
          {
            // no they have not => add for next iteration
            nextTransitions.add(transition);
          }
        }
      }
    }

    alreadyProcessed.addAll(thisDepthTransitions);

    addSteps(stepBuilder, type, nextTransitions, depth + 1, alreadyProcessed);
  }

  private boolean checkExecuteBefore(Transition transition, Set<Transition> alreadyProcessed)
  {
    for(Transition t : transition.getExecuteAfter())
    {
      if(!alreadyProcessed.contains(t))
        return false;
    }
    return true;
  }

  /**
   * Note that this method modifies the collection in place!
   */
  public static void filterVirtual(Collection<Transition> transitions)
  {

    for(Transition transition : transitions)
    {
      Iterator<Transition> iter = transition.getExecuteAfter().iterator();
      while(iter.hasNext())
      {
        if(iter.next().isVirtual())
          iter.remove();
      }

      iter = transition.getExecuteBefore().iterator();
      while(iter.hasNext())
      {
        if(iter.next().isVirtual())
          iter.remove();
      }
    }

    Iterator<Transition> iter = transitions.iterator();
    while(iter.hasNext())
    {
      Transition entry = iter.next();
      if(entry.isVirtual())
        iter.remove();
    }
  }

  /**
   * Optimizes the transitions by generating multi steps transitions. If the entire collection
   * can be optimized, then it returns a map where the key is the entryKey, otherwise it
   * returns <code>null</code>.
   * Note that this method modifies the collection in place!
   */
  public static Map<String, MultiStepsSingleEntryTransition> optimizeMultiSteps(Collection<Transition> transitions)
  {
    Map<String, MultiStepsSingleEntryTransition> multiSteps =
      new TreeMap<String, MultiStepsSingleEntryTransition>();

    Set<Transition> roots = findRoots(transitions, new HashSet<Transition>());

    for(Transition root : roots)
    {
      MultiStepsSingleEntryTransition mset = root.convertToMultiSteps();
      if(mset != null)
      {
        for(Transition transition : mset.getTransitions())
        {
          transitions.remove(transition);
        }
        transitions.add(mset);
      }
    }

    for(Transition transition : transitions)
    {
      if(transition instanceof MultiStepsSingleEntryTransition)
      {
        MultiStepsSingleEntryTransition mset = (MultiStepsSingleEntryTransition) transition;
        multiSteps.put(mset.getEntryKey(), mset);
      }
      else
      {
        return null;
      }
    }

    return multiSteps;
  }

  public static Set<Transition> findRoots(Collection<Transition> transitions, Set<Transition> roots)
  {
    for(Transition transition : transitions)
    {
      if(transition.isRoot())
      {
        roots.add(transition);
      }
    }

    return roots;
  }
}
