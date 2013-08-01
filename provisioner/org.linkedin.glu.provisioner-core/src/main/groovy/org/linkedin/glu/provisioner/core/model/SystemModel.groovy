/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2013 Yan Pujante
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

import org.linkedin.glu.provisioner.core.model.builder.SystemModelBuilder
import org.linkedin.util.codec.CodecUtils
import org.linkedin.util.codec.OneWayCodec
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.codec.HexaCodec
import org.linkedin.util.lang.LangUtils
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils

import org.linkedin.glu.utils.tags.ReadOnlyTaggeable
import org.linkedin.glu.utils.tags.Taggeable
import org.linkedin.glu.utils.tags.TaggeableTreeSetImpl

/**
 * @author ypujante@linkedin.com  */
class SystemModel implements MetadataProvider
{
  public static final OneWayCodec SHA1 =
    OneWayMessageDigestCodec.createSHA1Instance('', HexaCodec.INSTANCE)

  private final Map<String, SystemEntry> _entries = new TreeMap()
  private final Map<String, Collection<String>> _children = new HashMap<String,Collection<String>>()
  private Map<String, Taggeable> _agentTags = new TreeMap<String, Taggeable>()

  String id
  String fabric

  Map<String, Object> metadata = [:]

  // contains a list of filters that have been applied to this model from the original (chain if
  // more than one)
  SystemFilter filters

  // when a filter is applied we keep a reference to the original unfiltered model
  private SystemModel _unfilteredModel = null

  /**
   * @return the metadata called 'name'
   */
  String getName()
  {
    metadata.name
  }

  /**
   * @return the sha1 of the content (does not include the id) */
  @Deprecated // use SystemModelRenderer.computeSystemId instead!
   String computeContentSha1()
  {
    // YP note: since glu 132 (use of jackson) the following code is left on purpose to use
    // org.json rather than the new jackson because of the fact that the output of prettyPrint
    // is different which would generate a different sha-1 for the same model!
    def ext = toExternalRepresentation()
    ext.remove('id') // we remove id from the computation
    def json = JsonUtils.toJSON(ext).toString(2)
    CodecUtils.encodeString(SHA1, json)
  }

  void addEntry(SystemEntry entry)
  {
    if(_entries.containsKey(entry.key))
    {
      throw new IllegalArgumentException("already defined entry ${entry.key}")
    }
    _entries[entry.key] = entry
    if(entry.parent != SystemEntry.DEFAULT_PARENT)
    {
      Collection<String> children = _children[entry.parentKey]
      if(children == null)
        children = new HashSet<String>()
      _children[entry.parentKey] = children
      children << entry.key
    }
    def agentTags = getAgentTags(entry.agent)
    if(!entry.entryTags.hasAllTags(agentTags.tags))
    {
      entry.tags = entry.entryTags.tags + agentTags.tags
    }
  }

  void updateEntry(SystemEntry entry)
  {
    removeEntry(entry.key)
    addEntry(entry)
  }

  void removeEntry(String key)
  {
    _entries.remove(key)
  }

  SystemEntry findEntry(String agent, String mountPoint)
  {
    findEntry("${agent}:${mountPoint}".toString())
  }

  SystemEntry findEntry(String key)
  {
    _entries[key]
  }

  Collection<String> findChildrenKeys(String parentKey)
  {
    _children[parentKey]
  }

  Collection<SystemEntry> findEntries()
  {
    return _entries.values().collect { it }
  }

  public <T extends Collection<String>> T getKeys(T keys)
  {
    if(keys == null)
      return null

    _entries.keySet().each { keys << it }

    return keys
  }

  def computeStats()
  {
    return computeStats(null)
  }

  /**
   * Computes the stats of this model limited by the set of keys provided.
   * 
   * @return a map containing as the key, the name of the stat and the value is a counter (note that
   * when the value is unique, then the unique value is returned instead of the counter)
   */
  def computeStats(def keys)
  {
    def stats = [:]

    _entries.values().each { SystemEntry se ->
      def map = se.flatten()

      se.tags?.each { String tag ->
        map["tags.${tag}".toString()] = se.key
      }

      (keys ?: map.keySet()).each { k ->
        if(map.containsKey(k))
        {
          def values = stats[k]
          if(values == null)
          {
            values = [] as Set
            stats[k] = values
          }

          values << map[k]
        }
      }
    }

    GroovyCollectionsUtils.collectKey(stats, [:]) { k,v ->
      if(v.size() == 1)
        return v.iterator().next()
      else
        return v.size()
    }
  }

  /**
   * Adds the agent tags
   */
  void addAgentTags(String agentName, Collection<String> tags)
  {
    if(_entries)
    {
      throw new IllegalStateException("currently unsupported operation: add the tags, then the entries")
    }
    
    Taggeable taggeable = _agentTags[agentName]
    if(!taggeable)
    {
      taggeable = new TaggeableTreeSetImpl(tags)
      _agentTags[agentName] = taggeable
    }
    else
    {
      taggeable.addTags(tags)
    }
  }

  /**
   * @return the tags of the given agent
   */
  ReadOnlyTaggeable getAgentTags(String agentName)
  {
    return _agentTags[agentName] ?: ReadOnlyTaggeable.EMPTY
  }
  
  /**
   * @return all the agent tags
   */
  Map<String, ? extends ReadOnlyTaggeable> getAgentTags()
  {
    return _agentTags
  }

  /**
   * @return a collection with all the unique values for <code>entry.metadata[name]</code>
   */
  def groupByEntryMetadata(String name)
  {
    def res = new TreeSet()

    _entries.values().each { SystemEntry se ->
      def value = se.metadata[name]
      if(value != null)
        res << value
    }

    return res
  }

  /**
   * Apply all the filters from the provided system model. Note that this call does not filter
   * out entries that are not filtered in provided system (by key).
   */
  SystemModel filterBy(SystemModel system)
  {
    if(system == null)
      return this

    def res = filterBy(system.filters)

    def keys = new HashSet()
    system.each { keys << it.key }
    res.each { keys << it.key }

    res = res.unfilter()

    def filter = new SystemEntryKeyModelFilter(keys: keys)

    res = res.filterBy(filter)

    return res
  }

  /**
   * Filters the 2 models in the following way:
   * <ul>
   * <li>filter system2 with all filter applied to system1</li>
   * <li>filter system1 with all filter applied to system2</li>
   * <li>compute union of keys between system1 and system2</li>
   * <li>unfilter system1 and apply new filter which filters by keys</li>
   * <li>unfilter system2 and apply new filter which filters by keys</li>
   * </ul>
   *
   * @return <code>[system1, system2]</code>
   */
  static List<SystemModel> filter(SystemModel system1, SystemModel system2)
  {
    if(system1 == null || system2 == null)
    {
      return [system1, system2]
    }

    def keys = filterKeys(system1, system2)

    system1 = system1.unfilter()
    system2 = system2.unfilter()

    def filter = new SystemEntryKeyModelFilter(keys: keys)

    system1 = system1.filterBy(filter)
    system2 = system2.filterBy(filter)

    return [system1, system2]
  }

  /**
   * Filters the 2 models in the following way:
   * <ul>
   * <li>filter system2 with all filter applied to system1</li>
   * <li>filter system1 with all filter applied to system2</li>
   * <li>compute and return union of keys between system1 and system2</li>
   * </ul>
   *
   * @return the keys
   */
  static Set<String> filterKeys(SystemModel system1, SystemModel system2)
  {
    if(system1 == null || system2 == null)
    {
      return [] as Set
    }

    def filters1 = system1.filters
    def filters2 = system2.filters

    system1 = system1.filterBy(filters2)
    system2 = system2.filterBy(filters1)

    def keys = new HashSet()
    system1.each { keys << it.key }
    system2.each { keys << it.key }
    return keys
  }


  /**
   * The filter is a dsl
   * @return the filtered model
   * @see SystemFilterBuilder
   */
  SystemModel filterBy(String filter)
  {
    filterBy(SystemFilterBuilder.parse(filter))
  }

  /**
   * @return a model that has been filtered by the provided filter
   */
  SystemModel filterBy(SystemFilter filter)
  {
    if(filter == null)
      return this

    def newFilters = SystemFilterHelper.and(filters, filter)

    SystemModel model = new SystemModel(id: id,
                                        fabric: fabric,
                                        metadata: metadata,
                                        filters: newFilters,
                                        _agentTags: _agentTags,
                                        _unfilteredModel: _unfilteredModel ?: this)

    _entries.values().each { SystemEntry se ->
      if(filter.filter(se))
        model.addEntry(se)
    }

    return model
  }


  /**
   * @return the system as it was unfiltered 
   */
  SystemModel unfilter()
  {
    if(_unfilteredModel)
      return _unfilteredModel
    else
      return this
  }

  /**
   * @param filter (will be called for each entry (<code>SystemEntry</code>) and should return
   * <code>true</code> if the entry should be included, <code>false</code> for excluded
   * @return a model that has been filtered by the provided filter
   */
  SystemModel filterBy(Closure filter)
  {
    filterBy(new ClosureSystemFilter(filter))
  }

  /**
   * @param filter (will be called for each entry (<code>SystemEntry</code>) and should return
   * <code>true</code> if the entry should be included, <code>false</code> for excluded
   * @return a model that has been filtered by the provided filter
   */
  SystemModel filterBy(String name, Closure filter)
  {
    filterBy(new ClosureSystemFilter(name, filter))
  }

  /**
   * This is a shortcut to apply a filter of the style: <code>se."${name}" == value</code>.
   * Note that it properly handles names with a dot (ex: metadata.version will be turned into
   * <code>se.metadata.version</code> (not <code>se."metadata.version"</code>)
   *
   * @see SystemEntry#flatten()
   */
  SystemModel filterBy(String name, def value)
  {
    return filterBy(new PropertySystemFilter(name: name, value: value))
  }

  /**
   * This is a shortcut to apply a filter where the metadata matches the value
   */
  SystemModel filterByMetadata(String name, def value)
  {
    return filterBy(new PropertySystemFilter(name: "metadata.${name}".toString(), value: value))
  }

  def each(Closure closure)
  {
    _entries.values().each(closure)
    
    return this
  }

  /**
   * In the canonical representation, the agent tags are removed from the entry tags
   */
  def toCanonicalRepresentation()
  {
    def ext = toExternalRepresentation()

    if(!_agentTags.isEmpty())
    {
      ext = LangUtils.deepClone(ext)

      ext.entries.each { e ->
        e.tags?.removeAll(getAgentTags(e.agent).tags)
      }
    }
    
    return ext
  }

  def toExternalRepresentation()
  {
    toExternalRepresentation(true)
  }

  def toExternalRepresentation(boolean includeEntries)
  {
    def map = [
      id: id,
      fabric: fabric
    ]

    if(metadata)
      map.metadata = metadata

    if(includeEntries && _entries)
      map.entries = findEntries().collect { it.toExternalRepresentation() }

    if(!_agentTags.isEmpty())
    {
      map.agentTags = GroovyCollectionsUtils.collectKey(_agentTags, [:]) { k, v ->
        v.tags
      }
    }

    return map
  }

  public SystemModel clone()
  {
    def ext = toExternalRepresentation()
    ext = LangUtils.deepClone(ext)
    return fromExternalRepresentation(ext)
  }

  public SystemModel cloneNoEntries()
  {
    def ext = toExternalRepresentation(false)
    ext = LangUtils.deepClone(ext)
    return fromExternalRepresentation(ext)
  }

  static SystemModel fromExternalRepresentation(def er)
  {
    new SystemModelBuilder().deserializeFromJsonMap(er).toModel()
  }

  boolean equals(o)
  {
    if(this.is(o)) return true;

    if(!(o instanceof SystemModel)) return false;

    SystemModel that = (SystemModel) o;

    if(id != that.id) return false;
    if(_entries != that._entries) return false;
    if(_agentTags != that._agentTags) return false;
    if(fabric != that.fabric) return false;
    if(metadata != that.metadata) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = (id ? id.hashCode() : 0);
    result = 31 * result + (_entries ? _entries.hashCode() : 0);
    result = 31 * result + (_agentTags ? _agentTags.hashCode() : 0);
    result = 31 * result + (fabric ? fabric.hashCode() : 0);
    result = 31 * result + (metadata ? metadata.hashCode() : 0);
    return result;
  }

  def String toString()
  {
    return JsonUtils.prettyPrint(toExternalRepresentation(), 2)
  }
}
