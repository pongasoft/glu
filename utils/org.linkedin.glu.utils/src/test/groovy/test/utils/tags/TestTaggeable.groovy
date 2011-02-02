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

package test.utils.tags

import org.linkedin.glu.utils.tags.Taggeable
import org.linkedin.glu.utils.tags.TaggeableHashSetImpl

/**
 * @author yan@pongasoft.com */
public class TestTaggeable extends GroovyTestCase
{
  public void testTaggeableImpl()
  {
    Taggeable taggeable = new TaggeableHashSetImpl()

    // empty
    assertTrue(taggeable.hasTags())
    assertEquals(0, taggeable.tagsCount)
    assertFalse(taggeable.hasTag('fruit'))
    assertFalse(taggeable.hasTag('vegetable'))
    assertFalse(taggeable.hasTag('rock'))
    assertFalse(taggeable.hasAllTags(['fruit', 'vegetable']))
    assertFalse(taggeable.hasAnyTag(['fruit', 'vegetable']))

    // + fruit
    assertTrue(taggeable.addTag('fruit'))
    assertFalse(taggeable.hasTags())
    assertEquals(1, taggeable.tagsCount)
    assertTrue(taggeable.hasTag('fruit'))
    assertFalse(taggeable.hasTag('vegetable'))
    assertFalse(taggeable.hasTag('rock'))
    assertFalse(taggeable.hasAllTags(['fruit', 'vegetable']))
    assertTrue(taggeable.hasAnyTag(['fruit', 'vegetable']))

    // adding fruit again should not have any impact
    assertFalse(taggeable.addTag('fruit'))
    assertFalse(taggeable.hasTags())
    assertEquals(1, taggeable.tagsCount)
    assertTrue(taggeable.hasTag('fruit'))
    assertFalse(taggeable.hasTag('vegetable'))
    assertFalse(taggeable.hasTag('rock'))
    assertFalse(taggeable.hasAllTags(['fruit', 'vegetable']))
    assertTrue(taggeable.hasAnyTag(['fruit', 'vegetable']))

    // + vegetable
    assertEquals(['fruit'] as Set, taggeable.addTags(['vegetable', 'fruit']))
    assertFalse(taggeable.hasTags())
    assertEquals(2, taggeable.tagsCount)
    assertTrue(taggeable.hasTag('fruit'))
    assertTrue(taggeable.hasTag('vegetable'))
    assertFalse(taggeable.hasTag('rock'))
    assertTrue(taggeable.hasAllTags(['fruit', 'vegetable']))
    assertFalse(taggeable.hasAllTags(['fruit', 'vegetable', 'rock']))
    assertTrue(taggeable.hasAnyTag(['fruit', 'vegetable']))

    // - fruit
    assertEquals(['rock'] as Set, taggeable.removeTags(['fruit', 'rock']))
    assertFalse(taggeable.hasTags())
    assertEquals(1, taggeable.tagsCount)
    assertFalse(taggeable.hasTag('fruit'))
    assertTrue(taggeable.hasTag('vegetable'))
    assertFalse(taggeable.hasTag('rock'))
    assertFalse(taggeable.hasAllTags(['fruit', 'vegetable']))
    assertFalse(taggeable.hasAllTags(['fruit', 'vegetable', 'rock']))
    assertTrue(taggeable.hasAnyTag(['fruit', 'vegetable']))
  }
}