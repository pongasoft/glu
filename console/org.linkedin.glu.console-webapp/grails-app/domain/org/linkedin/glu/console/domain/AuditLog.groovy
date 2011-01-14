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

package org.linkedin.glu.console.domain

import org.apache.shiro.SecurityUtils

/**
 * Represent an audit log entry.
 *
 * @author ypujante@linkedin.com */
class AuditLog
{
  public static final String MODULE = AuditLog.class.getName();
  public static final org.slf4j.Logger staticLog = org.slf4j.LoggerFactory.getLogger(MODULE);

  static constraints = {
    username(nullable: false)
    type(nullable: false)
    details(nullable: true)
    info(nullable: true)
  }

  static mapping = {
    columns {
      info type: 'text'
    }
  }

  String username
  Date dateCreated
  String type
  String details
  String info

  String toString()
  {
    "{username: ${username}, type: ${type}, details: ${details}, info: ${info}}".toString()
  }

  /**
   * Convenient call to add an audit log entry
   */
  static def audit(String username, String type, String details, String info)
  {
    if(!username)
      username = "__console__"

    def auditLog = new AuditLog(username: username,
                                type: type,
                                details: details,
                                info: info).save()

    if(!auditLog)
    {
      staticLog.warn "Error while logging: ${auditLog} / ${auditLog.errors}... ignored"
    }

    return auditLog
  }

  /**
   * Convenient call to add an audit log entry
   */
  static def audit(String type, String details, String info)
  {
    def principal
    try
    {
      principal = SecurityUtils.getSubject()?.principal
    }
    catch (Exception e)
    {
      staticLog.warn("Could not determine principal (ignored) [${e.message}]")
      
      if(staticLog.isDebugEnabled())
        staticLog.debug("Could not determine principal (ignored)", e)
    }
    audit(principal, type, details, info)
  }

  /**
   * Convenient call to add an audit log entry
   */
  static def audit(String type, String details)
  {
    audit(type, details, null)
  }

  /**
   * Convenient call to add an audit log entry
   */
  static def audit(String type)
  {
    audit(type, null)
  }


  /**
   * Generic call. 
   */
  static def audit(params)
  {
    audit(params.username, params.type, params.details, params.info)
  }
}
