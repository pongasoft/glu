/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.console.controllers

import org.linkedin.glu.console.domain.AuditLog
import org.linkedin.util.codec.Codec
import org.linkedin.util.codec.Base64Codec
import org.linkedin.groovy.util.json.JsonUtils

/**
 * Base class for controllers
 *
 * @author ypujante@linkedin.com  */
class ControllerBase
{
  // we use a codec because somehow the decoding of the cookies when json format is not working :(
  public final static Codec COOKIE_CODEC = new Base64Codec('console')

  /**
   * Should be called from an interceptor to make sure that a current fabric is selected. If not
   * it redirects to the page to select one.
   */
  protected def ensureCurrentFabric()
  {
    def fabric = request.fabric

    if(!fabric)
    {
      redirect(controller: 'fabric', action: 'select')
      return false
    }

    return fabric
  }

  protected def cleanParams(params)
  {
    cleanParams(params, [])
  }

  protected def cleanParams(params, keysToRemove)
  {
    def res = [*:params]

    res.remove('action')
    res.remove('controller')

    keysToRemove?.each {
      res.remove(it)
    }

    return res
  }

  protected def flashException(message, throwable)
  {
    flash.error = message
    def sw = new StringWriter()
    sw.withPrintWriter {
      throwable.printStackTrace(it)
    }
    flash.stackTrace = sw.toString()
  }

  protected def flashException(throwable)
  {
    def e = throwable
    def msg = []
    while(e != null)
    {
      if(e.message)
        msg << e.message
      e = e.cause
    }

    if(msg.size() == 1)
      msg = msg[0]
    
    flashException(msg, throwable)
  }


   /**
    * Make sure that a given section runs with a unique lock accross the entire application.
    * @return whatever the closure returns
    */
  protected def withLock(String lockName, Closure closure)
  {
    def lock

    synchronized(servletContext)
    {
      lock = servletContext[lockName]
      if(lock == null)
      {
        lock = new Object()
        servletContext[lockName] = lock
      }
    }

    synchronized(lock)
    {
      return closure()
    }
  }

  /**
   * 'Serialize' the object to json and then conditionally pretty print it (when the
   * <code>prettyPrint</code> param is provided)
   */
  protected String prettyPrintJsonWhenRequested(Object o)
  {
    if(params.prettyPrint)
      return JsonUtils.prettyPrint(o)
    else
      return JsonUtils.compactPrint(o)
  }

  /**
   * Convenient call to add an audit log entry
   */
  static def audit(String username, String type, String details, String info)
  {
    AuditLog.audit(username, type, details, info)
  }

  /**
   * Convenient call to add an audit log entry
   */
  static def audit(String type, String details, String info)
  {
    AuditLog.audit(type, details, info)
  }

  /**
   * Convenient call to add an audit log entry
   */
  static def audit(String type, String details)
  {
    AuditLog.audit(type, details)
  }

  /**
   * Convenient call to add an audit log entry
   */
  static def audit(String type)
  {
    AuditLog.audit(type)
  }
}
