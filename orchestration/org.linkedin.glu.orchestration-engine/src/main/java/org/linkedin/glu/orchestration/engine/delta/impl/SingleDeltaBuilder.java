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

import org.linkedin.glu.orchestration.engine.delta.DeltaSystemModelFilter;
import org.linkedin.glu.provisioner.core.model.SystemEntry;
import org.linkedin.glu.provisioner.core.model.SystemFilter;
import org.linkedin.glu.provisioner.core.model.SystemModel;
import org.linkedin.groovy.util.state.StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class SingleDeltaBuilder
{
  public static final String MODULE = SingleDeltaBuilder.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private final InternalDeltaProcessor _deltaProcessor;
  private final InternalSystemModelDelta _systemModelDelta;
  private boolean _systemModelDeltaBuilt = false;

  private final SystemModel _filteredExpectedModel;
  private final SystemModel _filteredCurrentModel;
  private final DeltaSystemModelFilter _deltaSystemModelFilter;

  private SystemModel _unfilteredExpectedModel;
  private SystemModel _unfilteredCurrentModel;

  private Set<String> _filteredKeys;
  private Set<String> _deltaKeys;
  private Set<String> _parentKeys;
  private HashSet<String> _nonEmptyAgents;

  /**
   * Constructor
   */
  public SingleDeltaBuilder(InternalDeltaProcessor deltaProcessor,
                            SystemModel filteredExpectedModel,
                            SystemModel filteredCurrentModel,
                            DeltaSystemModelFilter deltaSystemModelFilter)
  {
    _filteredExpectedModel = filteredExpectedModel;
    _filteredCurrentModel = filteredCurrentModel;

    _deltaProcessor = deltaProcessor;
    _deltaSystemModelFilter = deltaSystemModelFilter;
    if(_filteredExpectedModel != null && _filteredCurrentModel != null)
    {
      SystemModelDeltaImpl delta =
        new SystemModelDeltaImpl(filteredExpectedModel, filteredCurrentModel);
      delta.setExpectedDependencies(computeDependencies(getUnfilteredExpectedModel()));
      delta.setCurrentDependencies(computeDependencies(getUnfilteredCurrentModel()));
      _systemModelDelta = delta;
    }
    else
      _systemModelDelta = null;
  }

  public SystemModel getFilteredCurrentModel()
  {
    return _filteredCurrentModel;
  }

  public SystemModel getFilteredExpectedModel()
  {
    return _filteredExpectedModel;
  }

  public DeltaSystemModelFilter getDeltaSystemModelFilter()
  {
    return _deltaSystemModelFilter;
  }

  public Set<String> getFilteredKeys()
  {
    if(_filteredKeys == null)
      _filteredKeys = computeFilteredKeys(_filteredExpectedModel,
                                          _filteredCurrentModel,
                                          _deltaSystemModelFilter);

    return _filteredKeys;
  }

  public void setFilteredKeys(Set<String> filteredKeys)
  {
    _filteredKeys = filteredKeys;
  }

  public InternalDeltaProcessor getDeltaProcessor()
  {
    return _deltaProcessor;
  }

  public InternalSystemModelDelta getSystemModelDelta()
  {
    return _systemModelDelta;
  }

  public SystemModel getUnfilteredExpectedModel()
  {
    if(_unfilteredExpectedModel == null)
      _unfilteredExpectedModel = getFilteredExpectedModel().unfilter();
    return _unfilteredExpectedModel;
  }

  public SystemModel getUnfilteredCurrentModel()
  {
    if(_unfilteredCurrentModel == null)
      _unfilteredCurrentModel = getFilteredCurrentModel().unfilter();
    return _unfilteredCurrentModel;
  }

  /**
   * The method {@link #getFilteredKeys()} return the keys that are part of the result. With
   * parent/child relationship, some keys may need to be added back (parent and/or children) in
   * order to compute the proper deltas. This method will contain all the keys necessary fo compute
   * the delta.
   * @return delta key set
   */
  public Set<String> getDeltaKeys()
  {
    if(_deltaKeys == null)
    {
      _deltaKeys = new HashSet<String>(getFilteredKeys());
      getSystemModelDelta().getExpectedDependencies().getEntriesWithDependency(_deltaKeys);
      getSystemModelDelta().getCurrentDependencies().getEntriesWithDependency(_deltaKeys);
    }
    return _deltaKeys;
  }

  /**
   * @return all the parent keys (note that it will be a subset of {@link #getDeltaKeys()}, and
   * not ALL parents)
   */
  public Set<String> getParentKeys()
  {
    if(_parentKeys == null)
    {
      _parentKeys = getSystemModelDelta().getParentKeys(new HashSet<String>());
    }
    return _parentKeys;
  }

  public void setParentKeys(Set<String> parentKeys)
  {
    _parentKeys = parentKeys;
  }

  /**
   * Builds and return the delta
   */
  public InternalSystemModelDelta build()
  {
    if(_systemModelDelta == null)
      return null;

    if(_systemModelDeltaBuilt)
      return _systemModelDelta;

    _nonEmptyAgents = new HashSet<String>();

    Set<String> filteredKeys = new HashSet<String>(getFilteredKeys());

    for(String parentKey : getParentKeys())
    {
      processParentChild(parentKey, filteredKeys);
    }

    for(String entryKey : filteredKeys)
    {
      processEntry(entryKey);
    }

    _systemModelDelta.removeNonEmptyAgents(_nonEmptyAgents);

    _systemModelDeltaBuilt = true;

    return _systemModelDelta;
  }

  protected void processParentChild(String parentKey, Set<String> filteredKeys)
  {
    ParentChildDeltaBuilder pcb = new ParentChildDeltaBuilder(this, parentKey);

    pcb.process();

    getFilteredKeys().addAll(pcb.getNewFilteredKeys());

    filteredKeys.removeAll(pcb.getCurrentChildrenKeys());
    filteredKeys.removeAll(pcb.getExpectedChildrenKeys());
    filteredKeys.remove(parentKey);
  }

  protected InternalSystemEntryDelta createSystemEntryDelta(String entryKey)
  {
    SystemEntry expectedEntry = getUnfilteredExpectedModel().findEntry(entryKey);
    SystemEntry currentEntry = getUnfilteredCurrentModel().findEntry(entryKey);

    if(currentEntry != null && currentEntry.isEmptyAgent() && expectedEntry == null)
    {
      expectedEntry = currentEntry;
    }

    InternalSystemEntryDelta delta =
      new SystemEntryDeltaImpl(expectedEntry,
                               currentEntry,
                               !getFilteredKeys().contains(entryKey));

    if(!delta.isEmptyAgent())
      _nonEmptyAgents.add(delta.getAgent());

    return delta;
  }


  protected InternalSystemEntryDelta processEntry(String entryKey)
  {
    InternalSystemEntryDelta delta = createSystemEntryDelta(entryKey);
    return processSystemEntryDelta(delta);
  }

  protected InternalSystemEntryDelta processSystemEntryDelta(InternalSystemEntryDelta delta)
  {
    delta = _deltaProcessor.processSystemEntryDelta(delta);
    _systemModelDelta.setEntryDelta(delta);
    return delta;
  }

  protected EntryDependencies computeDependencies(SystemModel model)
  {
    EntryDependenciesImpl dependencies = new EntryDependenciesImpl();

    for(String key : getFilteredKeys())
    {
      SystemEntry entry = model.findEntry(key);
      if(entry != null && !SystemEntry.DEFAULT_PARENT.equals(entry.getParent()))
      {
        SystemEntry parentEntry = model.findEntry(entry.getAgent(), entry.getParent());
        if(parentEntry == null)
        {
          log.warn("model does not contain parent (" + entry.getParent() + ") for " + key);
        }
        else
        {
          dependencies.setParent(key, parentEntry.getKey());
        }
      }

      Collection<String> children = model.findChildrenKeys(key);
      if(children != null)
      {
        for(String childKey : children)
        {
          dependencies.setParent(childKey, key);
        }
      }
    }

    return dependencies;
  }

  public static int computeDepth(StateMachine stateMachine, String state)
  {
    if(state == null)
      return -1;
    else
      return stateMachine.getDepth(state);
  }

  /**
   * Compute the set of keys that will be part of the delta: the filters set on the model will
   * be used as filter
   */
  public static Set<String> computeFilteredKeys(SystemModel filteredExpectedModel,
                                                SystemModel filteredCurrentModel,
                                                DeltaSystemModelFilter deltaSystemModelFilter)
  {
    Set<String> allKeys = new HashSet<String>();

    SystemFilter currentModelFilters = filteredCurrentModel.getFilters();
    SystemFilter expectedModelFilters = filteredExpectedModel.getFilters();

    // 1. we make sure to exclude all entries from the current model where that were filtered
    // out from the expected model
    filteredCurrentModel = filteredCurrentModel.filterBy(expectedModelFilters);
    filteredCurrentModel.getKeys(allKeys);

    // 2. we make sure to exclude all entries from the expected model where that were filtered
    // out from the current model
    filteredExpectedModel = filteredExpectedModel.filterBy(currentModelFilters);
    filteredExpectedModel.getKeys(allKeys);

    // if a filter was provided we use it
    if(deltaSystemModelFilter != null)
    {
      Set<String> filteredKeys = new HashSet<String>();
      for(String entryKey : allKeys)
      {
        if(deltaSystemModelFilter.filter(filteredExpectedModel.findEntry(entryKey),
                                         filteredCurrentModel.findEntry(entryKey)))
        {
          filteredKeys.add(entryKey);
        }
      }
      allKeys = filteredKeys;
    }

    return allKeys;
  }

}
