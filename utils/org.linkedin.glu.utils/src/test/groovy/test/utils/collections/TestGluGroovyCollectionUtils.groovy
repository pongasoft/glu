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

package test.utils.collections

import org.linkedin.glu.groovy.utils.collections.GluGroovyCollectionUtils

/**
 * @author yan@pongasoft.com */
public class TestGluGroovyCollectionUtils extends GroovyTestCase
{
  public void testSubMap()
  {
    assertNull(GluGroovyCollectionUtils.subMap(null, null))
    assertEquals([a: 1, b: 2, c: 3], GluGroovyCollectionUtils.subMap([a: 1, b: 2, c: 3], ['a', 'b', 'c']))
    assertEquals([a: 1, c: 3], GluGroovyCollectionUtils.subMap([a: 1, b: 2, c: 3], ['a', 'c']))
    assertEquals([a: 1, c: 3], GluGroovyCollectionUtils.subMap([a: 1, b: 2, c: 3], ['a', 'c', 'd']))
    assertEquals([:], GluGroovyCollectionUtils.subMap([a: 1, b: 2, c: 3], ['d']))
    assertEquals([:], GluGroovyCollectionUtils.subMap([a: 1, b: 2, c: 3], []))
    assertEquals([:], GluGroovyCollectionUtils.subMap([a: 1, b: 2, c: 3], null))
  }
}