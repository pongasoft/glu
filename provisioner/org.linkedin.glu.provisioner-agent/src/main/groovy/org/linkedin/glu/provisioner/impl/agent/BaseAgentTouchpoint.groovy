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

import org.linkedin.glu.provisioner.core.action.Action
import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.core.action.SimpleAction
import org.linkedin.glu.provisioner.core.touchpoint.Touchpoint
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.agent.rest.client.AgentFactory
import org.linkedin.glu.agent.rest.client.EncryptionKeysProvider
import org.linkedin.glu.agent.api.Agent

/**
 * Common code between touchpoints.
 *
 * @author ypujante@linkedin.com
 */
public class BaseAgentTouchpoint implements Touchpoint, AgentConstants
{

  public static final String MODULE = BaseAgentTouchpoint.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  /**
   *  the timeout for the agent operations
   */
  Timespan timeout = Timespan.parse('10s')

  private final String _id
  private final AgentFactory _factory
  private final EncryptionKeysProvider _keyProvider

  /**
   * Maps the phase names to the method to execute to get the
   * actions for each one of them
   */
  private Map actionsMap = [
    "installscript" : this.&getInstallscriptAction,
    "install" : this.&getInstallAction,
    "uninstall" : this.&getUninstallAction,
    "uninstallscript" : this.&getUninstallscriptAction
  ]

  def BaseAgentTouchpoint(String id, AgentFactory factory, EncryptionKeysProvider keyProvider)
  {
    _id = id
    _factory = factory
    _keyProvider = keyProvider
  }

  /**
   * Subclasses will provide their own actions
   */
  protected void addActions(Map actions)
  {
    if(actions)
      actionsMap.putAll(actions)
  }

  public String getId()
  {
    return _id
  }

  public Action getAction(ActionDescriptor ad)
  {
    if (!actionsMap.containsKey(ad.actionName))
    {
      throw new IllegalArgumentException("${getClass().name} is not aware of action (${ad.actionName}). Valid actions are ${actionsMap.keySet()}")
    }
    return actionsMap[ad.actionName] (ad)
  }

  AgentFactory getFactory()
  {
    return _factory
  }

  /**
   * Executes the action. Wait for the state and loop until successful or the thread is interrupted.
   * The timeout is how long to wait for checking... When the thread is interrupted, try to interrupt 
   * the action.
   */
  protected void executeAction(Agent a, ActionDescriptor ad, action, state)
  {
    def mp = ad.descriptorProperties[MOUNT_POINT]

    a.clearError(mountPoint: mp)

    def actionArgs = getActionArgs(ad)

    a.executeAction(mountPoint: mp, action: action, actionArgs: actionArgs)

    def success = false
    while(!success)
    {
      success = a.waitForState(mountPoint: mp, state: state, timeout: timeout)
      if(!success)
      {
        if(Thread.currentThread().isInterrupted())
        {
          a.interruptAction(mountPoint: mp, action: action)
          throw new InterruptedException()
        }
      }
    }
  }

  private Map getActionArgs(ActionDescriptor ad)
  {
    def Map args = [ : ]

    if (_keyProvider != null) {
      args.put(ENCRYPTION_KEYS, _keyProvider.getEncryptionKeys())
    }
    
    args.putAll(ad.actionParams)

    return args
  }

  protected Action getInstallscriptAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def mp = ad.descriptorProperties[MOUNT_POINT]
    def sl = ad.descriptorProperties[SCRIPT_LOCATION]
    def parent = ad.descriptorProperties[PARENT]
    def execute = {
      factory.withRemoteAgent(uri) { Agent a ->
        a.installScript(mountPoint: mp, scriptLocation: sl,
                        parent: parent,
                        initParameters: ad.actionParams)
      }
    }

    def rollback = {
      factory.withRemoteAgent(uri) { Agent a ->
        a.clearError(mountPoint: mp)
        a.uninstallScript(mountPoint: mp)
      }
    }
    return new SimpleAction(ad, execute, rollback)
  }

  protected Action getInstallAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def execute = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'install', 'installed')
      }
    }

    def rollback = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'uninstall', 'NONE')
      }
    }
    return new SimpleAction(ad, execute, rollback)
  }

  protected Action getUninstallscriptAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def mp = ad.descriptorProperties[MOUNT_POINT]
    def sl = ad.descriptorProperties[SCRIPT_LOCATION]
    def parent = ad.descriptorProperties[PARENT]
    def execute = {
      factory.withRemoteAgent(uri) { Agent a ->
        a.uninstallScript(mountPoint: mp)
      }
    }

    def rollback = {
      factory.withRemoteAgent(uri) { Agent a ->
        a.clearError(mountPoint: mp)
        a.installScript(mountPoint: mp, scriptLocation: sl,
                        parent: parent,
                        initParameters: ad.actionParams)
      }
    }

    return new SimpleAction(ad, execute, rollback)
  }

  protected Action getUninstallAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def execute = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'uninstall', 'NONE')
      }
    }

    def rollback = {
      factory.withRemoteAgent(uri) { Agent a ->
        executeAction(a, ad, 'install', 'installed')
      }
    }

    return new SimpleAction(ad, execute, rollback)
  }
}