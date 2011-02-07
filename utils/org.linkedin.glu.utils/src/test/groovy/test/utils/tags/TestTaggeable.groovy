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
import org.linkedin.glu.utils.tags.TagsSerializer
import org.linkedin.glu.utils.tags.ReadOnlyTaggeable
import org.linkedin.glu.utils.tags.TaggeableTreeSetImpl

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
    assertEquals(['fruit', 'vegetable'] as Set, taggeable.getMissingTags(['fruit', 'vegetable']))
    assertEquals([] as Set, taggeable.getCommonTags(['fruit', 'vegetable']))

    // + fruit
    assertTrue(taggeable.addTag('fruit'))
    assertFalse(taggeable.hasTags())
    assertEquals(1, taggeable.tagsCount)
    assertTrue(taggeable.hasTag('fruit'))
    assertFalse(taggeable.hasTag('vegetable'))
    assertFalse(taggeable.hasTag('rock'))
    assertFalse(taggeable.hasAllTags(['fruit', 'vegetable']))
    assertTrue(taggeable.hasAnyTag(['fruit', 'vegetable']))
    assertEquals(['vegetable'] as Set, taggeable.getMissingTags(['fruit', 'vegetable']))
    assertEquals(['fruit'] as Set, taggeable.getCommonTags(['fruit', 'vegetable']))

    // adding fruit again should not have any impact
    assertFalse(taggeable.addTag('fruit'))
    assertFalse(taggeable.hasTags())
    assertEquals(1, taggeable.tagsCount)
    assertTrue(taggeable.hasTag('fruit'))
    assertFalse(taggeable.hasTag('vegetable'))
    assertFalse(taggeable.hasTag('rock'))
    assertFalse(taggeable.hasAllTags(['fruit', 'vegetable']))
    assertTrue(taggeable.hasAnyTag(['fruit', 'vegetable']))
    assertEquals(['vegetable'] as Set, taggeable.getMissingTags(['fruit', 'vegetable']))
    assertEquals(['fruit'] as Set, taggeable.getCommonTags(['fruit', 'vegetable']))

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
    assertEquals(['fruit', 'vegetable'] as Set, taggeable.getCommonTags(['fruit', 'vegetable']))
    assertEquals(['fruit', 'vegetable'] as Set, taggeable.getCommonTags(['fruit', 'vegetable', 'rock']))
    assertEquals([] as Set, taggeable.getMissingTags(['fruit', 'vegetable']))
    assertEquals(['rock'] as Set, taggeable.getMissingTags(['fruit', 'vegetable', 'rock']))

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
    assertEquals(['vegetable'] as Set, taggeable.getCommonTags(['fruit', 'vegetable']))
    assertEquals(['vegetable'] as Set, taggeable.getCommonTags(['fruit', 'vegetable', 'rock']))
    assertEquals(['fruit'] as Set, taggeable.getMissingTags(['fruit', 'vegetable']))
    assertEquals(['fruit', 'rock'] as Set, taggeable.getMissingTags(['fruit', 'vegetable', 'rock']))
  }

  public void testSerialization()
  {
    TagsSerializer serializer = TagsSerializer.instance()

    // null testing
    assertNull(serializer.serialize((Collection<String>) null))
    assertNull(serializer.serialize((ReadOnlyTaggeable) null))
    assertNull(serializer.deserialize(null))

    // 0 entries
    assertEquals('', serializer.serialize([]))
    assertEquals('', serializer.serialize(new TaggeableTreeSetImpl([])))
    assertEquals([] as Set, serializer.deserialize(''))

    // 1 entry
    assertEquals('fruit', serializer.serialize(['fruit']))
    assertEquals('fruit', serializer.serialize(new TaggeableTreeSetImpl(['fruit'])))
    assertEquals(['fruit'] as Set, serializer.deserialize('fruit'))

    // 2 entries
    assertEquals('fruit;vegetable', serializer.serialize(['fruit', 'vegetable']))
    assertEquals('fruit;vegetable', serializer.serialize(new TaggeableTreeSetImpl(['vegetable', 'fruit'])))
    assertEquals(['fruit', 'vegetable'] as Set, serializer.deserialize('fruit;vegetable'))


  }
}