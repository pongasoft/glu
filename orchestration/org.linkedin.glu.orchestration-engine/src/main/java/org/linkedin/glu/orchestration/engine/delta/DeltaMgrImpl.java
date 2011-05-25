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
import org.linkedin.util.lang.LangUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    SystemModelDeltaImpl systemModelDelta = new SystemModelDeltaImpl();

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

    return systemModelDelta;
  }

  private SystemEntryDelta computeSystemEntryDelta(SystemEntry expectedEntry,
                                                   SystemEntry currentEntry)
  {
    SystemEntryDeltaImpl sed = new SystemEntryDeltaImpl(expectedEntry, currentEntry);

    if(expectedEntry == null || currentEntry == null)
      return sed;

    // processing version mismatch
    processVersionMismatch(sed);

    // a chance to add custom processing
    processCustomDelta(sed);

    return sed;
  }

  protected void processVersionMismatch(SystemEntryDeltaImpl sed)
  {
    // 1. compute the set of keys taken into consideration for the delta
    Set<String> valueKeys = new HashSet<String>();
    addKeys(valueKeys, sed.getExpectedValues().keySet());
    addKeys(valueKeys, sed.getCurrentValues().keySet());

    for(String valueKey : valueKeys)
    {
      Object expectedValue = sed.findExpectedValue(valueKey);
      Object currentValue = sed.findCurrentValue(valueKey);
      if(!LangUtils.isEqual(expectedValue, currentValue))
      {
        sed.setValueDelta(new ValueDeltaImpl<Object>(valueKey, expectedValue, currentValue));
      }
    }
  }

  protected void addKeys(Set<String> allValueKeys, Set<String> valueKeys)
  {
    for(String key : valueKeys)
    {
      if(isKeyIncludedInVersionMismatch(key))
        allValueKeys.add(key);
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

    res = res && !_excludedInVersionMismatch.contains(key);

    return res;
  }

  /**
   * Nothing to do here. Subclasses can tweak the delta.
   *
   * @param sed system delta for the entry
   */
  protected void processCustomDelta(SystemEntryDeltaImpl sed)
  {
    // nothing to do in this implementation
  }
}
