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
import org.linkedin.groovy.util.state.StateMachine;

import java.util.Map;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public interface SystemEntryDelta
{
  enum State
  {
    OK,
    WARN,
    ERROR,
    NA
  }

  // YP note: status cannot be an enum as it prevents extension :(
//  enum Status
//  {
//    notDeployed,
//    unexpected,
//    notExpectedState,
//    expectedState,
//    delta,
//    error
//  }

  /**
   * @return the unique key (in the model) for this entry
   */
  String getKey();

  /**
   * Shortcut to get the agent
   */
  String getAgent();

  /**
   * Shortcut to get the mountpoint
   */
  String getMountPoint();

  /*******************************
   * Methods related to "expected"
   */
  SystemEntry getExpectedEntry();
  String getExpectedEntryState();
  Object findExpectedValue(String key);

  /*******************************
   * Methods related to "current"
   */
  SystemEntry getCurrentEntry();
  String getCurrentEntryState();
  Object findCurrentValue(String key);

  /**
   * @return all the values of this entry */
  Map<String, SystemEntryValue> getValues();

  /**
   * @return the value given the key (or <code>null</code> if no such value)
   */
  <T> SystemEntryValue<T> findValue(String key);

  /**
   * @return the value given the key (or <code>null</code> if no such value or not in delta)
   */
  <T> SystemEntryValueWithDelta<T> findValueWithDelta(String key);

  /**
   * @return the value given the key (or <code>null</code> if no such value or in delta). Note
   * that this call return the value itself (not the wrapper).
   */
  <T> T findValueWithNoDelta(String key);

  /**
   * @return all the keys where the value has a delta */
  Set<String> getDeltaValueKeys();

  /**
   * @return all the keys where the value has a delta which triggers an error  */
  Set<String> getErrorValueKeys();

  /**
   * This call will return a non <code>null</code> result iff there is a value which has a delta
   * part of the error or in other word if key belongs to the set returned by
   * {@link #getErrorValueKeys()}
   * @return <code>null</code> if no such key OR not an error value (potentially even if delta!)
   */
  <T> SystemEntryValueWithDelta<T> findErrorValue(String key);

  /**
   * @return <code>true</code> if this entry is in error because of a delta or in other word
   * this call will return <code>true</code> iff {@link #getErrorValueKeys()} is not empty
   */
  boolean hasErrorDelta();

  /**
   * @return the delta for parent (<code>null</code> if no delta!)
   */
  SystemEntryValueWithDelta<String> findParentDelta();

  /**
   * @return the delta for entryState (<code>null</code> if no delta!)
   */
  SystemEntryValueWithDelta<String> findEntryStateDelta();

  /**
   * @return the error if this entry is in error (from a state machine point of view)
   */
  Object getError();

  /**
   * Shortcut to get the state (equivalent to <code>findValue("state")?.expectedValue</code>)
   */
  State getState();

  /**
   * Shortcut to get the status (equivalent to <code>findValue("status")?.expectedValue</code>)
   */
  String getStatus();

  /**
   * Shortcut to get the statusInfo (equivalent to <code>findValue("statusInfo")?.expectedValue</code>)
   */
  StatusInfo getStatusInfo();

  /**
   * @return the state machine associated to this delta
   */
  StateMachine getStateMachine();
}
