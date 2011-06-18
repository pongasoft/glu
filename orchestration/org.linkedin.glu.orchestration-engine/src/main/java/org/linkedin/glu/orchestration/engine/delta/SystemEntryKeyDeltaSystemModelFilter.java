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

import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class SystemEntryKeyDeltaSystemModelFilter implements DeltaSystemModelFilter
{
  private final Set<String> _entryKeys;

  /**
   * Constructor
   */
  public SystemEntryKeyDeltaSystemModelFilter(Set<String> entryKeys)
  {
    _entryKeys = entryKeys;
  }

  public Set<String> getEntryKeys()
  {
    return _entryKeys;
  }

  @Override
  public boolean filter(SystemEntry expectedEntry, SystemEntry currentEntry)
  {
    if(expectedEntry != null)
      return _entryKeys.contains(expectedEntry.getKey());
    else
      return _entryKeys.contains(currentEntry.getKey());
  }
}
