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

import org.linkedin.util.lang.LangUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yan@pongasoft.com
 */
public class DeltaUtils
{
  public static class AscDeltaRowComparator implements Comparator<Comparable>
  {
    @Override
    public int compare(Comparable c1, Comparable c2)
    {
      return LangUtils.compare(c1, c2);
    }
  }

  public static class DescDeltaRowComparator implements Comparator<Comparable>
  {
    @Override
    public int compare(Comparable c1, Comparable c2)
    {
      return LangUtils.compare(c2, c1);
    }
  }

  public static Map<String, Comparator<Comparable>> delatRowsComparators =
    new HashMap<String, Comparator<Comparable>>();

  static
  {
    delatRowsComparators.put("asc", new AscDeltaRowComparator());
    delatRowsComparators.put("desc", new DescDeltaRowComparator());
  }

  /**
   * The purpose of this method is to sort the rows in the delta when 'summary' is turned off.
   *
   * Example:
   * <pre>
   *   c1 | c2 | c3
   *   ---+----+---
   *   1  | 2  | 3
   *   3  | 1  | 2
   * </pre>
   *
   * This 'table' would be represented as (groovy notation) (<code>list</code> param):
   *   <code>[[c1: 1, c2: 2, c3: 3], [c1: 3, c2: 1, c3: 2]]</code>
   *
   * <code>orderBy</code> would be represented as <code>[c1: 'asc', c2: 'desc', c3: null]</code>
   * which would mean: use <code>c1</code> in ascending order first and if they are equal then
   * use <code>c2</code> in descending order and ignore <code>c3</code>.
   *
   * @param list each element in the list is a row (a map where the key is the name of the column
   * and the value is the value of the cell)
   * @param orderBy is a map where the key is the name of the column and the value is defined in
   * <code>CustomDeltaColumnDefinition</code>. The order in which the map is iterated is *very*
   * important so it should obviously be of type <code>LinkedHashMap</code>...
   */
  @SuppressWarnings("unchecked")
  public static void sortBy(List<Map<String, ? extends Comparable>> list,
                            final Map<String, String> orderBy)
  {
    if(orderBy == null)
      return;

    Collections.sort(list, new Comparator<Map<String, ? extends Comparable>>()
    {
      @Override
      public int compare(Map<String, ? extends Comparable> m1, Map<String, ? extends Comparable> m2)
      {
        for(Map.Entry<String, String> obe: orderBy.entrySet())
        {
          String sortOrder = obe.getValue();

          if(sortOrder == null)
            continue;

          String name = obe.getKey();

          int compare = delatRowsComparators.get(sortOrder).compare(m1.get(name), m2.get(name));

          if(compare != 0)
            return compare;
        }

        // if we reach here it means all values were equal => 2 maps are equal
        return 0;
      }
    });
  }

  /**
   * Constructor
   */
  private DeltaUtils()
  {
  }
}
