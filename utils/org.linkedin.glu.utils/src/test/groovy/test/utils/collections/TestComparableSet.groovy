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

package test.utils.collections

import org.linkedin.glu.utils.collections.ComparableTreeSet

/**
 * @author yan@pongasoft.com */
public class TestComparableSet extends GroovyTestCase
{
  public void testComparableSet()
  {
    ComparableTreeSet s1 = tocs(['a', 'b', 'c'])
    ComparableTreeSet emptySet = tocs([])

    assertEquals(0, emptySet.compareTo(tocs([])))
    assertEquals(0, s1.compareTo(s1))
    assertEquals(1, s1.compareTo(emptySet))
    assertEquals(-1, emptySet.compareTo(s1))
    assertEquals(0, s1.compareTo(tocs(['a', 'b', 'c'])))
    assertEquals(-1, s1.compareTo(tocs(['a', 'z', 'c'])))
    assertEquals(0, s1.compareTo(tocs(['b', 'a', 'c'])))
  }

  static private ComparableTreeSet tocs(def collection)
  {
    new ComparableTreeSet(collection)
  }
}