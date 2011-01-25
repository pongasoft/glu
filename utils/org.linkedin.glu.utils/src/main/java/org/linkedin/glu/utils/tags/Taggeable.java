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

import java.util.Collection;
import java.util.Set;

/**
 * Defines an entity that is taggeable. A tag is just a string. A taggeable entity can have an
 * unlimited number of tags.
 *
 * @author yan@pongasoft.com */
public interface Taggeable
{
  /**
   * @return the number of tags
   */
  int getSize();

  /**
   * @return <code>true</code> if there are no tags
   */
  boolean isEmpty();

  /**
   * @return the set of all tags
   */
  Set<String> getTags();

  /**
   * @return <code>true</code> if the given tag is present
   */
  boolean hasTag(String tag);

  /**
   * @return <code>true</code> if the given tags are present (all of them)
   */
  boolean hasAllTags(Collection<String> tags);

  /**
   * @return <code>true</code> if at least one of them is present
   */
  boolean hasAnyTag(Collection<String> tags);

  /**
   * Adds a tag.
   *
   * @return <code>true</code> if the tag which was added was a new tag, otherwise
   * <code>false</code>
   */
  boolean addTag(String tag);

  /**
   * Adds all the tags.
   *
   * @return the set of tags that were added (empty set if they were all already present)
   */
  Set<String> addTags(Collection<String> tags);

  /**
   * Removes the provided tag.
   *
   * @return <code>true</code> if the tag was removed or <code>false</code> if the tag was not
   * present in the first place
   */
  boolean removeTag(String tag);

  /**
   * Removes all the tags.
   *
   * @return the set of tags that were not present (empty set if they were all already present)
   */
  Set<String> removeTags(Collection<String> tags);
}