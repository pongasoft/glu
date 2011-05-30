/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

import org.linkedin.groovy.util.collections.GroovyCollectionsUtils
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.glu.utils.tags.TaggeableTreeSetImpl
import org.linkedin.glu.utils.tags.ReadOnlyTaggeable

/**
 * @author ypujante@linkedin.com */
class SystemEntry implements ReadOnlyTaggeable
{
  public static final String DEFAULT_ENTRY_STATE = "running";
  public static final String DEFAULT_PARENT = "/";

  String agent
  String mountPoint
  def script
  String entryState
  String parent // optional
  def initParameters = [:] // optional
  def actionArgs = [:] // optional
  def metadata = [:] // optional
  volatile ReadOnlyTaggeable entryTags = ReadOnlyTaggeable.EMPTY // optional

  String getKey()
  {
    return "${agent}:${mountPoint}".toString()
  }

  def toExternalRepresentation()
  {
    def res =
    [
      agent: agent,
      mountPoint: mountPoint,
      script: script
    ]

    if(entryState)
      res.entryState = entryState

    if(parent && parent != DEFAULT_PARENT)
      res.parent = parent

    if(initParameters)
      res.initParameters = initParameters

    if(metadata)
      res.metadata = metadata

    if(actionArgs)
      res.actionArgs = actionArgs

    if(hasTags())
      res.tags = tags

    return res
  }

  String getEntryState()
  {
    if(!entryState)
      return DEFAULT_ENTRY_STATE
    else
      return entryState
  }

  String getParent()
  {
    if(!parent)
      return DEFAULT_PARENT
    else
      return parent
  }

  @Override
  int getTagsCount()
  {
    return entryTags.tagsCount
  }

  @Override
  boolean hasTags()
  {
    return entryTags.hasTags()
  }

  @Override
  Set<String> getTags()
  {
    return entryTags.getTags()
  }

  @Override
  boolean hasTag(String tag)
  {
    return entryTags.hasTag(tag)
  }

  @Override
  boolean hasAllTags(Collection<String> tags)
  {
    return entryTags.hasAllTags(tags)
  }

  @Override
  boolean hasAnyTag(Collection<String> tags)
  {
    return entryTags.hasAnyTag(tags)
  }

  @Override
  Set<String> getCommonTags(Collection<String> tags)
  {
    return entryTags.getCommonTags(tags)
  }

  @Override
  Set<String> getMissingTags(Collection<String> tags)
  {
    return entryTags.getMissingTags(tags)
  }

  void setTags(Collection<String> tags)
  {
    entryTags = new TaggeableTreeSetImpl(tags)
  }

  /**
   * @return a flattened version of the entry (a map with only one level)
   */
  Map flatten()
  {
    flatten([:])
  }

  /**
   * @param destMap the map to store the result
   * @return destMap
   */
  Map flatten(Map destMap)
  {
    def er = toExternalRepresentation()
    er.remove('tags')
    GroovyCollectionsUtils.flatten(er, destMap)
    destMap.key = key
    destMap.entryState = getEntryState() // not part of er if <code>null</code>
    return destMap
  }

  static SystemEntry fromExternalRepresentation(def er)
  {
    new SystemEntry(er)
  }

  boolean equals(o)
  {
    if(this.is(o)) return true;

    if(getClass() != o.class) return false;

    SystemEntry that = (SystemEntry) o;

    if(agent != that.agent) return false;
    if(initParameters != that.initParameters) return false;
    if(metadata != that.metadata) return false;
    if(mountPoint != that.mountPoint) return false;
    if(entryState != that.entryState) return false;
    if(parent != that.parent) return false;
    if(script != that.script) return false;
    if(entryTags != that.entryTags) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = (agent != null ? agent.hashCode() : 0);
    result = 31 * result + (mountPoint != null ? mountPoint.hashCode() : 0);
    result = 31 * result + (entryState != null ? entryState.hashCode() : 0);
    result = 31 * result + (parent != null ? parent.hashCode() : 0);
    result = 31 * result + (script != null ? script.hashCode() : 0);
    result = 31 * result + (initParameters != null ? initParameters.hashCode() : 0);
    result = 31 * result + (metadata != null ? metadata.hashCode() : 0);
    result = 31 * result + (entryTags != null ? metadata.hashCode() : 0);
    return result;
  }

  def String toString()
  {
    return JsonUtils.toJSON(toExternalRepresentation()).toString(2)
  }
}
