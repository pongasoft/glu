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
import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptorAdjuster;
import org.linkedin.glu.orchestration.engine.action.descriptor.InternalActionDescriptor;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemModelDelta;
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;
import org.linkedin.glu.provisioner.plan.api.LeafStep;
import org.linkedin.util.lang.LangUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public abstract class Transition
{
  private final SingleDeltaTransitionPlan _transitionPlan;
  private final String _key;

  private boolean _virtual = true;
  protected SkippableTransition _skipRootCause = null;

  private Set<Transition> _executeAfter = new HashSet<Transition>();
  private Set<Transition> _executeBefore = new HashSet<Transition>();

  public static class TransitionComparator implements Comparator<Transition>
  {
    public static final TransitionComparator INSTANCE = new TransitionComparator();

    @Override
    public int compare(Transition t1, Transition t2)
    {
      int res = LangUtils.compare(t1.getKey(), t2.getKey());
      if(res == 0)
      {
        res = LangUtils.compare(t1.getTransitionPlanSequenceNumber(),
                                t2.getTransitionPlanSequenceNumber());
      }
      return res;
    }
  }

  /**
   * Constructor
   */
  public Transition(SingleDeltaTransitionPlan transitionPlan, String key)
  {
    _transitionPlan = transitionPlan;
    _key = key;
  }

  public SingleDeltaTransitionPlan getTransitionPlan()
  {
    return _transitionPlan;
  }

  public int getTransitionPlanSequenceNumber()
  {
    return _transitionPlan.getSequenceNumber();
  }

  public InternalSystemModelDelta getSystemModelDelta()
  {
    return _transitionPlan.getSystemModelDelta();
  }

  public ActionDescriptorAdjuster getActionDescriptorAdjuster()
  {
    return _transitionPlan.getActionDescriptorAdjuster();
  }

  public String getFabric()
  {
    return getSystemModelDelta().getFabric();
  }

  public String getKey()
  {
    return _key;
  }

  @Override
  public String toString()
  {
    return getKey();
  }

  public boolean isRoot()
  {
    return _executeAfter.size() == 0;
  }

  public SkippableTransition getSkipRootCause()
  {
    return _skipRootCause;
  }

  public boolean shouldSkip()
  {
    return _skipRootCause != null;
  }

  public void skip(SkippableTransition rootCause)
  {
    if(shouldSkip())
      return;

    _skipRootCause = rootCause;

    for(Transition transition : _executeAfter)
    {
      transition.skip(rootCause);
    }

    for(Transition transition : _executeBefore)
    {
      transition.skip(rootCause);
    }
  }

  public void executeAfter(Transition transition)
  {
    if(transition != null)
    {
      if(transition.shouldSkip())
      {
        skip(transition.getSkipRootCause());
      }
      else
      {
        if(shouldSkip())
          transition.skip(getSkipRootCause());
      }

      _executeAfter.add(transition);
      transition._executeBefore.add(this);
    }
  }

  public Set<Transition> getExecuteAfter()
  {
    return _executeAfter;
  }

  public Transition findSingleExecuteAfter()
  {
    if(_executeAfter.size() == 1)
      return _executeAfter.iterator().next();
    else
      return null;
  }

  public Transition findSingleExecuteBefore()
  {
    if(_executeBefore.size() == 1)
      return _executeBefore.iterator().next();
    else
      return null;
  }

  public Set<Transition> getExecuteBefore()
  {
    return _executeBefore;
  }

  public boolean isVirtual()
  {
    return _virtual;
  }

  public void clearVirtual()
  {
    _virtual = false;
  }

  public abstract void addSteps(ICompositeStepBuilder<ActionDescriptor> builder);

  public MultiStepsSingleEntryTransition convertToMultiSteps()
  {
    return null;
  }

  protected Collection<Transition> collectLinearTransitions(String entryKey,
                                                            Collection<Transition> linearTransitions)
  {
    return null;
  }

  /**
   * Builds a leaf step
   */
  protected LeafStep<ActionDescriptor> buildStep(InternalActionDescriptor actionDescriptor)
  {
    actionDescriptor = getActionDescriptorAdjuster().adjustDescriptor(getSystemModelDelta(),
                                                                      actionDescriptor);

    if(actionDescriptor != null)
    {

      return new LeafStep<ActionDescriptor>(null,
                                            actionDescriptor.toMetadata(),
                                            actionDescriptor);
    }
    else
    {
      return null;
    }
  }

  protected <T extends InternalActionDescriptor> T populateActionDescriptor(T actionDescriptor)
  {
    actionDescriptor.setValue("fabric", getFabric());
    return actionDescriptor;
  }

  @Override
  public boolean equals(Object o)
  {
    if(this == o) return true;
    if(!(o instanceof Transition)) return false;

    Transition that = (Transition) o;

    if(!_key.equals(that._key)) return false;
    if(!_transitionPlan.equals(that._transitionPlan)) return false;

    return true;
  }

  @Override
  public int hashCode()
  {
    int result = _transitionPlan.hashCode();
    result = 31 * result + _key.hashCode();
    return result;
  }
}
