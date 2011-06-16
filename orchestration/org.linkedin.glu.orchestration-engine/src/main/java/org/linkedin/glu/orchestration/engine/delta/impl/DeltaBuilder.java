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

import org.linkedin.glu.provisioner.core.model.SystemFilter;
import org.linkedin.glu.provisioner.core.model.SystemModel;

import java.util.HashSet;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class DeltaBuilder
{
  protected final SystemModel _filteredExpectedModel;
  protected final SystemModel _filteredCurrentModel;
  
  private Set<String> _filteredKeys;

  /**
   * Constructor
   */
  public DeltaBuilder(SystemModel filteredExpectedModel,
                      SystemModel filteredCurrentModel)
  {
    _filteredExpectedModel = filteredExpectedModel;
    _filteredCurrentModel = filteredCurrentModel;
  }

  public SystemModel getFilteredCurrentModel()
  {
    return _filteredCurrentModel;
  }

  public SystemModel getFilteredExpectedModel()
  {
    return _filteredExpectedModel;
  }

  public Set<String> getFilteredKeys()
  {
    if(_filteredKeys == null)
      _filteredKeys = computeFilteredKeys(_filteredExpectedModel, _filteredCurrentModel);
    
    return _filteredKeys;
  }

  public void setFilteredKeys(Set<String> filteredKeys)
  {
    _filteredKeys = filteredKeys;
  }

  /**
   * Compute the set of keys that will be part of the delta: the filters set on the model will
   * be used as filter
   */
  public static Set<String> computeFilteredKeys(SystemModel filteredExpectedModel,
                                                SystemModel filteredCurrentModel)
  {
    Set<String> keys = new HashSet<String>();

    SystemFilter currentModelFilters = filteredCurrentModel.getFilters();
    SystemFilter expectedModelFilters = filteredExpectedModel.getFilters();

    // 1. we make sure to exclude all entries from the current model where that were filtered
    // out from the expected model
    filteredCurrentModel = filteredCurrentModel.filterBy(expectedModelFilters);
    Set<String> currentKeys = filteredCurrentModel.getKeys(new HashSet<String>());

    // 2. we make sure to exclude all entries from the expected model where that were filtered
    // out from the current model
    filteredExpectedModel = filteredExpectedModel.filterBy(currentModelFilters);
    Set<String> expectedKeys = filteredExpectedModel.getKeys(new HashSet<String>());

    // if a filter was provided for the current model then we take only the keys from the
    // current model (excluding the one not in the expected model!)
    if(currentModelFilters != null)
    {
      for(String key : currentKeys)
      {
        if(expectedKeys.contains(key))
          keys.add(key);
      }
    }
    else
    {
      // otherwise we take keys from both
      keys.addAll(currentKeys);
      keys.addAll(expectedKeys);
    }

    return keys;
  }

}
