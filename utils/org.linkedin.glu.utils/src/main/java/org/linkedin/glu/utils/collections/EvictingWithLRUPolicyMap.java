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

package org.linkedin.glu.utils.collections;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A map that will evict the least recently used element once the maximum number of elements
 * has been reached. Implementation is "trivial" as it is based of the
 * {@link LinkedHashMap#removeEldestEntry(Map.Entry)} hook.
 *
 * @author yan@pongasoft.com
 */
public class EvictingWithLRUPolicyMap<K, V> extends LinkedHashMap<K, V>
{
  private final int _maxElements;

  public EvictingWithLRUPolicyMap(int maxElements)
  {
    _maxElements = maxElements;
  }

  @Override
  protected boolean removeEldestEntry(Map.Entry<K, V> kvEntry)
  {
    return size() > _maxElements;
  }
}
