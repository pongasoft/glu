/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

import org.linkedin.util.lang.LangUtils

/**
 * @author ypujante@linkedin.com */
class ConsoleUtils
{
  static def sortByName(list)
  {
    list?.sort() { e1, e2 ->
      return e1.name.compareTo(e2.name)
    }
  }

  static def sortBy(list, String propertyName)
  {
    list?.sort() { e1, e2 ->
      return e1."${propertyName}".compareTo(e2."${propertyName}")
    }
  }

  static def sortBy(list, Collection propertyNames)
  {
    list?.sort() { e1, e2 ->
      propertyNames.collect { propertyName -> LangUtils.compare(e1."${propertyName}", e2."${propertyName}") }.find { it } ?: 0
    }
  }

  static def sortBy(list, Closure closure)
  {
    list?.sort() { e1, e2 ->
      return closure(e1).compareTo(closure(e2))
    }
  }

  /**
   * @return always a non <code>null</code> result
   */
  static def mergeTags(tags1, tags2)
  {
    if(tags1 == null)
    {
      if(tags2 == null)
        return []
      else
        return tags2
    }
    else
    {
      if(tags2 == null)
        return tags1
      else
      {
        TreeSet tags = new TreeSet()
        tags.addAll(tags1)
        tags.addAll(tags2)
        return tags
      }
    }
  }
}
