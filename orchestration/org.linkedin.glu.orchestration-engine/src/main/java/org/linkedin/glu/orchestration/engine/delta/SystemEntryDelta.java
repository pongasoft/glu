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

import java.util.Map;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public interface SystemEntryDelta
{
  String getKey();

  SystemEntry getExpectedEntry();
  SystemEntry getCurrentEntry();

  Set<String> getValueDeltaKeys();
  <T> ValueDelta<T> getValueDelta(String key);

  ValueDelta<String> getParentDelta();
  ValueDelta<String> getEntryStateDelta();

  Map<String, Object> getExpectedValues();
  Object getExpectedValue(String key);

  Map<String, Object> getCurrentValues();
  Object getCurrentValue(String key);

  boolean hasDelta();
}
