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

import org.linkedin.glu.orchestration.engine.delta.StatusInfo;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta;

/**
 * @author yan@pongasoft.com
 */
public interface InternalSystemEntryDelta extends SystemEntryDelta
{
  public boolean isPrimaryDelta();
  public boolean isDependentDelta();
  public void setDependentDelta(boolean isDependentDelta);

  void setErrorValue(String key);
  void clearErrorValue(String key);

  void setState(State state);
  void setStatus(String status);
  void setStatusInfo(StatusInfo statusInfo);

  void setValue(String key, Object value);
}