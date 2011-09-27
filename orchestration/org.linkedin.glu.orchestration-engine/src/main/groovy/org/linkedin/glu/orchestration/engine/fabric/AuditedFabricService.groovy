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

package org.linkedin.glu.orchestration.engine.fabric

import org.linkedin.glu.orchestration.engine.audit.AuditLogService
import org.linkedin.util.annotations.Initializable

/**
 * @author yan@pongasoft.com */
public class AuditedFabricService implements FabricService
{
  // will delegate all calls to fabricService
  @Delegate
  @Initializable(required = true)
  FabricService fabricService

  @Initializable
  AuditLogService auditLogService

  @Override
  void setAgentFabric(String agentName, String fabricName)
  {
    auditLogService?.audit('fabric.addAgent', "agent: ${agentName}, fabric: ${fabricName}")
    fabricService.setAgentFabric(agentName, fabricName)
  }

  @Override
  void configureAgent(InetAddress host, String fabricName)
  {
    auditLogService?.audit('fabric.configureAgent', "agent: ${host}, fabric: ${fabricName}")
    fabricService.configureAgent(host, fabricName)
  }

  @Override
  void configureAgent(URI agentConfigURI, String fabricName)
  {
    auditLogService?.audit('fabric.configureAgent', "agent: ${agentConfigURI}, fabric: ${fabricName}")
    fabricService.configureAgent(agentConfigURI, fabricName)
  }


  @Override
  void resetCache()
  {
    auditLogService?.audit('fabric.resetCache')
    fabricService.resetCache()
  }
}