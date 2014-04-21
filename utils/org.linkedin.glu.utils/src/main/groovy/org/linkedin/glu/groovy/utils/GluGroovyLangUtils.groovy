/*
 * Copyright (c) 2011-2014 Yan Pujante
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
  /**
   * Generic comparator closure which invokes one of the compare methods (dynamic groovy
   * dispatching)
   */
  static final Closure COMPARATOR_CLOSURE = { v1, v2 -> compare(v1, v2) }

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
  static def onlyOneException(Closure... closures)
  {
    onlyOneException(closures as Collection<Closure>)
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

  /**
   * Given a string in <code>MemorySize</code> format return the number of bytes (while accounting
   * for the fact that the size may be negative (offset)
   */
  public static long computeOffsetFromMemorySize(String offsetString)
  {
    if(!offsetString || offsetString == '-')
      return 0

    if(offsetString.startsWith('-'))
      return -(MemorySize.parse(offsetString[1..-1]).sizeInBytes)
    else
      return MemorySize.parse(offsetString).sizeInBytes

  }

  /**
   * Extracts the exception stack trace and all causes. Each element in the returned collection
   * is a map with <code>name</code>, <code>message</code>.
   *
   * @return <code>out</code>
   */
  public static <T> T extractExceptionDetailsWithCause(exception, T out = [])
  {
    if(exception)
    {
      out << extractExceptionDetails(exception)
      extractExceptionDetailsWithCause(exception.cause, out)
    }

    return out
  }

  /**
   * Extracts the exception stack trace and all causes. Each element in the returned collection
   * is a map with <code>name</code>, <code>message</code>.
   *
   * @return <code>out</code>
   */
  public static Map extractExceptionDetails(exception)
  {
    if(exception)
    {
      [
        name: exception.getClass().name,
        message: exception.message
      ]
    }
    else
      return null
  }

  /**
   * @return a simple string representation of the throwable (just the name and message)
   */
  public static String toString(Throwable throwable)
  {
    if(throwable)
      "${throwable.getClass().name}: \"${throwable.message}\"".toString()
    else
      null
  }
}