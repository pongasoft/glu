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
 * @author yan@pongasoft.com */
public class SystemFilterHelper
{
  /**
   * Convenient call to create a chain of 2 filters that are 'anded' together. Handle properly
   * <code>null</code> and 'adjacent' 'and' filters.
   */
  static SystemFilter and(SystemFilter f1, SystemFilter f2)
  {
    if(f1 == null)
      return f2

    if(f2 == null)
      return f1

    Set<SystemFilter> filters = new LinkedHashSet<SystemFilter>()

    if(f1 instanceof LogicAndSystemFilterChain)
    {
      filters.addAll(f1.filters.collect { it })

      if(f2 instanceof LogicAndSystemFilterChain)
      {
        filters.addAll(f2.filters)
      }
      else
      {
        filters << f2
      }
    }
    else
    {
      filters << f1
      filters << f2
    }

    if(!filters)
      return null

    if(filters.size() == 1)
      return filters.iterator().next()
    else
      return new LogicAndSystemFilterChain(filters: filters)
  }

  /**
   * This is the 'opposite' of <code>and</code>: tries to remove f2 from f1 (if possible... this
   * call is simplified in the sense that it does not handle complex scenarios)
   */
  static SystemFilter unand(SystemFilter f1, SystemFilter f2)
  {
    if(f1 == null)
      return null

    if(f2 == null)
      return f1

    if(f2 == f1)
      return null

    Set<SystemFilter> filters = new LinkedHashSet<SystemFilter>()

    if(f1 instanceof LogicAndSystemFilterChain)
    {
      filters.addAll(f1.filters.collect { it })

      if(f2 instanceof LogicAndSystemFilterChain)
      {
        filters.removeAll(f2.filters)
      }
      else
      {
        filters.remove(f2)
      }
    }

    if(!filters)
      return null

    if(filters.size() == 1)
      return filters.iterator().next()
    else
      return new LogicAndSystemFilterChain(filters: filters)
  }

  /**
   * Convenient call to create a chain of filters that are 'anded' together. Handle properly
   * <code>null</code> and 'adjacent' 'and' filters.
   */
  static SystemFilter and(Collection<SystemFilter> filters)
  {
    if(!filters)
      return null

    Iterator<SystemFilter> iterator = filters.iterator()

    SystemFilter res = iterator.next()

    while(iterator.hasNext())
      res = and(res, iterator.next())

    return res
  }

  /**
   * This call is a 'simplified' version as it for example does not check for a filter like
   * <code>and{and{filter}}</code>...
   *
   * @return <code>true</code> if <code>filter1</code> defines a subset of <code>filter2</code>
   * or is equal to it
   */
  static boolean definesSubsetOrEqual(SystemFilter filter1, SystemFilter filter2)
  {
    if(filter1 != filter2)
    {
      if(filter2 instanceof LogicAndSystemFilterChain)
      {
        return filter2.filters.contains(filter1)
      }
      return false
    }
    else
    {
      return true
    }
  }
}