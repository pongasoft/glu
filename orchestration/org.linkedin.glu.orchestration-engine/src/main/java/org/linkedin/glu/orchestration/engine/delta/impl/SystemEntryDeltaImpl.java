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

import org.linkedin.glu.agent.api.Agent;
import org.linkedin.glu.orchestration.engine.delta.DeltaStatusInfo;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryValue;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryValueNoDelta;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryValueWithDelta;
import org.linkedin.glu.orchestration.engine.planner.PlannerService;
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
public class SystemEntryDeltaImpl implements InternalSystemEntryDelta
{
  public static final StateMachine DEFAULT_STATE_MACHINE;
  public static final StateMachine SELF_UPGRADE_STATE_MACHINE;


  static
  {
    Map<String, Object> args = new HashMap<String, Object>();
    args.put("transitions", Agent.DEFAULT_TRANSITIONS);
    DEFAULT_STATE_MACHINE = new StateMachineImpl(args);

    args = new HashMap<String, Object>();
    args.put("transitions", Agent.SELF_UPGRADE_TRANSITIONS);
    SELF_UPGRADE_STATE_MACHINE = new StateMachineImpl(args);
  }

  private final SystemEntry _expectedEntry;
  private final SystemEntry _currentEntry;

  private final Map<String, SystemEntryValue> _values;
  private final Set<String> _errorValueKeys = new HashSet<String>();

  private boolean _isFilteredOut;

  /**
   * Constructor
   */
  public SystemEntryDeltaImpl(SystemEntry expectedEntry,
                              SystemEntry currentEntry,
                              boolean isFilteredOut)
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
    Object error = SystemModelDeltaImpl.getMetadataValue(_currentEntry, ERROR_KEY);
    if(error != null)
      _values.put(ERROR_KEY, new SystemEntryValueNoDelta<Object>(error));
    _isFilteredOut = isFilteredOut;
    // when the entry is filtered out, the 'expected' state should be the 'current' state
    if(_isFilteredOut)
      setValue(ENTRY_STATE_KEY, this.<Object>findCurrentValue(ENTRY_STATE_KEY));
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
      values.put(entry.getKey(), computeValue(entry.getValue(),
                                              currentValues.get(entry.getKey())));
    }

    for(Map.Entry<String, Object> entry : currentValues.entrySet())
    {
      if(!values.containsKey(entry.getKey()))
      {
        // expectedValue must be null otherwise it would be in previous loop!
        values.put(entry.getKey(), computeValue(null,
                                                entry.getValue()));
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

  private static SystemEntryValue computeValue(Object expectedValue, Object currentValue)
  {
    if(LangUtils.isEqual(expectedValue, currentValue))
    {
      return new SystemEntryValueNoDelta<Object>(expectedValue);
    }
    else
    {
      return new SystemEntryValueWithDelta<Object>(expectedValue, currentValue);
    }
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
    return findExpectedValue(ENTRY_STATE_KEY);
  }

  @Override
  public String getCurrentEntryState()
  {
    return findCurrentValue(ENTRY_STATE_KEY);
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

  @Override
  public void setErrorValue(String key)
  {
    if(findValueWithDelta(key) == null)
      throw new IllegalArgumentException(key + " does not reference a delta");

    _errorValueKeys.add(key);
  }

  @Override
  public void clearErrorValue(String key)
  {
    _errorValueKeys.remove(key);
  }

  @Override
  public SystemEntryValueWithDelta<String> findParentDelta()
  {
    return findValueWithDelta(PARENT_KEY);
  }

  @Override
  public SystemEntryValueWithDelta<String> findEntryStateDelta()
  {
    return findValueWithDelta(ENTRY_STATE_KEY);
  }

  @Override
  public <T> T findCurrentValue(String key)
  {
    SystemEntryValue<T> value = findValue(key);
    if(value != null)
      return value.getCurrentValue();
    else
      return null;
  }

  @Override
  public <T> T findExpectedValue(String key)
  {
    SystemEntryValue<T> value = findValue(key);
    if(value != null)
      return value.getExpectedValue();
    else
      return null;
  }

  @Override
  public Object getError()
  {
    return findValueWithNoDelta(ERROR_KEY);
  }

  @Override
  public DeltaState getDeltaState()
  {
    return findValueWithNoDelta(DELTA_STATE_KEY);
  }

  @Override
  public void setDeltaState(DeltaState deltaState)
  {
    setValue(DELTA_STATE_KEY, deltaState);
  }

  @Override
  public String getDeltaStatus()
  {
    return findValueWithNoDelta(DELTA_STATUS_KEY);
  }

  @Override
  public void setDeltaStatus(String status)
  {
    setValue(DELTA_STATUS_KEY, status);
  }

  @Override
  public DeltaStatusInfo getDeltaStatusInfo()
  {
    return findValueWithNoDelta(DELTA_STATUS_INFO_KEY);
  }

  @Override
  public void setDeltaStatusInfo(DeltaStatusInfo deltaStatusInfo)
  {
    setValue(DELTA_STATUS_INFO_KEY, deltaStatusInfo);
  }

  @Override
  public void clearDeltaState()
  {
    clearValue(DELTA_STATE_KEY);
    clearValue(DELTA_STATUS_KEY);
    clearValue(DELTA_STATUS_INFO_KEY);
  }

  @Override
  public void setValue(String key, Object value)
  {
    setValue(key, value, value);
  }

  @Override
  public void setExpectedValue(String key, Object value)
  {
    setValue(key, value, this.<Object>findCurrentValue(key));
  }

  @Override
  public void setCurrentValue(String key, Object value)
  {
    setValue(key, this.<Object>findExpectedValue(key), value);
  }

  @Override
  public void setValue(String key, Object expectedValue, Object currentValue)
  {
    _values.put(key, computeValue(expectedValue, currentValue));
  }

  @Override
  public void clearValue(String key)
  {
    _values.remove(key);
  }

  @Override
  public boolean isNotFilteredOut()
  {
    return !_isFilteredOut;
  }

  public boolean isFilteredOut()
  {
    return _isFilteredOut;
  }

  public void setFilteredOut(boolean isFilteredOut)
  {
    _isFilteredOut = isFilteredOut;
  }

  @Override
  public boolean isEmptyAgent()
  {
    return _currentEntry != null && _currentEntry.isEmptyAgent();
  }

  @Override
  public StateMachine getStateMachine()
  {
    String mountPoint = getMountPoint();
    if(mountPoint == null)
      return null;
    else
      return mountPoint.startsWith(PlannerService.AGENT_SELF_UPGRADE_MOUNT_POINT) ?
        SELF_UPGRADE_STATE_MACHINE :
        DEFAULT_STATE_MACHINE;
  }
}
