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

package org.linkedin.glu.groovy.utils.rest

import org.linkedin.groovy.util.rest.RestException
import org.linkedin.util.reflect.ReflectUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.glu.groovy.utils.json.GluGroovyJsonUtils

/**
 * @author yan@pongasoft.com */
public class GluGroovyRestUtils
{
  public static final String MODULE = GluGroovyRestUtils.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  /**
   * This method will try to rebuild the full stack trace based on the rest exception recursively.
   * Handles the case when the client does not know about an exception
   * (or it simply cannot be created).
   */
  public static Throwable rebuildException(RestException restException)
  {
    Throwable originalException = restException
    try
    {
      def exceptionClass = ReflectUtils.forName(restException.originalClassName)
      originalException = exceptionClass.newInstance([restException.originalMessage] as Object[])

      originalException.setStackTrace(restException.stackTrace)

      if(restException.cause)
        originalException.initCause(rebuildException(restException.cause))
    }
    catch(Exception e)
    {
      if(log.isDebugEnabled())
      {
        log.debug("Cannot instantiate: ${restException.originalClassName}... ignored", e)
      }
    }

    return originalException
  }

  /**
   * 'Serializes' the throwable into a json representation in order to be able to rebuild it later.
   */
  static def toJSON(Throwable th)
  {
    if(th)
    {
      return [exception: GluGroovyJsonUtils.extractFullStackTrace(th)]
    }
    else
      return null
  }
  /**
   * From a json representation (as built by {@link #toJSON(Throwable)) builds a rest exception
   */
  static RestException fromJSON(jsonRepresentation)
  {
    def res = null
    def parent = null
    jsonRepresentation?.exception?.each { cause ->
      def ex = new RestException(cause.name, cause.message, rebuildStackTrace(cause.stackTrace))
      if(res == null)
        res = ex
      parent?.initCause(ex)
      parent = ex
    }
    return res
  }

}