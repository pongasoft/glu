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

import org.linkedin.glu.provisioner.core.model.SystemModel;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public interface SystemModelDelta
{
  String getFabric();

  SystemModel getExpectedSystemModel();
  SystemModel getCurrentSystemModel();

  Set<String> getKeys();
  <T extends Collection<String>> T getKeys(T keys);
  SystemEntryDelta findEntryDelta(String key);
  Map<String, SystemEntryDelta> getEntryDeltas();

  /**
   * @return the set of empty agents (agents which do not have anything deployed on it as well
   *         as nothing to be deployed on) */
  Set<String> getEmptyAgents();

  /**
   * Flatten the delta in the provided map
   * @return <code>flattenInto</code>
   */
  Map<String, Map<String, Object>> flatten(Map<String, Map<String, Object>> flattenInto);

  /**
   * @return <code>true</code> if any entry has a delta
   */
  boolean hasErrorDelta();
}