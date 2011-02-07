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
 * @author yan@pongasoft.com
 */
public class FilteredTaggeable implements Taggeable
{
  private final Taggeable _taggeable;

  /**
   * Constructor
   */
  public FilteredTaggeable(Taggeable taggeable)
  {
    _taggeable = taggeable;
  }

  public Taggeable getTaggeable()
  {
    return _taggeable;
  }

  @Override
  public int getTagsCount()
  {
    return _taggeable.getTagsCount();
  }

  @Override
  public boolean hasTags()
  {
    return _taggeable.hasTags();
  }

  @Override
  public Set<String> getTags()
  {
    return _taggeable.getTags();
  }

  @Override
  public boolean hasTag(String tag)
  {
    return _taggeable.hasTag(tag);
  }

  @Override
  public boolean hasAllTags(Collection<String> tags)
  {
    return _taggeable.hasAllTags(tags);
  }

  @Override
  public boolean hasAnyTag(Collection<String> tags)
  {
    return _taggeable.hasAnyTag(tags);
  }

  @Override
  public Set<String> getCommonTags(Collection<String> tags)
  {
    return _taggeable.getCommonTags(tags);
  }

  @Override
  public Set<String> getMissingTags(Collection<String> tags)
  {
    return _taggeable.getMissingTags(tags);
  }

  @Override
  public boolean addTag(String tag)
  {
    return _taggeable.addTag(tag);
  }

  @Override
  public Set<String> addTags(Collection<String> tags)
  {
    return _taggeable.addTags(tags);
  }

  @Override
  public boolean removeTag(String tag)
  {
    return _taggeable.removeTag(tag);
  }

  @Override
  public Set<String> removeTags(Collection<String> tags)
  {
    return _taggeable.removeTags(tags);
  }

  @Override
  public void setTags(Collection<String> tags)
  {
    _taggeable.setTags(tags);
  }
}
