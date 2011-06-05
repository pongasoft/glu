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
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemModelDelta;
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;
import org.linkedin.util.lang.LangUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public abstract class Transition
{
  private final String _key;

  private boolean _virtual = false;

  private Set<String> _executeAfter = new HashSet<String>();
  private Set<String> _executeBefore = new HashSet<String>();

  public static class TransitionComparator implements Comparator<Transition>
  {
    public static final TransitionComparator INSTANCE = new TransitionComparator();

    @Override
    public int compare(Transition t1, Transition t2)
    {
      return LangUtils.compare(t1.getKey(), t2.getKey());
    }
  }

  /**
   * Constructor
   */
  public Transition(String key)
  {
    _key = key;
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

  public void executeAfter(Transition transition)
  {
    if(transition != null)
    {
      _executeAfter.add(transition.getKey());
      transition._executeBefore.add(_key);
    }
  }

  public Set<String> getExecuteAfter()
  {
    return _executeAfter;
  }

  public String findSingleExecuteAfter()
  {
    if(_executeAfter.size() == 1)
      return _executeAfter.iterator().next();
    else
      return null;
  }

  public String findSingleExecuteBefore()
  {
    if(_executeBefore.size() == 1)
      return _executeBefore.iterator().next();
    else
      return null;
  }

  public Set<String> getExecuteBefore()
  {
    return _executeBefore;
  }

  public boolean isVirtual()
  {
    return _virtual;
  }

  public void setVirtual(boolean virtual)
  {
    _virtual = virtual;
  }

  public abstract void addSteps(ICompositeStepBuilder<ActionDescriptor> builder,
                                InternalSystemModelDelta systemModelDelta);

  public MultiStepsSingleEntryTransition convertToMultiSteps(Map<String, Transition> transitions)
  {
    return null;
  }

  protected Collection<Transition> collectLinearTransitions(String entryKey,
                                                            Map<String, Transition> transitions,
                                                            Collection<Transition> linearTransitions)
  {
    return null;
  }
}
