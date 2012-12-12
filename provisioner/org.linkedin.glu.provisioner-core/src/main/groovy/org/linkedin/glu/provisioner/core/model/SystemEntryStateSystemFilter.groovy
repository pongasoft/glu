/*
 * Copyright (c) 2012 Yan Pujante
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

package org.linkedin.glu.provisioner.core.model

/**
 * @author yan@pongasoft.com  */
public class SystemEntryStateSystemFilter implements SystemFilter
{
  Set<String> states

  @Override
  def toExternalRepresentation()
  {
    return [s: states];
  }

  @Override
  String toDSL()
  {
    throw new RuntimeException("Not implemented yet")
  }

  @Override
  boolean filter(SystemEntry entry)
  {
    states.contains(entry?.entryState)
  }

  @Override
  String getKind()
  {
    return 's'
  }

  static SystemFilter create(String... states)
  {
    create(states as Set)
  }

  static SystemFilter create(Collection<String> states)
  {
    if(states == null || states.size() == 0)
      return null
    else
      new SystemEntryStateSystemFilter(states: states as Set)
  }
}