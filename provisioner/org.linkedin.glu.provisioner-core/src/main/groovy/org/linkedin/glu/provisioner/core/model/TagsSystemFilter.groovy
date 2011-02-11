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

package org.linkedin.glu.provisioner.core.model

/**
 * Filter by tags.
 *
 * @author yan@pongasoft.com */
public class TagsSystemFilter implements SystemFilter
{
  Collection<String> tags

  /**
   * Defines whether all tags should be present (<code>true</code>) or any of them
   * (<code>false</code>) in order to match
   */
  boolean allTags

  TagsSystemFilter(Collection<String> tags)
  {
    this(tags, true)
  }

  TagsSystemFilter(Collection<String> tags, boolean allTags)
  {
    this.tags = tags
    this.allTags = allTags
  }

  @Override def toExternalRepresentation()
  {
    return [(kind): toString() - 't']
  }

  @Override
  boolean filter(SystemEntry entry)
  {
    if(allTags)
    {
      return entry.hasAllTags(tags)
    }
    else
      return entry.hasAnyTag(tags)
  }

  @Override
  String getKind()
  {
    return 't'
  }

  String toString()
  {
    if(allTags)
      return "t(${tags.join(';')})".toString()
    else
      return "t[${tags.join(';')}]".toString()
  }
}