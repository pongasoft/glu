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

import org.linkedin.glu.agent.api.Agent;
import org.linkedin.glu.provisioner.core.model.SystemEntry;
import org.linkedin.groovy.util.state.StateMachine;
import org.linkedin.groovy.util.state.StateMachineImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class SystemEntryDeltaImpl implements SystemEntryDelta
{
  public static final StateMachine DEFAULT_STATE_MACHINE =
    new StateMachineImpl(Agent.DEFAULT_TRANSITIONS);

  private final SystemEntry _expectedEntry;
  private final SystemEntry _currentEntry;

  private final Map<String, Object> _expectedValues;
  private final Map<String, Object> _currentValues;

  private final Map<String, ValueDelta> _valueDeltas = new HashMap<String, ValueDelta>();

  /**
   * Constructor
   */
  public SystemEntryDeltaImpl(SystemEntry expectedEntry, SystemEntry currentEntry)
  {
    if(expectedEntry == null && currentEntry == null)
      throw new IllegalArgumentException("at least one entry must not be null");

    if(expectedEntry != null &&
       currentEntry != null &&
       !expectedEntry.getKey().equals(currentEntry.getKey()))
      throw new IllegalArgumentException("key mismatch");

    _expectedEntry = expectedEntry;
    _currentEntry = currentEntry;

    _expectedValues = computeValues(_expectedEntry);
    _currentValues = computeValues(_currentEntry);
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> computeValues(SystemEntry entry)
  {
    if(entry == null)
      return Collections.emptyMap();

    return entry.flatten();
  }

  @Override
  public String getKey()
  {
    return _expectedEntry == null ? _currentEntry.getKey() : _expectedEntry.getKey();
  }

  @Override
  public String getAgent()
  {
    return _expectedEntry == null ? _currentEntry.getAgent() : _expectedEntry.getAgent();
  }

  @Override
  public String getMountPoint()
  {
    return _expectedEntry == null ? _currentEntry.getMountPoint() : _expectedEntry.getAgent();
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
  public <T> ValueDelta<T> findValueDelta(String key)
  {
    return (ValueDelta<T>) _valueDeltas.get(key);
  }

  @Override
  public ValueDelta<String> findParentDelta()
  {
    return findValueDelta("parent");
  }

  @Override
  public ValueDelta<String> findEntryStateDelta()
  {
    return findValueDelta("entryState");
  }

  @Override
  public Map<String, Object> getExpectedValues()
  {
    return _expectedValues;
  }

  @Override
  public Object findExpectedValue(String key)
  {
    return _expectedValues.get(key);
  }

  @Override
  public Map<String, Object> getCurrentValues()
  {
    return _currentValues;
  }

  @Override
  public Object findCurrentValue(String key)
  {
    return _currentValues.get(key);
  }

  @Override
  public boolean hasDelta()
  {
    return !_valueDeltas.isEmpty();
  }

  @Override
  public StateMachine getStateMachine()
  {
    return DEFAULT_STATE_MACHINE;
  }
}
