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

import java.util.Set;

/**
 * Represent the dependencies between entries. Currently supporting only parent/child relationship.
 *
 * @author yan@pongasoft.com
 */
public interface EntryDependencies
{
  /**
   * @return the parent of the entry if there is any, <code>null</code> otherwise
   */
  String findParent(String key);
  Set<String> getEntriesWithParents();

  /**
   * @return the childen of the entry if there are any, empty collection otherwise
   */
  Set<String> findChildren(String key);
  Set<String> getEntriesWithChildren();

  /**
   * @return all the entries that have a dependency
   *         (<code>getEntriesWithParent() + getEntriesWithChildren()</code>)
   */
  Set<String> getEntriesWithDependency();

  /**
   * @param entriesWithDependency the set to use for output
   * @return all the entries that have a dependency
   *         (<code>getEntriesWithParent() + getEntriesWithChildren()</code>)
   *         (stored in <code>entriesWithDependency</code>)
   */
  Set<String> getEntriesWithDependency(Set<String> entriesWithDependency);
}