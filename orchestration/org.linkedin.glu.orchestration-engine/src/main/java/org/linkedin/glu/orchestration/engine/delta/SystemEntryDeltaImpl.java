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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class SystemEntryDeltaImpl implements SystemEntryDelta
{
  private final SystemEntry _expectedEntry;
  private final SystemEntry _currentEntry;

  private final Map<String, Object> _expectedValues;
  private final Map<String, Object> _currentValues;

  private final Map<String, ValueDelta> _valueDeltas = new HashMap<String, ValueDelta>();

  /**
   * Constructor
   */
  @SuppressWarnings("unchecked")
  public SystemEntryDeltaImpl(SystemEntry expectedEntry, SystemEntry currentEntry)
  {
    if(expectedEntry == null && currentEntry == null)
      throw new IllegalArgumentException("at least one entry must not be null");

    if(expectedEntry != null &&
       currentEntry != null &&
       expectedEntry.getKey().equals(currentEntry.getKey()))
      throw new IllegalArgumentException("key mismatch");

    _expectedEntry = expectedEntry;
    _currentEntry = currentEntry;

    _expectedValues =
      _expectedEntry != null ? _expectedEntry.flatten() : Collections.<String, Object>emptyMap();

    _currentValues =
      _currentEntry != null ? _currentEntry.flatten() : Collections.<String, Object>emptyMap();
  }

  @Override
  public String getKey()
  {
    return _expectedEntry == null ? _currentEntry.getKey() : _expectedEntry.getKey();
  }

  @Override
  public SystemEntry getExpectedEntry()
  {
    return _expectedEntry;
  }

  @Override
  public SystemEntry getCurrentEntry()
  {
    return _currentEntry;
  }

  @Override
  public Set<String> getValueDeltaKeys()
  {
    return _valueDeltas.keySet();
  }

  <T> void setValueDelta(ValueDelta<T> valueDelta)
  {
    _valueDeltas.put(valueDelta.getKey(), valueDelta);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> ValueDelta<T> getValueDelta(String key)
  {
    return (ValueDelta<T>) _valueDeltas.get(key);
  }

  @Override
  public ValueDelta<String> getParentDelta()
  {
    return getValueDelta("parent");
  }

  @Override
  public ValueDelta<String> getEntryStateDelta()
  {
    return getValueDelta("entryState");
  }

  @Override
  public Map<String, Object> getExpectedValues()
  {
    return _expectedValues;
  }

  @Override
  public Object getExpectedValue(String key)
  {
    return _expectedValues.get(key);
  }

  @Override
  public Map<String, Object> getCurrentValues()
  {
    return _currentValues;
  }

  @Override
  public Object getCurrentValue(String key)
  {
    return _currentValues.get(key);
  }

  @Override
  public boolean hasDelta()
  {
    return !_valueDeltas.isEmpty();
  }
}
