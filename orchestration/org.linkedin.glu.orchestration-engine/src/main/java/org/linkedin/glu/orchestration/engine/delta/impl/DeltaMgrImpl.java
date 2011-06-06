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

import org.linkedin.glu.orchestration.engine.delta.DeltaMgr;
import org.linkedin.glu.orchestration.engine.delta.MultipleDeltaStatusInfo;
import org.linkedin.glu.orchestration.engine.delta.SingleDeltaStatusInfo;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryValueWithDelta;
import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta;
import org.linkedin.glu.provisioner.core.model.SystemEntry;
import org.linkedin.glu.provisioner.core.model.SystemModel;
import org.linkedin.groovy.util.state.StateMachine;
import org.linkedin.util.annotations.Initializer;
import org.linkedin.util.lang.LangUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author yan@pongasoft.com
 */
public class DeltaMgrImpl implements DeltaMgr
{
  public static final String MODULE = DeltaMgrImpl.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private Set<String> _includedInVersionMismatch =
    new HashSet<String>(Arrays.asList("parent", "script", "entryState"));

  private Set<String> _excludedInVersionMismatch = null;

  /**
   * Constructor
   */
  public DeltaMgrImpl()
  {
  }

  public Set<String> getIncludedInVersionMismatch()
  {
    return _includedInVersionMismatch;
  }

  @Initializer
  public void setIncludedInVersionMismatch(Set<String> includedInVersionMismatch)
  {
    _includedInVersionMismatch = includedInVersionMismatch;
  }

  public Set<String> getExcludedInVersionMismatch()
  {
    return _excludedInVersionMismatch;
  }

  @Initializer
  public void setExcludedInVersionMismatch(Set<String> excludedInVersionMismatch)
  {
    _excludedInVersionMismatch = excludedInVersionMismatch;
  }

  @Override
  public SystemModelDelta computeDelta(SystemModel filteredExpectedModel,
                                       SystemModel filteredCurrentModel)
  {
    if(filteredExpectedModel == null || filteredCurrentModel == null)
      return null;

    InternalSystemModelDelta systemModelDelta = new SystemModelDeltaImpl(filteredExpectedModel,
                                                                         filteredCurrentModel);

    // 1. we get all the keys that will be part of the delta
    Set<String> filteredKeys = SystemModel.filterKeys(filteredExpectedModel,
                                                      filteredCurrentModel);

    // 2. we compute the dependencies (on the unfiltered model) which may bring back some keys
    // that were filtered out in step 1.
    SystemModel unfilteredExpectedModel = systemModelDelta.getExpectedSystemModel().unfilter();
    SystemModel unfilteredCurrentModel = systemModelDelta.getCurrentSystemModel().unfilter();

    EntryDependencies expectedDependencies =
      computeDependencies(filteredKeys, unfilteredExpectedModel);
    systemModelDelta.setExpectedDependencies(expectedDependencies);

    EntryDependencies currentDependencies =
      computeDependencies(filteredKeys, unfilteredCurrentModel);
    systemModelDelta.setCurrentDependencies(currentDependencies);

    // 3. keys required are all filtered keys + all dependency keys (which may or may not be present
    // in filteredKeys!)
    Set<String> requiredKeys = new HashSet<String>(filteredKeys);
    expectedDependencies.getEntriesWithDependency(requiredKeys);
    currentDependencies.getEntriesWithDependency(requiredKeys);

    // 4. we compute the delta for all entries pretending there is no dependencies
    for(String key : requiredKeys)
    {
      SystemEntry expectedEntry = unfilteredExpectedModel.findEntry(key);
      SystemEntry currentEntry = unfilteredCurrentModel.findEntry(key);
      if(currentEntry != null && currentEntry.isEmptyAgent() && expectedEntry == null)
      {
        expectedEntry = currentEntry;
      }
      InternalSystemEntryDelta delta =
        new SystemEntryDeltaImpl(expectedEntry,
                                 currentEntry,
                                 !filteredKeys.contains(key));

      computeSystemEntryDelta(delta);

      systemModelDelta.setEntryDelta(delta);
    }

    // 5. we adjust the delta based on the dependencies
    adjustDeltaFromDependencies(systemModelDelta);

    return systemModelDelta;
  }

  protected void adjustDeltaFromDependencies(InternalSystemModelDelta systemModelDelta)
  {
    // we start from the parents
    Set<String> parentKeys = systemModelDelta.getParentKeys(new TreeSet<String>());
    for(String key : parentKeys)
    {
      InternalSystemEntryDelta parentEntryDelta = systemModelDelta.findAnyEntryDelta(key);
      StateMachine stateMachine = parentEntryDelta.getStateMachine();

      Collection<InternalSystemEntryDelta> expectedChildrenEntryDelta =
        systemModelDelta.findExpectedChildrenEntryDelta(key);

      String parentExpectedState = parentEntryDelta.getExpectedEntryState();

      for(InternalSystemEntryDelta childEntryDelta : expectedChildrenEntryDelta)
      {
        int childDepth =
          stateMachine.getDepth(childEntryDelta.getExpectedEntryState());
        if(childDepth > stateMachine.getDepth(parentExpectedState))
          parentExpectedState = childEntryDelta.getExpectedEntryState();
      }

      adjustDeltaFromDependencies(parentEntryDelta, parentExpectedState);

      if("delta".equals(parentEntryDelta.getDeltaStatus()))
      {
        for(InternalSystemEntryDelta childDelta : expectedChildrenEntryDelta)
        {
          adjustChildDeltaWhenParentDelta(childDelta);
        }
        Collection<InternalSystemEntryDelta> currentChildrenEntryDelta =
          systemModelDelta.findCurrentChildrenEntryDelta(key);
        for(InternalSystemEntryDelta childDelta : currentChildrenEntryDelta)
        {
          adjustChildDeltaWhenParentDelta(childDelta);
        }
      }
    }
  }

  protected void adjustDeltaFromDependencies(InternalSystemEntryDelta systemEntryDelta,
                                             String expectedState)
  {
    if(!LangUtils.isEqual(expectedState, systemEntryDelta.getExpectedEntryState()))
    {
      systemEntryDelta.setExpectedValue(SystemEntryDelta.ENTRY_STATE_KEY, expectedState);
      systemEntryDelta.clearDeltaState();
      computeSystemEntryDelta(systemEntryDelta);
      systemEntryDelta.setFilteredOut(false);
    }
  }

  protected void adjustChildDeltaWhenParentDelta(InternalSystemEntryDelta childDelta)
  {
    if(!"delta".equals(childDelta.getDeltaStatus()))
    {
      childDelta.clearDeltaState();
      childDelta.setDeltaStatus("parentDelta");
      childDelta.setDeltaState(SystemEntryDelta.DeltaState.ERROR);
      childDelta.setDeltaStatusInfo(new SingleDeltaStatusInfo("needs redeploy (parent delta)"));
      childDelta.setFilteredOut(false);
    }
  }

  protected EntryDependencies computeDependencies(Set<String> keys, SystemModel model)
  {
    EntryDependenciesImpl dependencies = new EntryDependenciesImpl();

    for(String key : keys)
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

  protected InternalSystemEntryDelta computeSystemEntryDelta(InternalSystemEntryDelta sed)
  {
    // processing version mismatch
    processCustomDeltaPreVersionMismatch(sed);

    // processing version mismatch
    processVersionMismatch(sed);

    // a chance to add custom processing
    processCustomDeltaPostVersionMismatch(sed);

    return sed;
  }

  protected void processVersionMismatch(InternalSystemEntryDelta sed)
  {
    if(sed.getDeltaState() != null)
      return;

    for(String key : sed.getDeltaValueKeys())
    {
      if(isKeyIncludedInVersionMismatch(key))
        sed.setErrorValue(key);
    }

    // means that it is not deployed at all
    if(sed.getCurrentEntry() == null)
    {
      sed.setDeltaStatus("notDeployed");
      sed.setDeltaState(SystemEntryDelta.DeltaState.ERROR);
      sed.setDeltaStatusInfo(new SingleDeltaStatusInfo("NOT deployed"));
      return;
    }

    // means that it should not be deployed at all
    if(sed.getExpectedEntry() == null)
    {
      sed.setDeltaStatus("unexpected");
      sed.setDeltaState(SystemEntryDelta.DeltaState.ERROR);
      sed.setDeltaStatusInfo(new SingleDeltaStatusInfo("should NOT be deployed"));
      return;
    }

    // processing delta
    processDelta(sed);
  }

  /**
   * Set status/statusInfo/state for an entry delta (note that the delta has already been computed
   * and is accessible with {@link SystemEntryDelta#getErrorValueKeys()})
   */
  protected void processDelta(InternalSystemEntryDelta sed)
  {
    // give a chance for custom delta...
    processCustomDelta(sed);

    if(sed.getDeltaState() != null)
      return;

    // in case there is a delta
    if(sed.hasErrorDelta())
    {
      sed.setDeltaState(SystemEntryDelta.DeltaState.ERROR);

      Set<String> keys = sed.getErrorValueKeys();

      // when only a difference in state
      if(sed.findEntryStateDelta() != null && keys.size() == 1)
      {
        sed.setDeltaStatus("notExpectedState");
        sed.setDeltaStatusInfo(new SingleDeltaStatusInfo(sed.getExpectedEntryState() + "!=" +
                                                         sed.getCurrentEntryState()));
      }
      else
      {
        // handle it as a delta
        sed.setDeltaStatus("delta");
        keys = new TreeSet<String>(keys);
        Collection<String> values = new ArrayList<String>(keys.size());
        for(String key : keys)
        {
          SystemEntryValueWithDelta<Object> errorValue = sed.findErrorValue(key);
          values.add(key + ":[" + errorValue.getExpectedValue() + "!=" +
                     errorValue.getCurrentValue() + "]");

        }
        sed.setDeltaStatusInfo(MultipleDeltaStatusInfo.create(values));
      }
    }
    else
    {
      // when there is an error
      if(sed.getError() != null)
      {
        sed.setDeltaState(SystemEntryDelta.DeltaState.ERROR);
        sed.setDeltaStatus("error");
        sed.setDeltaStatusInfo(new SingleDeltaStatusInfo(sed.getError().toString()));
      }
      else
      {
        // empty agent
        if(sed.isEmptyAgent())
        {
          sed.setDeltaState(SystemEntryDelta.DeltaState.NA);
          sed.setDeltaStatus("NA");
          sed.setDeltaStatusInfo(new SingleDeltaStatusInfo("empty agent"));
        }
        else
        {
          // everything ok!
          sed.setDeltaState(SystemEntryDelta.DeltaState.OK);
          sed.setDeltaStatus("expectedState");
          sed.setDeltaStatusInfo(new SingleDeltaStatusInfo(sed.getExpectedEntryState()));
        }
      }
    }

  }

  /**
   * By default the key is included in the version mismatch iff starts with "initParameters." or
   * if it is in the {@link #_includedInVersionMismatch} set and it is not in the
   * {@link #_excludedInVersionMismatch}
   */
  protected boolean isKeyIncludedInVersionMismatch(String key)
  {
    boolean res = _includedInVersionMismatch != null  && _includedInVersionMismatch.contains(key);

    res = res || key.startsWith("initParameters.");

    res = res && (_excludedInVersionMismatch == null || !_excludedInVersionMismatch.contains(key));

    return res;
  }

  /**
   * Nothing to do here. Subclasses can tweak the delta. Note that if you set the state in this
   * method, then there won't be further processing, thus effectively bypassing the processing
   *
   * @param sed system delta for the entry
   */
  protected void processCustomDeltaPreVersionMismatch(InternalSystemEntryDelta sed)
  {
    // nothing to do in this implementation
  }

  /**
   * Nothing to do here. Subclasses can tweak the delta.
   *
   * @param sed system delta for the entry
   */
  protected void processCustomDeltaPostVersionMismatch(InternalSystemEntryDelta sed)
  {
    // nothing to do in this implementation
  }

  /**
   * Nothing to do here. Subclasses can tweak the delta.
   *
   * @param sed the delta has already been computed
   * and is accessible with {@link SystemEntryDelta#getErrorValueKeys()}
   */
  protected void processCustomDelta(InternalSystemEntryDelta sed)
  {
  }
}
