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

package org.linkedin.glu.orchestration.engine.core.action.execution

import org.linkedin.glu.orchestration.engine.action.execution.ActionExecutionFactory
import org.linkedin.glu.orchestration.engine.action.execution.ActionExecution
import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor
import org.linkedin.glu.orchestration.engine.action.descriptor.NoOpActionDescriptor
import org.linkedin.glu.orchestration.engine.agents.MountPointStateProvider
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptTransitionActionDescriptor
import org.linkedin.glu.agent.rest.client.AgentFactory
import org.linkedin.util.annotations.Initializable
import org.linkedin.glu.orchestration.engine.action.descriptor.AgentActionDescriptor
import org.linkedin.glu.orchestration.engine.action.descriptor.AgentURIProvider
import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.rest.client.EncryptionKeysProvider
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptLifecycleInstallActionDescriptor
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptLifecycleUninstallActionDescriptor
import org.linkedin.util.reflect.ObjectProxyBuilder
import org.linkedin.glu.orchestration.engine.agents.RecoverableAgent

/**
 * This implementation uses a convention:
 * @author yan@pongasoft.com */
public class ActionExecutionFactoryImpl implements ActionExecutionFactory
{
  public static final String MODULE = ActionExecutionFactoryImpl.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static final String ENCRYPTION_KEYS = 'encryptionKeys'

  @Initializable(required = true)
  AgentFactory agentFactory

  @Initializable(required = true)
  AgentURIProvider agentURIProvider

  @Initializable
  MountPointStateProvider mountPointStateProvider

  @Initializable(required = false)
  EncryptionKeysProvider encryptionKeysProvider

  /**
   *  the timeout for the agent operations */
  @Initializable(required = false)
  Timespan timeout = Timespan.parse('10s')

  /**
   *  the timeout for state propagation (ZooKeeper) */
  @Initializable(required = false)
  Timespan timeoutForStatePropagation = Timespan.parse('2s')

  /**
   * when a communication exception is detected with the agent, it will sleep for this time
   * before trying again */
  @Initializable(required = false)
  Timespan agentRecoveryTimeout = Timespan.parse('5s')

  // wait for 10s (default) for the agent to restart
  @Initializable(required = false)
  Timespan selfUpgradeWaitForRestartTimeout = Timespan.parse("10s")

  /**
   * when a communication exception is detected with the agent, it will retry a certain number of
   * times */
  @Initializable(required = false)
  int agentRecoveryNumRetries = 10

  /**
   * For NoOpActionDescriptor: do nothing
   */
  def NoOpActionDescriptor_execution = { NoOpActionDescriptor ad ->
    // nothing to do (noop)
    if(log.isDebugEnabled())
      log.debug("noop: ${ad.toMetadata()}")
  }

  /**
   * For ScriptTransitionActionDescriptor: execute the action (transition) on the agent and wait
   * for the action to complete
   */
  def ScriptTransitionActionDescriptor_execution = { ScriptTransitionActionDescriptor ad ->
    withAgent(ad) { Agent agent ->

      String mountPoint = ad.mountPoint

      // 1. we clear any error which could be left over
      agent.clearError(mountPoint: mountPoint)

      // 2. we execute the transition action
      Map actionArgs = computeActionArgs(ad)
      agent.executeAction(mountPoint: mountPoint, action: ad.action, actionArgs: actionArgs)

      // // TODO MED YP: this is somewhat hacky but it will do for now
      if(mountPoint == "/self/upgrade")
      {
        if(ad.action == 'prepare' || ad.action == 'rollback')
        {
          if(log.isDebugEnabled())
            log.debug("sleeping before waiting for state for ${ad.action}")
          Thread.sleep(selfUpgradeWaitForRestartTimeout.durationInMilliseconds)
        }
      }

      // 3. we wait for the action to be completed
      def success = false
      while(!success)
      {
        success = agent.waitForState(mountPoint: mountPoint, state: ad.toState, timeout: timeout)
        if(!success)
        {
          if(Thread.currentThread().isInterrupted())
          {
            agent.interruptAction(mountPoint: mountPoint, action: ad.action)
            throw new InterruptedException()
          }
        }
      }

      // 4. we wait for the state to propagate through ZooKeeper (glu-134)
      if(mountPointStateProvider)
      {
        success = false
        while(!success)
        {
          success = mountPointStateProvider.waitForState(ad.fabric,
                                                         ad.agent,
                                                         mountPoint,
                                                         ad.toState,
                                                         timeoutForStatePropagation)
          if(!success)
          {
            if(Thread.currentThread().isInterrupted())
            {
              // we know that the action already completed (step 3) but somehow
              // it did not propagate yet to ZooKeeper and has been interrupted
              throw new InterruptedException()
            }
          }
        }
      }
    }
  }

  /**
   * For ScriptLifecycleInstallActionDescriptor: installs the script
   */
  def ScriptLifecycleInstallActionDescriptor_execution = { ScriptLifecycleInstallActionDescriptor ad ->
    withAgent(ad) { Agent agent ->
      def args =
      [
        mountPoint: ad.mountPoint,
        parent: ad.parent,
        initParameters: ad.initParameters
      ]
      if(ad.script instanceof Map)
        args.putAll(ad.script)
      else
        args.scriptLocation = ad.script
      agent.installScript(args)
    }
  }

  /**
   * For ScriptLifecycleUninstallActionDescriptor: uninstalls the script
   */
  def ScriptLifecycleUninstallActionDescriptor_execution = { ScriptLifecycleUninstallActionDescriptor ad ->
    withAgent(ad) { Agent agent ->
      agent.clearError(mountPoint: ad.mountPoint)

      agent.uninstallScript(mountPoint: ad.mountPoint)
    }
  }

  /**
   * Compute the action args (automatically adds encryption keys if any present)
   */
  Map computeActionArgs(ScriptTransitionActionDescriptor ad)
  {
    def Map args = [ : ]

    Map encryptionKeys = encryptionKeysProvider?.getEncryptionKeys()

    if (encryptionKeys) {
      args.put(ENCRYPTION_KEYS, encryptionKeys)
    }

    if(ad.actionArgs)
      args.putAll(ad.actionArgs)

    return args
  }

  /**
   * Compute the agent URI and call the closure with the {@link Agent}.
   */
  private def withAgent(AgentActionDescriptor ad, Closure closure)
  {
    agentFactory.withRemoteAgent(agentURIProvider.getAgentURI(ad.fabric, ad.agent)) { Agent agent ->
      def agentProxy = new RecoverableAgent(agent, agentRecoveryNumRetries, agentRecoveryTimeout)
      agent = ObjectProxyBuilder.createProxy(agentProxy, Agent.class)
      closure(agent)
    }
  }

  @Override
  <V> ActionExecution<V> createAction(ActionDescriptor actionDescriptor)
  {
    Closure actionClosure = this."${actionDescriptor.class.simpleName}_execution"

    return new ClosureActionExecution<V>(actionDescriptor: actionDescriptor,
                                         actionClosure: actionClosure)
  }
}