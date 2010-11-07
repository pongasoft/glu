/*
 * Copyright 2010-2010 LinkedIn, Inc
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
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.agent.rest.client.AgentFactory
import org.linkedin.glu.agent.rest.client.EncryptionKeysProvider
import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.api.AgentException
import org.linkedin.glu.agent.api.ScriptExecutionException

/**
 * A touchpoint that interacts with the agent for the upgrade
 * @author ypujante@linkedin.com
 */
public class AgentUpgradeTouchpoint extends BaseAgentTouchpoint
{
  public static final String ID = "agentUpgrade"

  public static final String MODULE = AgentUpgradeTouchpoint.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  // wait for 5s (default) for the agent to restart
  Timespan waitForRestartTimeout = Timespan.parse("5s")

  AgentUpgradeTouchpoint(AgentFactory factory, EncryptionKeysProvider keyProvider)
  {
    super(ID, factory, keyProvider)
    addActions([
               "cleanup" : this.&getCleanupAction,
               "prepare" : this.&getPrepareAction,
               "waitForRestart" : this.&getWaitForRestart,
               "commitOrRollback" : this.&getCommitOrRollbackAction,
               ])
  }

  /**
   * The cleanup action simply uninstalls (with force = true) the previously installed script in
   * case something goes wrong.
   */
  private Action getCleanupAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def mp = ad.descriptorProperties[MOUNT_POINT]
    def execute = {
      factory.withRemoteAgent(uri) { Agent a ->
        a.uninstallScript(mountPoint: mp, force: true)
      }
    }

    return new SimpleAction(ad, execute, null)
  }

  /**
   * We need to provide a different action for install script because it uses a script class name
   */
  protected Action getInstallscriptAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def mp = ad.descriptorProperties[MOUNT_POINT]
    def parent = ad.descriptorProperties[PARENT]
    def execute = {
      factory.withRemoteAgent(uri) { Agent a ->
        a.installScript(mountPoint: mp,
                        scriptClassName: 'org.linkedin.glu.agent.impl.script.AutoUpgradeScript',
                        parent: parent,
                        initParameters: ad.actionParams)
      }
    }

    return new SimpleAction(ad, execute, null)
  }

  /**
   * Execute 'prepare' action which will restart the agent
   */
  private Action getPrepareAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def mp = ad.descriptorProperties[MOUNT_POINT]

    def execute = {
      factory.withRemoteAgent(uri) { Agent a ->
        a.clearError(mountPoint: mp)
        a.executeAction(mountPoint: mp, action: 'prepare', actionArgs: ad.actionParams)
        // YP note: prepare shutdowns the agent... we cannot really call waitForState
      }
    }

    return new SimpleAction(ad, execute, null)
  }

  /**
   * In this action we wait for the agent to restart... we try to call it in a loop
   */
  private Action getWaitForRestart(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def mp = ad.descriptorProperties[MOUNT_POINT]

    def execute = {
      doWaitForRestart(uri, mp, 'prepared') 
    }

    return new SimpleAction(ad, execute, null)
  }

  /**
   * Wait for the state accross a restart: allow the agent to disappear and reappear
   */
  private void doWaitForRestart(URI uri, mp, state)
  {
    def success = false
    while(!success)
    {
      Thread.sleep(waitForRestartTimeout.durationInMilliseconds)

      try
      {
        factory.withRemoteAgent(uri) { Agent a ->
          success = a.waitForState(mountPoint: mp, state: state, timeout: timeout)
        }
      }
      catch(AgentException e)
      {
        if(log.isDebugEnabled())
          log.debug("agent has not restarted yet (ignored)", e)
      }
    }
  }

  /**
   * In this action we try to commit and if it fails we rollback.
   */
  private Action getCommitOrRollbackAction(ActionDescriptor ad)
  {
    URI uri = ad.descriptorProperties[AGENT_URI]
    def mp = ad.descriptorProperties[MOUNT_POINT]
    def execute = {
      boolean waitForRollback = false

      factory.withRemoteAgent(uri) { Agent a ->
        try
        {
          executeAction(a, ad, 'commit', 'upgraded')
        }
        catch(ScriptExecutionException e)
        {
          if(log.isDebugEnabled())
            log.debug("Agent not upgraded properly... rolling back...")
          a.clearError(mountPoint: mp)
          a.executeAction(mountPoint: mp, action: 'rollback', actionArgs: ad.actionParams)
          // YP note: rollback shutdowns the agent... we cannot really call waitForState
          waitForRollback = true
        }
      }

      if(waitForRollback)
      {
        doWaitForRestart(uri, mp, 'installed')
      }
    }

    return new SimpleAction(ad, execute, null)
  }

}