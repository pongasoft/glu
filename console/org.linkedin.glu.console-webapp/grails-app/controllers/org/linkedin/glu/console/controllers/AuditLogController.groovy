/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2014 Yan Pujante
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

import javax.servlet.http.HttpServletResponse

class AuditLogController extends ControllerBase
{
  def list = {
    params.max = Math.min(params.max ? params.max.toInteger() : 100, 100)
    params.sort = 'dateCreated'
    params.order = 'desc'
    [auditLogInstanceList: AuditLog.list(params), auditLogInstanceTotal: AuditLog.count()]
  }

  /**
   * Count audit logs (HEAD /audit/logs) */
  def rest_count_audit_logs = {
    response.addHeader("X-glu-totalCount", AuditLog.count().toString())
    response.setStatus(HttpServletResponse.SC_OK)
    render ''
  }

  /**
   * List audit logs (GET /audit/logs) */
  def rest_list_audit_logs = {
    params.max = params.max ? params.max.toInteger() : 100
    params.sort = params.sort ?: 'id'
    params.order = params.order ?: 'desc'
    params.offset = params.offset ? params.offset.toInteger() : 0

    def list = AuditLog.list(params)?.collect { buildMap(it) }

    if(list)
    {
      response.addHeader("X-glu-count", list.size().toString())
      response.addHeader("X-glu-totalCount", AuditLog.count().toString())
      ['max', 'offset', 'sort', 'order'].each { k ->
        response.addHeader("X-glu-${k}", params[k].toString())
      }

      response.setContentType('text/json')
      render prettyPrintJsonWhenRequested(list)
    }
    else
    {
      response.sendError(HttpServletResponse.SC_NO_CONTENT,
                         'no audit logs')
      render ''
    }
  }

  private static Map buildMap(AuditLog auditLog)
  {
    [
      id: auditLog.id,
      username: auditLog.username,
      dateCreated: auditLog.dateCreated.time,
      type: auditLog.type,
      details: auditLog.details,
      info: auditLog.info
    ]
  }

}
