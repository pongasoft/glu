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

import org.linkedin.util.text.StringSplitter;

import java.util.Collection;

/**
 * @author yan@pongasoft.com
 */
public class TagsSerializer
{
  public static final TagsSerializer INSTANCE = new TagsSerializer();

  public static TagsSerializer instance()
  {
    return INSTANCE;
  }

  public static final String TAGS_SEPARATOR = ";";
  public static final StringSplitter STRING_SPLITTER = new StringSplitter(';');

  /**
   * Constructor
   */
  public TagsSerializer()
  {
  }

  public String serialize(ReadOnlyTaggeable tags)
  {
    if(tags == null)
      return null;
    
    return serialize(tags.getTags());
  }

  public String serialize(Collection<String> tags)
  {
    if(tags == null)
      return null;

    StringBuilder sb = new StringBuilder();

    for(String tag : tags)
    {
      if(sb.length() > 0)
        sb.append(TAGS_SEPARATOR);
      sb.append(tag);
    }

    return sb.toString();
  }

  public Collection<String> deserialize(String tags)
  {
    return STRING_SPLITTER.splitAsSet(tags);
  }
}
