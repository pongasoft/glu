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

package org.linkedin.glu.orchestration.engine.delta.impl;

import org.linkedin.glu.provisioner.core.model.SystemEntry;
import org.linkedin.groovy.util.state.StateMachine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class ParentChildDeltaStateBuilder
{
  private final SingleDeltaBuilder _latestDelta;
  private final StateMachine _stateMachine;

  private SystemEntry _parent;
  private final Collection<SystemEntry> _children = new ArrayList<SystemEntry>();
  private final Set<String> _newFilteredKeys = new HashSet<String>();

  /**
   * Constructor
   */
  public ParentChildDeltaStateBuilder(SingleDeltaBuilder latestDelta,
                                      String parentKey)
  {
    _latestDelta = latestDelta;

    EntryDependencies expectedDependencies =
      _latestDelta.getSystemModelDelta().getExpectedDependencies();

    _parent = _latestDelta.getUnfilteredExpectedModel().findEntry(parentKey).clone();
    _stateMachine = _latestDelta.build().findAnyEntryDelta(_parent.getKey()).getStateMachine();

    Set<String> childrenKeys = expectedDependencies.findChildren(parentKey);
    for(String childKey : childrenKeys)
    {
      SystemEntry child = _latestDelta.getUnfilteredExpectedModel().findEntry(childKey).clone();
      _children.add(child);

    }
  }

  public void setState(String state)
  {
    if(state == null)
      setNullState();
    else
      setNonNullState(state);
  }

  /**
   * Handle <code>null</code> state differently (means undeploy)
   */
  private void setNullState()
  {
    if(!isFilteredOut(_parent.getKey()))
    {
      for(SystemEntry child : _children)
      {
        _newFilteredKeys.add(child.getKey());
      }
      _children.clear();
      _parent = null;
    }
    else
    {
      Iterator<SystemEntry> iter = _children.iterator();
      while(iter.hasNext())
      {
        SystemEntry child = iter.next();
        if(!isFilteredOut(child.getKey()))
        {
          iter.remove();
        }
      }
    }
  }

  private void setNonNullState(String state)
  {
    int maxChildrenDepth = -1;

    // 1. we set all the non filtered out children to 'state'
    for(SystemEntry child : _children)
    {
      if(!isFilteredOut(child.getKey()))
        setState(child, state);

      maxChildrenDepth = Math.max(maxChildrenDepth, computeDepth(child));
    }

    // 2. parent is filtered out => new parent state will be 'max' children state
    if(isFilteredOut(_parent.getKey()))
    {
      int parentDepth = computeDepth(_parent);
      if(parentDepth < maxChildrenDepth)
        setState(_parent, state);
    }
    else
    {
      // 3. parent is not filtered out => depth is set for parent which may adjust child state
      setState(_parent, state);

      int parentDepth = computeDepth(_parent);
      for(SystemEntry child : _children)
      {
        int childDepth = computeDepth(child);
        if(parentDepth < childDepth)
          setState(child, state);
      }
    }
  }

  public SystemEntry getParent()
  {
    return _parent;
  }

  public Collection<SystemEntry> getChildren()
  {
    return _children;
  }

  public Set<String> getNewFilteredKeys()
  {
    return _newFilteredKeys;
  }

  private void setState(SystemEntry systemEntry, String state)
  {
    systemEntry.setEntryState(state);
    _newFilteredKeys.add(systemEntry.getKey());
  }

  private int computeDepth(SystemEntry entry)
  {
    return SingleDeltaBuilder.computeDepth(_stateMachine, entry.getEntryState());
  }

  protected boolean isFilteredOut(String key)
  {
    return !_latestDelta.getFilteredKeys().contains(key);
  }
}
