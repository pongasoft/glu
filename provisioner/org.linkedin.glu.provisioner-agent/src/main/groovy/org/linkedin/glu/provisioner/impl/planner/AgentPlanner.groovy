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

package org.linkedin.glu.provisioner.impl.planner

import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.impl.agent.AgentUpgradeTouchpoint
import org.linkedin.glu.provisioner.impl.agent.AgentConstants
import org.linkedin.glu.provisioner.plan.api.LeafStep
import org.linkedin.glu.provisioner.plan.api.SequentialStep
import org.linkedin.glu.provisioner.plan.api.ParallelStep
import org.linkedin.glu.provisioner.api.planner.IAgentPlanner
import org.linkedin.glu.provisioner.impl.agent.AgentActionDescriptorFactory
import org.linkedin.glu.provisioner.core.environment.Installation
import org.linkedin.glu.provisioner.core.action.IDescriptionProvider
import org.linkedin.groovy.util.state.StateMachine
import org.linkedin.groovy.util.state.StateMachineImpl
import org.linkedin.glu.agent.api.Agent

/**
 * Planner to upgrade the agent.
 *
 * @author ypujante@linkedin.com */
class AgentPlanner implements IAgentPlanner
{
  private final StateMachine stateMachine =
    new StateMachineImpl([transitions: Agent.DEFAULT_TRANSITIONS])

  private AgentActionDescriptorFactory agentActionDescriptorFactory =
    AgentActionDescriptorFactory.INSTANCE

  /**
   * Create a plan to go to the provided list of states (one after the other) for the given
   * installation. Note that there is no validation whether it is possible or not.
   *
   * @param filter will be called for each step to filter it in or out of the plan
   * (return <code>true</code> to keep, <code>false</code> to reject)
   */
  public Plan<ActionDescriptor> createTransitionPlan(Collection<Installation> installations,
                                                     Collection<String> toStates,
                                                     IDescriptionProvider descriptionProvider,
                                                     Closure filter)
  {
    def steps = []

    installations.each { Installation installation ->
      steps.addAll(createTransitionSteps(installation, toStates, descriptionProvider, filter))
    }

    steps = new SequentialStep(null, [name: "Transition [${toStates.join('->')}]".toString()], steps)

    return new Plan(steps)
  }

  private def createTransitionSteps(Installation installation,
                                    Collection<String> toStates,
                                    IDescriptionProvider descriptionProvider,
                                    Closure filter)
  {
    def steps = []

    if(installation.transitionState)
    {
      addSteps(installation, 'noop', descriptionProvider, steps, filter)
    }
    else
    {
      def startState = installation.state

      toStates.each { toState ->
        if(startState == 'undeployed')
        {
          addSteps(installation, 'installscript', descriptionProvider, steps, filter)
          startState = StateMachine.NONE
        }

        def uninstallScript = false
        if(toState == 'undeployed')
        {
          toState = StateMachine.NONE
          uninstallScript = true
        }
        def path = stateMachine.findShortestPath(startState, toState)

        path.each { transition ->
          addSteps(installation, transition.action, descriptionProvider, steps, filter)
        }

        if(uninstallScript)
        {
          addSteps(installation, 'uninstallscript', descriptionProvider, steps, filter)
          toState = 'undeployed'
        }

        startState = toState
      }
    }

    return steps
  }

  private def addSteps(Installation installation,
                       String action,
                       IDescriptionProvider descriptionProvider,
                       def steps,
                       Closure filter)
  {
    def ad = agentActionDescriptorFactory.getActionDescriptor(action,
                                                              installation,
                                                              descriptionProvider)
    def step = new LeafStep(null, [name: ad.description], ad)
    if(filter(step))
      steps << step
  }

  /**
   * Create a plan to go from state <code>fromState</code> to state <code>toState</code>. Note that
   * there is no validation whether it is possible or not.
   */
  Plan<ActionDescriptor> createTransitionPlan(String agentName,
                                              String mountPoint,
                                              URI agentURI,
                                              String fromState,
                                              String toState,
                                              IDescriptionProvider descriptionProvider)
  {
    createTransitionPlan([new Installation([hostname: agentName,
                                          mount: mountPoint,
                                          uri: agentURI,
                                          name: mountPoint,
                                          state: fromState])],
                         [toState],
                         descriptionProvider) { true }
  }

  /**
   * Create a plan to go to the provided list of states (one after the other). Note that
   * there is no validation whether it is possible or not.
   */
  public Plan<ActionDescriptor> createTransitionPlan(String agentName,
                                                     String mountPoint,
                                                     URI agentURI,
                                                     String fromState,
                                                     Collection<String> toStates,
                                                     IDescriptionProvider descriptionProvider)
  {
    createTransitionPlan([new Installation([hostname: agentName,
                                          mount: mountPoint,
                                          uri: agentURI,
                                          name: mountPoint,
                                          state: fromState])],
                         toStates,
                         descriptionProvider) { true }
  }



  Plan<ActionDescriptor> createUpgradePlan(agents, String version, String coordinates)
  {
    def steps = []

    def mountPoint = "/upgrade/${version}".toString()

    agents.each { String agentName, URI agentURI ->
      def leafSteps = createUpgradeSteps(agentName, agentURI, version, coordinates)
      def mountPointSteps = new SequentialStep(null, [mountPoint: mountPoint], leafSteps)
      steps << new SequentialStep(null, [agent: agentName], [mountPointSteps])
    }

    return new Plan(new ParallelStep(null, [name: "Agent upgrade to version ${version}".toString()], steps))
  }

  public Plan<ActionDescriptor> createCleanupPlan(agents, String version)
  {
    def steps = []

    def mountPoint = "/upgrade/${version}".toString()

    agents.each { String agentName, URI agentURI ->
      def leafSteps = createCleanupSteps(agentName, agentURI, version)
      def mountPointSteps = new SequentialStep(null, [mountPoint: mountPoint], leafSteps)
      steps << new SequentialStep(null, [agent: agentName], [mountPointSteps])
    }

    return new Plan(new ParallelStep(null, [name: "Agent cleanup version ${version}".toString()], steps))
  }

  private def createUpgradeSteps(String agentName, URI agentURI, String version, String coordinates)
  {
    def steps = []

    def mountPoint = "/upgrade/${version}".toString()

    def descriptorProperties = [:]
    descriptorProperties[AgentConstants.AGENT_NAME] = agentName
    descriptorProperties[AgentConstants.AGENT_URI] = agentURI
    descriptorProperties[AgentConstants.MOUNT_POINT] = mountPoint

    steps << createLeafStep('installscript',
                            descriptorProperties,
                            [newVersion: version, agentTar: coordinates],
                            "Install agent upgrade script for upgrading to ${version}")

    steps << createLeafStep('install',
                            descriptorProperties,
                            [:],
                            "Run install action (download new version from ${coordinates})")

    steps << createLeafStep('prepare',
                            descriptorProperties,
                            [:],
                            "Moves the new agent into its proper location and restart the agent")

    steps << createLeafStep('waitForRestart',
                            descriptorProperties,
                            [:],
                            "Wait for the agent to restart")

    steps << createLeafStep('commitOrRollback',
                            descriptorProperties,
                            [:],
                            "Commit or rollback depending on success of upgrade")

    steps << createLeafStep('uninstall',
                            descriptorProperties,
                            [:],
                            "Runs the uninstall action")

    steps << createLeafStep('uninstallscript',
                            descriptorProperties,
                            [:],
                            "Uninstall the script")

    return steps
  }

  private def createCleanupSteps(String agentName, URI agentURI, String version)
  {
    def steps = []

    def mountPoint = "/upgrade/${version}".toString()

    def descriptorProperties = [:]
    descriptorProperties[AgentConstants.AGENT_NAME] = agentName
    descriptorProperties[AgentConstants.AGENT_URI] = agentURI
    descriptorProperties[AgentConstants.MOUNT_POINT] = mountPoint

    steps << createLeafStep('cleanup',
                            descriptorProperties,
                            [:],
                            "Cleanup left over script: ${version}")

    return steps
  }


  private LeafStep createLeafStep(String id, descriptorProperties, actionParams, String description)
  {
    def ad = new ActionDescriptor(id: id,
                                  actionName: id,
                                  type: AgentUpgradeTouchpoint.ID,
                                  descriptorProperties: descriptorProperties,
                                  actionParams: actionParams,
                                  description: description.toString())

    return new LeafStep(null, [name: description], ad)
  }
}
