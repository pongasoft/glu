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

package org.linkedin.glu.orchestration.engine.delta;

import org.linkedin.glu.provisioner.core.model.SystemEntry;
import org.linkedin.glu.provisioner.core.model.SystemModel;
import org.linkedin.util.annotations.Initializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author yan@pongasoft.com
 */
public class DeltaMgrImpl implements DeltaMgr
{
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
  public SystemModelDelta computeDelta(SystemModel expectedModel, SystemModel currentModel)
  {
    if(expectedModel == null || currentModel == null)
      return null;

    SystemModelDeltaImpl systemModelDelta = new SystemModelDeltaImpl(expectedModel, currentModel);

    List<SystemModel> models = SystemModel.filter(expectedModel, currentModel);

    expectedModel = models.get(0);
    currentModel = models.get(1);

    Set<String> allKeys = new HashSet<String>();

    for(SystemEntry entry : expectedModel.findEntries())
    {
      allKeys.add(entry.getKey());
    }

    for(SystemEntry entry : currentModel.findEntries())
    {
      allKeys.add(entry.getKey());
    }

    for(String key : allKeys)
    {
      SystemEntryDelta delta =
        computeSystemEntryDelta(expectedModel.findEntry(key), currentModel.findEntry(key));
      
      systemModelDelta.setEntryDelta(delta);
    }

    computeEmptyAgentsDelta(systemModelDelta);

    return systemModelDelta;
  }

  private SystemEntryDelta computeSystemEntryDelta(SystemEntry expectedEntry,
                                                   SystemEntry currentEntry)
  {
    SystemEntryDeltaImpl sed = new SystemEntryDeltaImpl(expectedEntry, currentEntry);

    // processing version mismatch
    processCustomDeltaPreVersionMismatch(sed);

    // processing version mismatch
    processVersionMismatch(sed);

    // a chance to add custom processing
    processCustomDeltaPostVersionMismatch(sed);

    return sed;
  }

  protected void processVersionMismatch(SystemEntryDeltaImpl sed)
  {
    if(sed.getState() != null)
      return;

    for(String key : sed.getDeltaValueKeys())
    {
      if(isKeyIncludedInVersionMismatch(key))
        sed.setErrorValue(key);
    }

    // means that it is not deployed at all
    if(sed.getCurrentEntry() == null)
    {
      sed.setStatus("notDeployed");
      sed.setState(SystemEntryDelta.State.ERROR);
      sed.setStatusInfo(new SimpleStatusInfo("NOT deployed"));
      return;
    }

    // means that it should not be deployed at all
    if(sed.getExpectedEntry() == null)
    {
      sed.setStatus("unexpected");
      sed.setState(SystemEntryDelta.State.ERROR);
      sed.setStatusInfo(new SimpleStatusInfo("should NOT be deployed"));
      return;
    }

    // processing delta
    processDelta(sed);
  }

  /**
   * Set status/statusInfo/state for an entry delta (note that the delta has already been computed
   * and is accessible with {@link SystemEntryDelta#getErrorValueKeys()})
   */
  protected void processDelta(SystemEntryDeltaImpl sed)
  {
    // give a chance for custom delta...
    processCustomDelta(sed);

    if(sed.getState() != null)
      return;

    // in case there is a delta
    if(sed.hasErrorDelta())
    {
      sed.setState(SystemEntryDelta.State.ERROR);

      Set<String> keys = sed.getErrorValueKeys();

      // when only a difference in state
      if(sed.findEntryStateDelta() != null && keys.size() == 1)
      {
        sed.setStatus("notExpectedState");
        sed.setStatusInfo(new SimpleStatusInfo(sed.getExpectedEntryState() + "!=" +
                                               sed.getCurrentEntryState()));
      }
      else
      {
        // handle it as a delta
        sed.setStatus("delta");
        keys = new TreeSet<String>(keys);
        Collection<String> values = new ArrayList<String>(keys.size());
        for(String key : keys)
        {
          SystemEntryValueWithDelta<Object> errorValue = sed.findErrorValue(key);
          values.add(key + ":[" + errorValue.getExpectedValue() + "!=" +
                     errorValue.getCurrentValue() + "]");

        }
        sed.setStatusInfo(MultipleStatusInfo.create(values));
      }
    }
    else
    {
      // when there is an error
      if(sed.getError() != null)
      {
        sed.setState(SystemEntryDelta.State.ERROR);
        sed.setStatus("error");
        sed.setStatusInfo(new SimpleStatusInfo(sed.getError().toString()));
      }
      else
      {
        // everything ok!
        sed.setState(SystemEntryDelta.State.OK);
        sed.setStatus("expectedState");
        sed.setStatusInfo(new SimpleStatusInfo(sed.getExpectedEntryState()));
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
   * Processes all empty agent to add a 'fake' delta
   * @param systemModelDelta
   */
  protected void computeEmptyAgentsDelta(SystemModelDeltaImpl systemModelDelta)
  {
    for(String emptyAgent : systemModelDelta.getEmptyAgents())
    {
      // TODO HIGH YP:  add this back
      /*
  emptyAgents.each { agent ->
    def entry= [:]
    entry.agent = agent
    entry.entryState = 'NA'
    entry['metadata.currentState'] = 'NA'
    entry.status = 'NA'
    entry.state = 'NA'
    setTags(entry, expectedModel?.getAgentTags(agent)?.tags)
    entries << entry
  }
       */
    }
  }

  /**
   * Nothing to do here. Subclasses can tweak the delta. Note that if you set the state in this
   * method, then there won't be further processing, thus effectively bypassing the processing
   *
   * @param sed system delta for the entry
   */
  protected void processCustomDeltaPreVersionMismatch(SystemEntryDeltaImpl sed)
  {
    // nothing to do in this implementation
  }

  /**
   * Nothing to do here. Subclasses can tweak the delta.
   *
   * @param sed system delta for the entry
   */
  protected void processCustomDeltaPostVersionMismatch(SystemEntryDeltaImpl sed)
  {
    // nothing to do in this implementation
  }

  /**
   * Nothing to do here. Subclasses can tweak the delta.
   *
   * @param sed the delta has already been computed
   * and is accessible with {@link SystemEntryDelta#getErrorValueKeys()}
   */
  protected void processCustomDelta(SystemEntryDeltaImpl sed)
  {
  }
}
