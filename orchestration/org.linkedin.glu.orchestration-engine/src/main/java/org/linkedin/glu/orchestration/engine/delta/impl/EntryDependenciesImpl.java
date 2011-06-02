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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * YP Implementation note: this class is not thread safe but it is ok because it is meant to be
 * initialized and then not modified.
 * 
 * @author yan@pongasoft.com
 */
public class EntryDependenciesImpl implements EntryDependencies
{
  private static final Set<String> NO_CHILDREN =
    Collections.unmodifiableSet(Collections.<String>emptySet());

  private final Map<String, String> _parents = new HashMap<String, String>();
  private final Map<String, Set<String>> _children = new HashMap<String, Set<String>>();

  /**
   * Constructor
   */
  public EntryDependenciesImpl()
  {
  }

  @Override
  public String findParent(String key)
  {
    return _parents.get(key);
  }

  @Override
  public Set<String> getEntriesWithParents()
  {
    return _parents.keySet();
  }

  @Override
  public Set<String> findChildren(String key)
  {
    Set<String> res = _children.get(key);
    if(res == null)
      res = NO_CHILDREN;
    return res;
  }

  @Override
  public Set<String> getEntriesWithChildren()
  {
    return _children.keySet();
  }

  @Override
  public Set<String> getEntriesWithDependency()
  {
    return getEntriesWithDependency(new HashSet<String>());
  }

  @Override
  public Set<String> getEntriesWithDependency(Set<String> entriesWithDependency)
  {
    if(entriesWithDependency == null)
      return null;

    entriesWithDependency.addAll(getEntriesWithParents());
    entriesWithDependency.addAll(getEntriesWithChildren());
    return entriesWithDependency;
  }

  public void setParent(String key, String parent)
  {
    if(_parents.containsKey(key))
      throw new IllegalStateException("parent already set for " + key);

    _parents.put(key, parent);
    
    addChild(parent, key);
  }

  private void addChild(String key, String child)
  {
    Set<String> children = _children.get(key);
    if(children == null)
    {
      children = new HashSet<String>();
      _children.put(key, children);
    }
    children.add(child);
  }
}
