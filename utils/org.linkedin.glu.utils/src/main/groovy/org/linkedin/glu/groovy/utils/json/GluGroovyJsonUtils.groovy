/*
 * Copyright (c) 2012 Yan Pujante
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

package org.linkedin.glu.groovy.utils.json

import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.reflect.ReflectUtils

/**
 * @author yan@pongasoft.com */
public class GluGroovyJsonUtils extends JsonUtils
{
  /**
   * This method is the opposite of {@link #extractFullStackTrace(Throwable)} and will attempt
   * to "deserialize" the full stack trace into the original exception including all the chain
   * of causes. Note that if (due mostly to class loader issues), one of the exception cannot
   * be rebuild, a substitute will be created in its place
   */
  public static Throwable rebuildException(def fullStackTrace)
  {
    if(fullStackTrace == null)
      return null

    if(fullStackTrace instanceof String || fullStackTrace instanceof GString)
    {
      fullStackTrace = fullStackTrace.trim()
      if(fullStackTrace.isEmpty())
        return null
      fullStackTrace = fromJSON(fullStackTrace)
    }

    def res = null
    def parent = null
    fullStackTrace.each { cause ->
      Throwable ex
      try
      {
        def exceptionClass = ReflectUtils.forName(cause.name)
        ex = exceptionClass.newInstance([cause.message] as Object[])
      }
      catch(Throwable th)
      {
        ex = new CannotRebuildOriginalThrowableException(cause.message, cause.name, th)
      }

      ex.setStackTrace(rebuildStackTrace(cause.stackTrace))

      if(res == null)
        res = ex
      parent?.initCause(ex)
      parent = ex
    }
    return res
  }

  /**
   * Extracts the exception stack trace and all causes. Each element in the returned collection
   * is a map with <code>name</code>, <code>message</code> and <code>stackTrace</code>.
   * <code>stackTrace</code> is a collection of map: <code>dc</code> (class name),
   * <code>mn</code> method name, <code>fn</code> field name and <code>ln</code> line number
   *
   * @return <code>out</code>
   */
  public static <T> T extractFullStackTrace(exception, T out = [])
  {
    if(exception)
    {
      out << [
        name: exception.getClass().name,
        message: exception.message,
        stackTrace: extractStackTrace(exception)
      ]
      extractFullStackTrace(exception.cause, out)
    }

    return out
  }

  /**
   * Extracts the exception stack trace and all causes and returns it as a json string
   *
   * @see #extractFullStackTrace
   * @param prettyPrintJson if you want to pretty print the output... otherwise compact print
   * @return the json string
   */
  public static String exceptionToJSON(exception, boolean prettyPrintJson = false)
  {
    def fst = extractFullStackTrace(exception)
    if(prettyPrintJson)
      return prettyPrint(fst)
    else
      return compactPrint(fst)
  }

  /**
   * Opposite of {@link #extractStackTrace}
   */
  public static StackTraceElement[] rebuildStackTrace(stackTrace)
  {
    def elements = []

    stackTrace.each { ste ->
      elements << new StackTraceElement(ste.dc, ste.mn, ste.fn, ste.ln as int)
    }

    return elements as StackTraceElement[]
  }

  /**
   * Returns the stack trace elements as a collection of maps:  <code>dc</code> (class name),
   * <code>mn</code> method name, <code>fn</code> field name and <code>ln</code> line number
   */
  public static Collection<Map> extractStackTrace(exception)
  {
    def stackTrace = []

    exception?.stackTrace?.each { ste ->
      stackTrace << [dc: ste.className, mn: ste.methodName, fn: ste.fileName, ln: ste.lineNumber]
    }

    return stackTrace
  }

}