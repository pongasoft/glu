/*
 * Copyright (c) 2011-2013 Yan Pujante
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

package org.linkedin.glu.groovy.utils

import org.linkedin.groovy.util.lang.GroovyLangUtils
import org.linkedin.glu.utils.exceptions.MultipleExceptions

/**
 * @author yan@pongasoft.com */
public class GluGroovyLangUtils extends GroovyLangUtils
{
  static boolean getOptionalBoolean(def value, boolean defaultValue)
  {
    if(value == null || value instanceof ConfigObject)
      return defaultValue

    if(value instanceof Boolean)
      return value.booleanValue()

    value = value.toString().toLowerCase()

    switch(value)
    {
      case 'true':
      case 'yes':
      case 'on':
        return true

      case 'false':
      case 'no':
      case 'off':
        return false
    }

    throw new IllegalArgumentException("not a boolean : ${value}");
  }

  /**
   * Call all the closures and make sure none of it throws an exception. This method itself
   * does not throws exception. The value returned by the last closure in the collection will be
   * returned
   */
  static def noException(Collection<Closure> closures)
  {
    noException {
      def res = null
      closures?.each { c ->
      if(c)
        res = noException(c)
      else
        res = null
      }
      return res
    }
  }

  /**
   * Throw only 1 exception (at most) even if there are multiple.
   */
  static def onlyOneException(Collection<Closure> closures)
  {
    def res = null
    Collection<Throwable> exceptions = []

    noException {
      closures?.each { c ->
        if(c)
        {
          try
          {
            res = c()
          }
          catch(Throwable th)
          {
            exceptions << th
          }
        }
        else
          res = null
      }
    }

    MultipleExceptions.throwIfExceptions(exceptions)

    return res
  }

  /**
   * @return the long found in the config or the default value
   */
  static long getOptionalLong(config, String name, long defaultValue)
  {
    def value = config?."${name}"

    if(value == null)
      return defaultValue

    return value as long
  }

  /**
   * Copy the properties from source to target and copy only the properties that exist in target
   * @return <code>target</code>
   */
  static <T> T copyProperties(def source, T target)
  {
    if(source != null && target != null)
    {
      target.metaClass.properties.each { p ->
        if(source.metaClass.hasProperty(source, p.name)
          && p.name != 'metaClass'
          && p.name != 'class'
          && p.setter) // read only?
          p.setProperty(target, source.metaClass.getProperty(source, p.name))
      }
    }
    return target
  }
}