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

import org.linkedin.glu.orchestration.engine.delta.DeltaStatusInfo;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta;

/**
 * @author yan@pongasoft.com
 */
public interface InternalSystemEntryDelta extends SystemEntryDelta
{
  public boolean isNotFilteredOut();
  public boolean isFilteredOut();
  public void setFilteredOut(boolean isFilteredOut);

  void setErrorValue(String key);
  void clearErrorValue(String key);

  void setDeltaState(DeltaState deltaState);
  void setDeltaStatus(String status);
  void setDeltaStatusInfo(DeltaStatusInfo deltaStatusInfo);
  void clearDeltaState();

  /* value overriding */
  void setExpectedValue(String key, Object value);
  void setCurrentValue(String key, Object value);
  void setValue(String key, Object expectedValue, Object currentValue);
  void setValue(String key, Object value);
  void clearValue(String key);
}