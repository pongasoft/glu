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

package org.linkedin.glu.groovy.utils.test

import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.groovy.util.collections.IgnoreTypeComparator

/**
 * @author yan@pongasoft.com */
public class GluGroovyTestUtils
{
  /**
   * The main issue when comparing 2 maps is that if the type of the map is different then the 2
   * maps are different even if their content is the same... this method simply compares the
   * content of the maps.
   */
  static void assertEqualsIgnoreType(def testCase, String message = null, Map map1, Map map2)
  {
    checkAndFail(testCase, message, {
      testCase.assertEquals(JsonUtils.prettyPrint(map1), JsonUtils.prettyPrint(map2))
      if(map1 == null)
        testCase.assertNull("map1 is null, map2 is not", map2)

      testCase.assertEquals("map size differ ${map1.size()} != ${map2?.size()}", map1.size(), map2?.size())

      map1.each { k, v ->
        testCase.assertTrue("map2 does not contain key ${k}", map2.containsKey(k))
        assertEqualsIgnoreType(testCase, "map value for key ${k} mismatch", v, map2[k])
      }
    })
  }

  /**
   * The main issue when comparing 2 lists is that if the type of the list is different
   * then the 2 lists are different even if their content is the same... this method simply
   * compares the content of the lists
   */
  static void assertEqualsIgnoreType(def testCase, String message = null, List list1, List list2)
  {
    checkAndFail(testCase, message, {
      testCase.assertEquals(JsonUtils.prettyPrint(list1), JsonUtils.prettyPrint(list2))
      if(list1 == null)
        testCase.assertNull("list1 is null, list2 is not", list2)

      testCase.assertEquals("list size differ ${list1.size()} != ${list2?.size()}", list1.size(), list2?.size())

      def iterator = list2.iterator()

      list1.eachWithIndex { e, idx ->
        assertEqualsIgnoreType(testCase, "list index ${idx} mismatch", e, iterator.next())
      }
    })
  }

  /**
   * The main issue when comparing 2 sets is that if the type of the list is different
   * then the 2 sets are different even if their content is the same... this method simply
   * compares the content of the sets
   */
  static void assertEqualsIgnoreType(def testCase, String message = null, Set set1, Set set2)
  {
    checkAndFail(testCase, message, {
      testCase.assertEquals(JsonUtils.prettyPrint(set1), JsonUtils.prettyPrint(set2))
      if(set1 == null)
        testCase.assertNull("set1 is null, set2 is not", set2)

      testCase.assertEquals("set size differ ${set1.size()} != ${set2?.size()}", set1.size(), set2?.size())

      assertEqualsIgnoreType(testCase,
                             message,
                             set1.sort(IgnoreTypeComparator.INSTANCE),
                             set2.sort(IgnoreTypeComparator.INSTANCE))
    })
  }

  /**
   * The main issue when comparing 2 collections is that if the type of the collection is different
   * then the 2 collections are different even if their content is the same... this method simply
   * compares the content of the collections
   */
  static void assertEqualsIgnoreType(def testCase, String message = null, Collection c1, Collection c2)
  {
    assertEqualsIgnoreType(testCase, message, c1?.asList(), c2?.asList())
  }

  /**
   * This method is being used for recursing purposes
   */
  static void assertEqualsIgnoreType(def testCase, String message = null, Object o1, Object o2)
  {
    testCase.assertEquals(message, o1, o2)
  }

  static void checkAndFail(def testCase, String message, Closure closure)
  {
    try
    {
      closure()
    }
    catch(Throwable th)
    {
      if(message)
      {
        try
        {
          testCase.fail(message)
        }
        catch(Throwable failureError)
        {
          failureError.initCause(th)
          throw failureError
        }
      }
      throw th
    }
  }

  /**
   * It seems that timing can vary by up to 1 second... due to os precision...
   */
  public static void checkTimeDifference(def testCase, long time1, long time2)
  {
    testCase.assertTrue(Math.abs(time1 - time2) <= 1000);
  }

}