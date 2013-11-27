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

import org.linkedin.glu.utils.tags.TagsSerializer

/**
 * DSL for {@link SystemFilter}.
 *
 * <pre>
 * // in the code
 * def filter = new SystemFilterBuilder().and {
 *   metadata.product = 'p1' // can use =
 *   initParameters.skeleton == 's1' // or ==
 *   tags = 't1;t2' // same as tags.hasAny('t1;t2')
 *   not {
 *     or {
 *       agent = 'h1'
 *       c('myClosure) {
 *         // this is a real closure: 'it' is of type SystemEntry!
 *         it.initParameters.war == 'w1'
 *       }
 *     }
 *   }
 * }
 *
 * // as a String (call parse method)
 * """
 *   metadata.product = 'p1' // can use =
 *   tags = 't1;t2' // same as tags.hasAny('t1;t2')
 *   initParameters.skeleton == 's1' // or ==
 *   not {
 *     or {
 *       agent = 'h1'
 *       c('myClosure) {
 *         // this is a real closure: 'it' is of type SystemEntry!
 *         it.initParameters.war == 'w1'
 *       }
 *     }
 *   }
 *"""
 * </pre>
 *
 * @author ypujante@linkedin.com  */
public class SystemFilterBuilder
{
  LogicSystemFilterChain filter

  /**
   * Parses the dsl (see documentation)
   */
  static SystemFilter parse(String dsl)
  {
    if(dsl == null)
      return null

    dsl = dsl.trim()

    if(dsl == '' || dsl == '-')
      return null

    def builder = new SystemFilterBuilder(filter: new LogicAndSystemFilterChain())

    def shell = new GroovyShell(new BindingBuilder(systemFilterBuilder: builder))
    try
    {
      Script script = shell.parse(dsl)

      script.metaClass = createEMC(script.class) { ExpandoMetaClass emc ->
        ['and', 'or', 'not', 'c'].each { method ->
          emc."${method}" = { Closure cl ->
            builder."${method}"(cl)
          }
        }
      }

      script.run()
    }
    finally
    {
      shell.resetLoadedClasses()
    }

    return builder.optimizeFilter(builder.filter)
  }

  private static ExpandoMetaClass createEMC(Class clazz, Closure cl)
  {
    ExpandoMetaClass emc = new ExpandoMetaClass(clazz, false)
    cl(emc)
    emc.initialize()
    return emc
  }

  SystemFilter and(Closure cl)
  {
    optimizeFilter(doAddNewFilter(new LogicAndSystemFilterChain(), cl))
  }

  SystemFilter or(Closure cl)
  {
    optimizeFilter(doAddNewFilter(new LogicOrSystemFilterChain(), cl))
  }

  SystemFilter not(Closure cl)
  {
    def newFilter = new LogicNotSystemFilter(filter: new LogicAndSystemFilterChain())
    addNewFilter(newFilter)
    newFilter.filter = executeClosure(newFilter.filter, cl)
    return newFilter
  }

  SystemFilter c(Closure cl)
  {
    addNewFilter(new ClosureSystemFilter(cl))
  }

  SystemFilter c(String name, Closure cl)
  {
    addNewFilter(new ClosureSystemFilter(name, cl))
  }

  def propertyMissing(String name)
  {
    if(name == 'tags')
      return new TagsSystemFilterBuilder(systemFilterBuilder: this)
    else
      return new PropertySystemFilterBuilder(systemFilterBuilder: this,
                                             propertySystemFilter: new PropertySystemFilter(name: name))
  }

  void propertyMissing(String name, def value)
  {
    if(name == 'tags')
    {
      new TagsSystemFilterBuilder(systemFilterBuilder: this).hasAll(value)
    }
    else
      addNewFilter(new PropertySystemFilter(name: name, value: value))
  }

  void propertyMissing(PropertySystemFilter propertySystemFilter, def value)
  {
    propertySystemFilter.value = value
    addNewFilter(propertySystemFilter)
  }

  SystemFilter addNewFilter(SystemFilter newFilter)
  {
    if(filter)
    {
      filter.filters << newFilter
    }

    return newFilter
  }

  private SystemFilter doAddNewFilter(SystemFilter newFilter, Closure cl)
  {
    return executeClosure(addNewFilter(newFilter), cl)
  }

  private SystemFilter executeClosure(SystemFilter newFilter, Closure cl)
  {
    cl.delegate = new SystemFilterBuilder(filter: newFilter)
    cl.resolveStrategy = Closure.DELEGATE_FIRST
    cl()
    return optimizeFilter(newFilter)
  }

  private SystemFilter optimizeFilter(SystemFilter filter)
  {
    if(filter instanceof LogicSystemFilterChain)
    {
      if(!filter.filters)
        return null

      if(filter.filters.size() == 1)
        return optimizeFilter(filter.filters.iterator().next())
    }

    return filter
  }
}

class PropertySystemFilterBuilder
{
  SystemFilterBuilder systemFilterBuilder
  PropertySystemFilter propertySystemFilter

  /**
   * Handles notation agent.initParameters
   *
   * @param name in this example it would be 'initParameters' (and <code>this.propertyName</code>
   *             would be 'agent.initParameters'
   * @return a new instance of this class
   */
  def propertyMissing(String name)
  {
    new PropertySystemFilterBuilder(systemFilterBuilder: systemFilterBuilder,
                                    propertySystemFilter: propertySystemFilter.appendToken(name))
  }

  /**
   * Handles notation agent.initParameters.m1 = 'foo'
   *
   * @param name in this example it would be 'm1' (and <code>this.propertyName</code> would be
   *             'agent.initParameters'
   * @param value in this example it would be 'foo'
   */
  void propertyMissing(String name, def value)
  {
    propertySystemFilter = propertySystemFilter.appendToken(name)
    if(value instanceof PropertySystemFilterBuilder)
      throw new IllegalArgumentException("missing quotes around ${value.propertyName} for ${propertySystemFilter.name}")

    systemFilterBuilder.propertyMissing(propertySystemFilter, value)
  }

  def getAt(Object index)
  {
    new PropertySystemFilterBuilder(systemFilterBuilder: systemFilterBuilder,
                                    propertySystemFilter: propertySystemFilter.appendIndex(index))
  }

  /**
   * Handles notation agent.initParameters.m1 == 'foo'
   *
   * @param value in this example it would be 'foo'  (and <code>this.propertyName</code> would be
   *             'agent.initParameters.m1'
   */
  def boolean equals(Object value)
  {
    systemFilterBuilder.propertyMissing(propertySystemFilter, value)
    return false;
  }
}

class TagsSystemFilterBuilder
{
  public static final TagsSerializer TAGS_SERIALIZER = TagsSerializer.INSTANCE

  SystemFilterBuilder systemFilterBuilder

  /**
   * Handles hasAll(xxx)
   */
  void hasAll(def tags)
  {
    systemFilterBuilder.addNewFilter(new TagsSystemFilter(toCollection(tags), true))
  }

  /**
   * Handles hasAny(xxx)
   */
  void hasAny(def tags)
  {
    systemFilterBuilder.addNewFilter(new TagsSystemFilter(toCollection(tags), false))
  }

  /**
   * Handles notation tags == <tags>
   */
  def boolean equals(Object value)
  {
    hasAll(value)
    return false;
  }

  /**
   * Converts the tags into a collection: allowed types: <code>String</code>,
   * <code>Collection</code> or anything that answer to <code>each</code>
   */
  Collection<String> toCollection(def tags)
  {
    if(tags instanceof Collection)
      return new TreeSet(tags)

    if(tags instanceof String || tags instanceof GString)
    {
      return TAGS_SERIALIZER.deserialize(tags)
    }

    if(tags.class.metaClass.respondsTo('each'))
    {
      Set<String> res = new TreeSet<String>()

      tags.each { tag -> res << tag }

      return res
    }

    throw new IllegalArgumentException("unsupported type for tags ${tags.class.name}")
  }
}

class BindingBuilder extends Binding
{
  SystemFilterBuilder systemFilterBuilder

  def Object getVariable(String name)
  {
    if(name == 'class')
      return super.getVariable(name)
    else
      return systemFilterBuilder.propertyMissing(name)
  }

  def void setVariable(String name, Object value)
  {
    if(value instanceof PropertySystemFilterBuilder)
      throw new IllegalArgumentException("missing quotes around ${value.propertyName} for ${name}")

    systemFilterBuilder.propertyMissing(name, value)
  }
}
