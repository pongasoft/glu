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

package org.linkedin.glu.provisioner.impl.agent

import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.core.environment.Installation
import org.linkedin.glu.provisioner.core.action.IActionDescriptorFactory
import org.linkedin.glu.provisioner.core.action.IDescriptionProvider

/**
 * Creates descriptors for actions that will use the agent touchpoint
 */
public class AgentActionDescriptorFactory implements IActionDescriptorFactory, AgentConstants
{

  public static final AgentActionDescriptorFactory INSTANCE = new AgentActionDescriptorFactory()

  /**
   * Maps the action names to the method to execute to get the
   * descriptor for each one of them
   */
  private static final def ACTIONS = [
          "noop",
          "installscript", "install", "configure", "start",
          "stop", "unconfigure", "uninstall","uninstallscript"
  ]

  public ActionDescriptor getActionDescriptor(String action, Installation i)
  {
    getActionDescriptor(action, i, null)
  }

  public ActionDescriptor getActionDescriptor(String action,
                                              Installation i,
                                              IDescriptionProvider descriptionProvider)
  {
    if (!ACTIONS.contains(action))
    {
      throw new IllegalArgumentException("Agent action factory is not aware of action (${action}). Valid action are ${ACTIONS}")
    }

    def description =
      descriptionProvider?.computeDescription(action, i) ?:
      DefaultDescriptionProvider.INSTANCE.computeDescription(action, i)

    new ActionDescriptor(type: ID,
                         actionName: action,
                         actionParams: i.props,
                         description: description,
                         descriptorProperties: getDescriptorProperties(i))
  }

  public String[] getAvailableActions()
  {
    return ACTIONS
  }

  public String getId()
  {
    return ID
  }

  private Map getDescriptorProperties(Installation i)
  {
    def res = [:]
    res[MOUNT_POINT] = i.mount
    res[SCRIPT_LOCATION] = i.gluScript
    res[PARENT] = i.parent
    res[AGENT_NAME] = i.hostname
    res[AGENT_URI] = i.uri
    return   res
  }
}