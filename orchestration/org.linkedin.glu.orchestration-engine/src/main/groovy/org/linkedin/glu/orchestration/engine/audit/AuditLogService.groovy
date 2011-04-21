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

package org.linkedin.glu.orchestration.engine.audit

/**
 * @author yan@pongasoft.com */
public interface AuditLogService
{
  /**
   * Record an audit event
   */
  void audit(String type, String details, String info)

  /**
   * Record an audit event (no info)
   */
  void audit(String type, String details)

  /**
   * Record an audit event (no detail, no info)
   */
  void audit(String type)

  /**
   * Generic call with. See other calls for key names (type, details, info).
   */
  void audit(Map params)
}