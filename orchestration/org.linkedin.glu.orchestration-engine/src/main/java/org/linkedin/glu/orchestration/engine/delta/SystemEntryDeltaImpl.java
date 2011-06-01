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
import org.linkedin.util.lang.LangUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author yan@pongasoft.com
 */
public class SystemEntryDeltaImpl implements SystemEntryDelta
{
  public static final StateMachine DEFAULT_STATE_MACHINE;

  static
  {
    Map<String, Object> args = new HashMap<String, Object>();
    args.put("transitions", Agent.DEFAULT_TRANSITIONS);
    DEFAULT_STATE_MACHINE = new StateMachineImpl(args);
  }

  private final SystemEntry _expectedEntry;
  private final SystemEntry _currentEntry;

  private final Map<String, SystemEntryValue> _values;
  private final Set<String> _errorValueKeys = new HashSet<String>();

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
    _values = computeValues(expectedEntry, currentEntry);
    Object error = SystemModelDeltaImpl.getMetadataValue(_currentEntry, "error");
    if(error != null)
      _values.put("error", new SystemEntryValueNoDelta<Object>(error));
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> computeValues(SystemEntry entry)
  {
    if(entry == null)
      return Collections.emptyMap();

    return entry.flatten();
  }

  private static Map<String, SystemEntryValue> computeValues(SystemEntry expectedEntry,
                                                             SystemEntry currentEntry)
  {
    Map<String, SystemEntryValue> values = new HashMap<String, SystemEntryValue>();

    Map<String, Object> expectedValues = computeValues(expectedEntry);
    Map<String, Object> currentValues = computeValues(currentEntry);


    for(Map.Entry<String, Object> entry : expectedValues.entrySet())
    {
      Object currentValue = currentValues.get(entry.getKey());
      if(LangUtils.isEqual(entry.getValue(), currentValue))
      {
        values.put(entry.getKey(), new SystemEntryValueNoDelta<Object>(currentValue));
      }
      else
      {
        values.put(entry.getKey(),
                   new SystemEntryValueWithDelta<Object>(entry.getValue(), currentValue));
      }
    }

    for(Map.Entry<String, Object> entry : currentValues.entrySet())
    {
      if(!values.containsKey(entry.getKey()))
      {
        Object expectedValue = null; // it must be null otherwise it would be in previous loop!
        if(LangUtils.isEqual(entry.getValue(), expectedValue))
        {
          values.put(entry.getKey(), new SystemEntryValueNoDelta<Object>(expectedValue));
        }
        else
        {
          values.put(entry.getKey(),
                     new SystemEntryValueWithDelta<Object>(expectedValue, entry.getValue()));
        }
      }
    }

    if(expectedEntry != null && expectedEntry.hasTags())
    {
      Set<String> tags = expectedEntry.getTags();
      values.put("tags", new SystemEntryValueNoDelta<Object>(new TreeSet<String>(tags)));
      for(String tag : tags)
      {
        values.put("tags." + tag, new SystemEntryValueNoDelta<String>(expectedEntry.getKey()));
      }
    }

    return values;
  }

  @Override
  public Map<String, SystemEntryValue> getValues()
  {
    return _values;
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
    return _expectedEntry == null ? _currentEntry.getMountPoint() : _expectedEntry.getMountPoint();
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
  public String getExpectedEntryState()
  {
    if(_expectedEntry != null)
      return _expectedEntry.getEntryState();
    else
      return null;
  }

  @Override
  public String getCurrentEntryState()
  {
    if(_currentEntry != null)
      return _currentEntry.getEntryState();
    else
      return null;
  }

  @Override
  public Set<String> getDeltaValueKeys()
  {
    Set<String> res = new HashSet<String>();

    for(Map.Entry<String, SystemEntryValue> entry : _values.entrySet())
    {
      if(entry.getValue().hasDelta())
        res.add(entry.getKey());
    }

    return res;
  }

  @Override
  public Set<String> getErrorValueKeys()
  {
    return _errorValueKeys;
  }

  @Override
  public boolean hasErrorDelta()
  {
    return !_errorValueKeys.isEmpty();
  }

  @Override
  public <T> SystemEntryValueWithDelta<T> findErrorValue(String key)
  {
    if(_errorValueKeys.contains(key))
      return findValueWithDelta(key);
    else
      return null;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> SystemEntryValue<T> findValue(String key)
  {
    return _values.get(key);
  }

  @Override
  public <T> SystemEntryValueWithDelta<T> findValueWithDelta(String key)
  {
    SystemEntryValue<T> value = findValue(key);
    if(value instanceof SystemEntryValueWithDelta)
      return (SystemEntryValueWithDelta<T>) value;
    else
      return null;
  }

  @Override
  public <T> T findValueWithNoDelta(String key)
  {
    SystemEntryValue<T> value = findValue(key);
    if(value instanceof SystemEntryValueNoDelta)
      return value.getExpectedValue();
    else
      return null;
  }

  void setErrorValue(String key)
  {
    if(findValueWithDelta(key) == null)
      throw new IllegalArgumentException(key + " does not reference a delta");

    _errorValueKeys.add(key);
  }

  void clearErrorValue(String key)
  {
    _errorValueKeys.remove(key);
  }

  @Override
  public SystemEntryValueWithDelta<String> findParentDelta()
  {
    return findValueWithDelta("parent");
  }

  @Override
  public SystemEntryValueWithDelta<String> findEntryStateDelta()
  {
    return findValueWithDelta("entryState");
  }

  @Override
  public Object findCurrentValue(String key)
  {
    SystemEntryValue<Object> value = findValue(key);
    if(value != null)
      return value.getCurrentValue();
    else
      return null;
  }

  @Override
  public Object findExpectedValue(String key)
  {
    SystemEntryValue<Object> value = findValue(key);
    if(value != null)
      return value.getExpectedValue();
    else
      return null;
  }

  @Override
  public Object getError()
  {
    return findValueWithNoDelta("error");
  }

  @Override
  public State getState()
  {
    return findValueWithNoDelta("state");
  }

  public void setState(State state)
  {
    setValue("state", state);
  }

  @Override
  public String getStatus()
  {
    return findValueWithNoDelta("status");
  }

  public void setStatus(String status)
  {
    setValue("status", status);
  }

  @Override
  public StatusInfo getStatusInfo()
  {
    return findValueWithNoDelta("statusInfo");
  }

  public void setStatusInfo(StatusInfo statusInfo)
  {
    setValue("statusInfo", statusInfo);
  }

  public void setValue(String key, Object value)
  {
    _values.put(key, new SystemEntryValueNoDelta<Object>(value));
  }

  @Override
  public StateMachine getStateMachine()
  {
    return DEFAULT_STATE_MACHINE;
  }
}
