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

package org.linkedin.glu.agent.impl.storage

import org.linkedin.glu.utils.tags.FilteredTaggeable
import org.linkedin.glu.utils.tags.Taggeable
import org.linkedin.glu.utils.tags.TaggeableTreeSetImpl
import org.linkedin.glu.utils.tags.TagsSerializer

/**
 * @author yan@pongasoft.com */
public class TagsStorage extends FilteredTaggeable
{
  public static final TagsSerializer TAGS_SERIALIZER = TagsSerializer.INSTANCE

  private final WriteOnlyStorage _storage
  private final String _tagsAgentPropertyName
  
  TagsStorage(Storage storage,
              String tagsAgentPropertyName)
  {
    super(loadTags(storage, tagsAgentPropertyName))
    _storage = storage
    _tagsAgentPropertyName = tagsAgentPropertyName
  }

  @Override
  synchronized boolean addTag(String tag)
  {
    boolean res = super.addTag(tag)
    saveTags()
    return res
  }

  @Override
  synchronized Set<String> addTags(Collection<String> tags)
  {
    Set<String> res = super.addTags(tags)
    saveTags()
    return res
  }

  @Override
  synchronized boolean removeTag(String tag)
  {
    boolean res = super.removeTag(tag)
    saveTags()
    return res
  }

  @Override
  synchronized Set<String> removeTags(Collection<String> tags)
  {
    Set<String> res = super.removeTags(tags)
    saveTags()
    return res
  }

  @Override
  synchronized void setTags(Collection<String> tags)
  {
    super.setTags(tags)
    saveTags()
  }

  private void saveTags()
  {
    _storage.updateAgentProperty(_tagsAgentPropertyName, TAGS_SERIALIZER.serialize(tags))
  }

  private static Taggeable loadTags(Storage storage, String tagsAgentPropertyName)
  {
    AgentProperties agentProperties = storage.loadAgentProperties()
    String tags = agentProperties.getExposedProperty(tagsAgentPropertyName)?.toString() ?: ''
    return new TaggeableTreeSetImpl(TAGS_SERIALIZER.deserialize(tags))
  }

}