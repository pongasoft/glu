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

import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta;
import org.linkedin.glu.provisioner.core.model.SystemEntry;
import org.linkedin.glu.provisioner.core.model.SystemModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class MultiDeltaBuilder
{
  private final InternalDeltaProcessor _deltaProcessor;
  private final Collection<String> _toStates;
  private final SingleDeltaBuilder _originalDelta;

  private SingleDeltaBuilder _latestDelta;
  private Set<String> _newFilteredKeys;

  /**
   * Constructor
   */
  public MultiDeltaBuilder(InternalDeltaProcessor deltaProcessor,
                           SystemModel filteredExpectedModel,
                           SystemModel filteredCurrentModel,
                           Collection<String> toStates)
  {
    _deltaProcessor = deltaProcessor;
    _toStates = toStates;
    if(filteredExpectedModel != null && filteredCurrentModel != null)
      _originalDelta = new SingleDeltaBuilder(deltaProcessor,
                                              filteredExpectedModel,
                                              filteredCurrentModel);
    else
      _originalDelta = null;
  }

  public SingleDeltaBuilder getOriginalDelta()
  {
    return _originalDelta;
  }

  public SystemModel getOriginalFilteredExpectedModel()
  {
    return _originalDelta.getFilteredExpectedModel();
  }

  public SystemModel getOriginalFilteredCurrentModel()
  {
    return _originalDelta.getFilteredCurrentModel();
  }

  public SystemModel getOriginalUnfilteredExpectedModel()
  {
    return _originalDelta.getUnfilteredExpectedModel();
  }

  public SystemModel getOriginalUnfilteredCurrentModel()
  {
    return _originalDelta.getUnfilteredCurrentModel();
  }

  public SingleDeltaBuilder getLatestDelta()
  {
    return _latestDelta;
  }

  public Collection<SystemModelDelta> build()
  {
    if(_toStates == null)
      return null;

    if(getOriginalDelta() == null)
      return null;

    Collection<SystemModelDelta> deltas = new ArrayList<SystemModelDelta>(_toStates.size());

    _latestDelta = new SingleDeltaBuilder(_deltaProcessor,
                                          getOriginalUnfilteredCurrentModel(),
                                          getOriginalUnfilteredCurrentModel());
    _latestDelta.setFilteredKeys(getOriginalDelta().getFilteredKeys());

    for(String state : _toStates)
    {
      _newFilteredKeys =  new HashSet<String>(_latestDelta.getFilteredKeys());

      SystemModel newExpectedModel = createNewExpectedModel(state);

      _latestDelta =
        new SingleDeltaBuilder(_deltaProcessor,
                               newExpectedModel,
                               _latestDelta.getUnfilteredExpectedModel());

      _latestDelta.setFilteredKeys(_newFilteredKeys);

      SystemModelDelta delta = _latestDelta.build();

      deltas.add(delta);
    }

    return deltas;
  }

  /**
   * Creates the new expected model which will be in the given <code>state</code>.
   */
  private SystemModel createNewExpectedModel(String state)
  {
    if("<expected>".equals(state))
      return getOriginalUnfilteredExpectedModel().clone();

    SystemModel newExpectedModel = _latestDelta.getUnfilteredExpectedModel().cloneNoEntries();

    Set<String> filteredKeys = new HashSet<String>(_latestDelta.getFilteredKeys());

    for(String parentKey : _latestDelta.getParentKeys())
    {
      processParentChild(parentKey,
                         state,
                         newExpectedModel,
                         filteredKeys);
    }

    for(String entryKey : filteredKeys)
    {
      processEntry(state, entryKey, newExpectedModel);
    }
    
    return newExpectedModel;
  }

  private void processParentChild(String parentKey,
                                  String state,
                                  SystemModel newExpectedModel,
                                  Set<String> filteredKeys)
  {
    ParentChildDeltaStateBuilder pc = new ParentChildDeltaStateBuilder(_latestDelta, parentKey);

    pc.setState(state);

    if(pc.getParent() != null)
      newExpectedModel.addEntry(pc.getParent());
    filteredKeys.remove(parentKey);

    for(SystemEntry child : pc.getChildren())
    {
      newExpectedModel.addEntry(child);
      filteredKeys.remove(child.getKey());
    }

    _newFilteredKeys.addAll(pc.getNewFilteredKeys());
  }

  private void processEntry(String state, String entryKey, SystemModel newExpectedModel)
  {
    if(state == null)
      return;

    SystemEntry systemEntry = findSystemEntry(entryKey);
    systemEntry = systemEntry.clone();
    systemEntry.setEntryState(state);
    newExpectedModel.addEntry(systemEntry);
    _newFilteredKeys.add(entryKey);
  }

  private SystemEntry findSystemEntry(String entryKey)
  {
    SystemEntry systemEntry = _latestDelta.getUnfilteredExpectedModel().findEntry(entryKey);

    if(systemEntry == null)
      systemEntry = getOriginalUnfilteredExpectedModel().findEntry(entryKey);

    return systemEntry;
  }
}
