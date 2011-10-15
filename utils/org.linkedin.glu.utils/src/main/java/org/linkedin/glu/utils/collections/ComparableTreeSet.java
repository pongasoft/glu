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

package org.linkedin.glu.utils.collections;

import org.linkedin.util.lang.LangUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Extends a <code>TreeSet</code> to make it comparable
 *
 * @author yan@pongasoft.com
 */
public class ComparableTreeSet<T extends Comparable<T>> extends TreeSet<T> implements Comparable<ComparableTreeSet<T>>
{

  public ComparableTreeSet()
  {
  }

  public ComparableTreeSet(Comparator<? super T> comparator)
  {
    super(comparator);
  }

  public ComparableTreeSet(Collection<? extends T> ts)
  {
    super(ts);
  }

  public ComparableTreeSet(SortedSet<T> ts)
  {
    super(ts);
  }

  @Override
  public int compareTo(ComparableTreeSet<T> set)
  {
    // if sizes mismatch then it is easy
    int res = LangUtils.compare(size(), set.size());
    if(res != 0)
      return res;

    // we know they are the same size
    Iterator<T> thisValues = iterator();
    Iterator<T> otherValues = set.iterator();

    while(thisValues.hasNext())
    {
      res = LangUtils.compare(thisValues.next(), otherValues.next());
      if(res != 0)
        return res;
    }

    return 0;
  }
}
