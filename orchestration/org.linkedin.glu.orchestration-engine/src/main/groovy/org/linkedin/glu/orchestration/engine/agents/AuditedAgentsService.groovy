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

package org.linkedin.glu.orchestration.engine.agents

import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.orchestration.engine.audit.AuditLogService
import java.security.AccessControlException

/**
 * This class decorates an {@link AgentsService} to add auditing.
 * 
 * @author yan@pongasoft.com  */
public class AuditedAgentsService implements AgentsService
{
  // will delegate all calls to agentsService
  @Delegate
  @Initializable
  AgentsService agentsService

  @Initializable
  AuditLogService auditLogService

  def clearError(args)
  {
    auditLogService.audit('agent.clearError', "${args}")
    agentsService.clearError(args)
  }

  def uninstallScript(args)
  {
    auditLogService.audit('agent.uninstallScript', "${args}")
    agentsService.uninstallScript(args)
  }

  def forceUninstallScript(args)
  {
    auditLogService.audit('agent.forceUninstallScript', "${args}")
    agentsService.forceUninstallScript(args)
  }

  def interruptAction(args)
  {
    auditLogService.audit('agent.interruptAction', "${args}")
    agentsService.interruptAction(args)
  }

  def sync(args)
  {
    auditLogService.audit('agent.sync')
    agentsService.sync(args)
  }

  def kill(args)
  {
    auditLogService.audit('agent.kill', "${args}")
    agentsService.kill(args)
  }

  void tailLog(args, Closure closure)
  {
    auditLogService.audit('agent.tailLog', "${args}")
    agentsService.tailLog(args, closure)
  }

  void streamFileContent(args, Closure closure)
  {
    try
    {
      agentsService.streamFileContent(args) {
        auditLogService.audit('agent.getFileContent', "${args}")
        closure(it)
      }
    }
    catch (AccessControlException e)
    {
      auditLogService.audit('agent.getFileContent.notAuthorized', "${args}")
      throw e
    }
  }
}