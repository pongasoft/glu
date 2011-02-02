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

package org.linkedin.glu.utils.tags;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is not thread safe.
 *
 * @author yan@pongasoft.com
 */
public class TagIndex
{
  private final Map<String, Set<String>> _indexByTag = new HashMap<String, Set<String>>();
  private final Map<String, Set<String>> _indexByKey = new HashMap<String, Set<String>>();

  /**
   * Constructor
   */
  public TagIndex()
  {
  }

  /**
   * Constructor
   */
  public TagIndex(TagIndex other)
  {
    for(Map.Entry<String, Set<String>> entry : other._indexByKey.entrySet())
    {
      add(entry.getKey(), entry.getValue());
    }
  }

  public void add(String key, Set<String> tags)
  {
    _indexByKey.put(key, tags);
    for(String tag : tags)
    {
      Set<String> keys = _indexByTag.get(tag);
      if(keys == null)
      {
        keys = new HashSet<String>();
        _indexByTag.put(tag, keys);
      }
      keys.add(key);
    }
  }

  public void remove(String key)
  {
    Set<String> tags = _indexByKey.remove(key);
    if(tags != null)
    {
      for(String tag : tags)
      {
        Set<String> keys = _indexByTag.get(tag);
        keys.remove(key);
        if(keys.isEmpty())
          _indexByTag.remove(tag);
      }
    }
  }

  public Set<String> getKeys()
  {
    return _indexByKey.keySet();
  }

  public Set<String> getTags()
  {
    return _indexByTag.keySet();
  }

  public Set<String> getKeys(String tag)
  {
    return _indexByTag.get(tag);
  }

  public Set<String> getTags(String key)
  {
    return _indexByKey.get(key);
  }
}
