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

import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryValue;
import org.linkedin.glu.provisioner.core.model.MetadataProvider;
import org.linkedin.glu.provisioner.core.model.SystemModel;
import org.linkedin.glu.utils.core.Externable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class SystemModelDeltaImpl implements InternalSystemModelDelta
{
  private static final Collection<InternalSystemEntryDelta> NO_ENTRY_DELTA_LIST =
    Collections.unmodifiableList(Collections.<InternalSystemEntryDelta>emptyList());

  private final Map<String, InternalSystemEntryDelta> _deltas =
    new HashMap<String, InternalSystemEntryDelta>();

  private final SystemModel _expectedSystemModel;
  private final SystemModel _currentSystemModel;

  private EntryDependencies _expectedDependencies;
  private EntryDependencies _currentDependencies;

  /**
   * Constructor
   */
  public SystemModelDeltaImpl(SystemModel expectedSystemModel, SystemModel currentSystemModel)
  {
    if(!expectedSystemModel.getFabric().equals(currentSystemModel.getFabric()))
      throw new IllegalArgumentException("mismatch fabric");

    _expectedSystemModel = expectedSystemModel;
    _currentSystemModel = currentSystemModel;
  }

  @Override
  public String getFabric()
  {
    return _expectedSystemModel.getFabric();
  }

  public SystemModel getExpectedSystemModel()
  {
    return _expectedSystemModel;
  }

  public SystemModel getCurrentSystemModel()
  {
    return _currentSystemModel;
  }

  public Map<String, SystemEntryDelta> getEntryDeltas()
  {
    Map<String, SystemEntryDelta> res = new HashMap<String, SystemEntryDelta>();
    for(InternalSystemEntryDelta entryDelta : _deltas.values())
    {
      if(entryDelta.isNotFilteredOut())
        res.put(entryDelta.getKey(), entryDelta);
    }
    return res;
  }

  @Override
  public Set<String> getKeys()
  {
    return getKeys(new HashSet<String>());
  }

  @Override
  public <T extends Collection<String>> T getKeys(T keys)
  {
    if(keys == null)
      return null;

    for(InternalSystemEntryDelta entryDelta : _deltas.values())
    {
      if(entryDelta.isNotFilteredOut())
        keys.add(entryDelta.getKey());
    }
    return keys;
  }

  @Override
  public SystemEntryDelta findEntryDelta(String key)
  {
    InternalSystemEntryDelta entryDelta = _deltas.get(key);
    if(entryDelta != null && entryDelta.isNotFilteredOut())
      return entryDelta;
    else
      return null;
  }

  @Override
  public InternalSystemEntryDelta findAnyEntryDelta(String key)
  {
    return _deltas.get(key);
  }

  @Override
  public void setEntryDelta(InternalSystemEntryDelta delta)
  {
    if(delta != null)
      _deltas.put(delta.getKey(), delta);
  }

  @Override
  public boolean hasErrorDelta()
  {
    for(InternalSystemEntryDelta delta : _deltas.values())
    {
      if(!delta.isFilteredOut() && delta.hasErrorDelta())
        return true;
    }
    
    return false;
  }

  @Override
  public Set<String> getEmptyAgents()
  {
    Set<String> emptyAgents = new HashSet<String>();
    Collection<String> currentModelEmptyAgent = getMetadataValue(_currentSystemModel, "emptyAgents");
    if(currentModelEmptyAgent != null)
      emptyAgents.addAll(currentModelEmptyAgent);
    for(InternalSystemEntryDelta entryDelta : _deltas.values())
    {
      if(entryDelta.isNotFilteredOut())
        emptyAgents.remove(entryDelta.getAgent());
    }
    return emptyAgents;
  }

  @Override
  public Map<String, Map<String, Object>> flatten(Map<String, Map<String, Object>> flattenInto)
  {
    if(flattenInto == null)
      return null;

    for(InternalSystemEntryDelta entryDelta : _deltas.values())
    {
      if(entryDelta.isNotFilteredOut())
      {
        boolean isInError = entryDelta.getDeltaState() == SystemEntryDelta.DeltaState.ERROR;

        Map<String, Object> valueMap = new HashMap<String, Object>();
        for(Map.Entry<String, SystemEntryValue> entry : entryDelta.getValues().entrySet())
        {
          SystemEntryValue sev = entry.getValue();
          Object value = sev.getCurrentValue();
          if(value == null || isInError)
          {
            if(sev.getExpectedValue() != null)
              value = sev.getExpectedValue();
          }
          if(value instanceof Externable)
            value = ((Externable) value).toExternalRepresentation();
          valueMap.put(entry.getKey(), value);
        }
        flattenInto.put(entryDelta.getKey(), valueMap);
      }
    }

    return flattenInto;
  }

  @Override
  public void setExpectedDependencies(EntryDependencies expectedDependencies)
  {
    _expectedDependencies = expectedDependencies;
  }

  @Override
  public void setCurrentDependencies(EntryDependencies currentDependencies)
  {
    _currentDependencies = currentDependencies;
  }

  @Override
  public InternalSystemEntryDelta findExpectedParentEntryDelta(String key)
  {
    return findAnyEntryDelta(_expectedDependencies.findParent(key));
  }

  @Override
  public InternalSystemEntryDelta findCurrentParentEntryDelta(String key)
  {
    return findAnyEntryDelta(_currentDependencies.findParent(key));
  }

  @Override
  public Set<String> getParentKeys(Set<String> keys)
  {
    if(keys == null)
      return null;
    keys.addAll(_expectedDependencies.getEntriesWithChildren());
    keys.addAll(_currentDependencies.getEntriesWithChildren());
    return keys;
  }

  @Override
  public Collection<InternalSystemEntryDelta> findExpectedChildrenEntryDelta(String key)
  {
    return findChildrenEntryDelta(_expectedDependencies, key);
  }

  @Override
  public Collection<InternalSystemEntryDelta> findCurrentChildrenEntryDelta(String key)
  {
    return findChildrenEntryDelta(_currentDependencies, key);
  }

  private Collection<InternalSystemEntryDelta> findChildrenEntryDelta(EntryDependencies dependencies,
                                                                      String key)
  {
    Set<String> children = dependencies.findChildren(key);
    if(children.isEmpty())
      return NO_ENTRY_DELTA_LIST;
    else
    {
      Collection<InternalSystemEntryDelta> res =
        new ArrayList<InternalSystemEntryDelta>(children.size());
      for(String child : children)
      {
        res.add(findAnyEntryDelta(child));
      }
      return res;
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T getMetadataValue(MetadataProvider metadaProvider, String key)
  {
    if(metadaProvider == null)
      return null;

    Map<String, Object> metadata = metadaProvider.getMetadata();
    if(metadata == null)
      return null;

    return (T) metadata.get(key);
  }
}
