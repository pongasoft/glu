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

import org.linkedin.glu.orchestration.engine.delta.SingleDeltaStatusInfo;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta;
import org.linkedin.glu.provisioner.core.model.SystemEntry;
import org.linkedin.groovy.util.state.StateMachine;
import org.linkedin.util.lang.LangUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class ParentChildDeltaBuilder
{
  private final SingleDeltaBuilder _delta;

  private final StateMachine _stateMachine;

  private final Set<String> _newFilteredKeys = new HashSet<String>();

  private final Set<String> _currentChildrenKeys;
  private final Set<String> _expectedChildrenKeys;

  private InternalSystemEntryDelta _parentDelta;

  /**
   * Constructor
   */
  public ParentChildDeltaBuilder(SingleDeltaBuilder delta,
                                 String parentKey)
  {
    _delta = delta;

    _parentDelta = _delta.createSystemEntryDelta(parentKey);
    _stateMachine = _parentDelta.getStateMachine();

    _currentChildrenKeys =
      _delta.getSystemModelDelta().getCurrentDependencies().findChildren(parentKey);
    _expectedChildrenKeys =
      _delta.getSystemModelDelta().getExpectedDependencies().findChildren(parentKey);

  }

  public Set<String> getNewFilteredKeys()
  {
    return _newFilteredKeys;
  }

  public SingleDeltaBuilder getDelta()
  {
    return _delta;
  }

  public StateMachine getStateMachine()
  {
    return _stateMachine;
  }

  public Set<String> getCurrentChildrenKeys()
  {
    return _currentChildrenKeys;
  }

  public Set<String> getExpectedChildrenKeys()
  {
    return _expectedChildrenKeys;
  }

  public InternalSystemEntryDelta getParentDelta()
  {
    return _parentDelta;
  }

  public void process()
  {
    processParent();
    processChildren();
  }

  private void processParent()
  {
    String expectedParentState = getExpectedParentState();

    // first we adjust the parent state based off children expected state
    String computedExpectedParentState = computeExpectedParentState();

    _parentDelta.setExpectedValue(SystemEntryDelta.ENTRY_STATE_KEY,
                                  computedExpectedParentState);
    _parentDelta = _delta.processSystemEntryDelta(_parentDelta);
    if(_parentDelta.isFilteredOut())
    {
      if(!LangUtils.isEqual(expectedParentState, computedExpectedParentState))
      {
        unfilter(_parentDelta);
      }
    }
  }

  private void processChildren()
  {
    boolean parentDelta = "delta".equals(_parentDelta.getDeltaStatus());

    // now process the current children
    for(String childKey : _currentChildrenKeys)
    {
      processChild(parentDelta, childKey);
    }

    // now process the expected children
    for(String childKey : _expectedChildrenKeys)
    {
      processChild(parentDelta, childKey);
    }
  }

  private void processChild(boolean parentDelta, String childKey)
  {
    // child has already been processed (reparenting case or already processed in current)
    if(_delta.getSystemModelDelta().findAnyEntryDelta(childKey) != null)
      return;

    InternalSystemEntryDelta delta = _delta.processEntry(childKey);
    if(parentDelta)
      adjustChildDeltaWhenParentDelta(delta);
    else
    {
      if(delta.isFilteredOut())
      {
        processFilteredOutChild(delta);
      }
    }
  }

  /**
   * parent expected state is at least max(children state)
   */
  private String computeExpectedParentState()
  {
    String expectedParentState = getExpectedParentState();

    int expectedParentStateDepth = computeDepth(expectedParentState);

    for(String childKey : _expectedChildrenKeys)
    {
      if(!isFilteredOut(childKey))
      {
        SystemEntry child =
          _delta.getSystemModelDelta().getExpectedSystemModel().findEntry(childKey);

        if(computeDepth(child) > expectedParentStateDepth)
          expectedParentState = child.getEntryState();
      }
    }

    return expectedParentState;
  }

  private InternalSystemEntryDelta processFilteredOutChild(InternalSystemEntryDelta childDelta)
  {
    if(computeDepth(childDelta) > computeDepth(_parentDelta))
    {
      childDelta.setExpectedValue(SystemEntryDelta.ENTRY_STATE_KEY,
                                  getExpectedState(_parentDelta));
      childDelta = _delta.processEntry(childDelta.getKey());
      unfilter(childDelta);
    }

    return childDelta;
  }

  private int computeDepth(String state)
  {
    if(state == null)
      return -1;

    return SingleDeltaBuilder.computeDepth(_stateMachine, state);
  }

  private int computeDepth(InternalSystemEntryDelta delta)
  {
    return computeDepth(getExpectedState(delta));
  }

  private int computeDepth(SystemEntry systemEntry)
  {
    if(systemEntry == null)
      return -1;
    return SingleDeltaBuilder.computeDepth(_stateMachine, systemEntry.getEntryState());
  }

  protected boolean isFilteredOut(String key)
  {
    return !_delta.getFilteredKeys().contains(key);
  }

  protected void adjustChildDeltaWhenParentDelta(InternalSystemEntryDelta childDelta)
  {
    if(!"delta".equals(childDelta.getDeltaStatus()))
    {
      childDelta.clearDeltaState();
      childDelta.setDeltaStatus("parentDelta");
      childDelta.setDeltaState(SystemEntryDelta.DeltaState.ERROR);
      childDelta.setDeltaStatusInfo(new SingleDeltaStatusInfo("needs redeploy (parent delta)"));
      unfilter(childDelta);
    }
  }

  private String getExpectedParentState()
  {
    return getExpectedState(_parentDelta);
  }

  private String getExpectedState(InternalSystemEntryDelta systemEntryDelta)
  {
    return systemEntryDelta.findExpectedValue(SystemEntryDelta.ENTRY_STATE_KEY);
  }

  private void unfilter(InternalSystemEntryDelta systemEntryDelta)
  {
    if(systemEntryDelta.isFilteredOut())
    {
      systemEntryDelta.setFilteredOut(false);
      _newFilteredKeys.add(systemEntryDelta.getKey());
    }
  }
}
