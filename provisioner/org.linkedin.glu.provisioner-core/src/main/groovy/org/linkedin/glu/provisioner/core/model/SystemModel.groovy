/*
 * Copyright 2010-2010 LinkedIn, Inc
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

import org.linkedin.util.codec.CodecUtils
import org.linkedin.util.codec.OneWayCodec
import org.linkedin.util.codec.OneWayMessageDigestCodec
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.codec.HexaCodec
import org.linkedin.util.lang.LangUtils
import org.linkedin.groovy.util.collections.GroovyCollectionsUtils

/**
 * @author ypujante@linkedin.com  */
class SystemModel
{
  public static final OneWayCodec SHA1 =
    OneWayMessageDigestCodec.createSHA1Instance('', HexaCodec.INSTANCE)

  public static final SystemModelSerializer TO_STRING_SERIALIZER =
    new JSONSystemModelSerializer(prettyPrint: 2)

  private final Map<String, SystemEntry> _entries = new TreeMap()

  String id
  String fabric

  def metadata = [:]

  // contains a list of filters that have been applied to this model from the original (chain if
  // more than one)
  SystemFilter filters

  // when a filter is applied we keep a reference to the original unfiltered model
  private SystemModel _unfilteredModel = null

  /**
   * @return the sha1 of the content (does not include the id) */
  String computeContentSha1()
  {
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

  Collection<SystemEntry> findEntries()
  {
    return _entries.values().collect { it }
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

    _entries.values().each {
      def map = it.flatten()

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
  static def filter(SystemModel system1, SystemModel system2)
  {
    if(system1 == null || system2 == null)
    {
      return [system1, system2]
    }

    def filters1 = system1.filters
    def filters2 = system2.filters

    system1 = system1.filterBy(filters2)
    system2 = system2.filterBy(filters1)

    def keys = new HashSet()
    system1.each { keys << it.key }
    system2.each { keys << it.key }

    system1 = system1.unfilter()
    system2 = system2.unfilter()

    def filter = new SystemEntryKeyModelFilter(keys: keys)

    system1 = system1.filterBy(filter)
    system2 = system2.filterBy(filter)

    return [system1, system2]
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

    def newFilters = SystemFilterBuilder.and(filters, filter)

    SystemModel model = new SystemModel(id: id,
                                        fabric: fabric,
                                        metadata: metadata,
                                        filters: newFilters,
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

  def toExternalRepresentation()
  {
    return [id: id,
            fabric: fabric,
            metadata: metadata,
            entries: findEntries().collect { it.toExternalRepresentation() }]
  }

  public SystemModel clone()
  {
    def ext = toExternalRepresentation()
    ext = LangUtils.deepClone(ext)
    return SystemModel.fromExternalRepresentation(ext)
  }

  static SystemModel fromExternalRepresentation(def er)
  {
    if(er == null)
      return null

    SystemModel res = new SystemModel(id: er.id, fabric: er.fabric, metadata: er.metadata)

    er.entries?.each { res.addEntry(SystemEntry.fromExternalRepresentation(it)) }

    return res
  }

  boolean equals(o)
  {
    if(this.is(o)) return true;

    if(!(o instanceof SystemModel)) return false;

    SystemModel that = (SystemModel) o;

    if(id != that.id) return false;
    if(_entries != that._entries) return false;
    if(fabric != that.fabric) return false;
    if(metadata != that.metadata) return false;

    return true;
  }

  int hashCode()
  {
    int result;

    result = (id ? id.hashCode() : 0);
    result = 31 * result + (_entries ? _entries.hashCode() : 0);
    result = 31 * result + (fabric ? fabric.hashCode() : 0);
    result = 31 * result + (metadata ? metadata.hashCode() : 0);
    return result;
  }

  def String toString()
  {
    return TO_STRING_SERIALIZER.serialize(this)
  }
}
