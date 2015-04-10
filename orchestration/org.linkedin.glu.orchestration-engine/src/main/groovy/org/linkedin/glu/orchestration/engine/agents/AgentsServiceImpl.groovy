/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011-2014 Yan Pujante
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

import org.linkedin.glu.agent.api.Agent
import org.linkedin.glu.agent.api.MountPoint
import org.linkedin.glu.agent.rest.client.AgentFactory
import org.linkedin.glu.agent.tracker.AgentInfo
import org.linkedin.glu.agent.tracker.MountPointInfo
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.glu.provisioner.core.model.SystemEntry
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.orchestration.engine.fabric.Fabric
import org.linkedin.glu.orchestration.engine.tracker.TrackerService
import org.linkedin.groovy.util.state.StateMachine
import org.linkedin.util.lang.LangUtils
import org.linkedin.util.annotations.Initializable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.glu.orchestration.engine.action.descriptor.AgentURIProvider
import org.linkedin.glu.groovy.utils.plugins.PluginService
import org.linkedin.util.url.URLBuilder
import org.linkedin.glu.groovy.util.state.DefaultStateMachine

import org.linkedin.util.reflect.ObjectProxyBuilder
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.agent.api.TimeOutException

/**
 * @author ypujante
 */
class AgentsServiceImpl implements AgentsService, AgentURIProvider, MountPointStateProvider
{
  public static final String MODULE = AgentsServiceImpl.class.getName ();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private final StateMachine stateMachine = DefaultStateMachine.INSTANCE

  // will be dependency injected
  @Initializable(required = true)
  AgentFactory agentFactory

  @Initializable(required = true)
  TrackerService trackerService

  @Initializable(required = true)
  PluginService pluginService

  /**
   * when a communication exception is detected with the agent, it will sleep for this time
   * before trying again */
  @Initializable(required = false)
  Timespan agentRecoveryTimeout = Timespan.parse('5s')

  /**
   * when a communication exception is detected with the agent, it will retry a certain number of
   * times */
  @Initializable(required = false)
  int agentRecoveryNumRetries = 10

  @Override
  URI getAgentURI(String fabric, String agent) throws NoSuchAgentException
  {
    AgentInfo info = trackerService.getAgentInfo(fabric, agent)
    if(!info)
      throw new NoSuchAgentException(agent)
    return info.getURI()
  }

  @Override
  URI findAgentURI(String fabric, String agent)
  {
    return trackerService.getAgentInfo(fabric, agent)?.URI
  }

  def getAllInfosWithAccuracy(Fabric fabric)
  {
    return trackerService.getAllInfosWithAccuracy(fabric)
  }

  Map<String, AgentInfo> getAgentInfos(Fabric fabric)
  {
    return trackerService.getAgentInfos(fabric)
  }

  AgentInfo getAgentInfo(Fabric fabric, String agentName)
  {
    return trackerService.getAgentInfo(fabric, agentName)
  }

  boolean clearAgentInfo(Fabric fabric, String agentName)
  {
    return trackerService.clearAgentInfo(fabric, agentName)
  }

  Map<MountPoint, MountPointInfo> getMountPointInfos(Fabric fabric, String agentName)
  {
    trackerService.getMountPointInfos(fabric, agentName)
  }

  MountPointInfo getMountPointInfo(Fabric fabric, String agentName, mountPoint)
  {
    trackerService.getMountPointInfo(fabric, agentName, mountPoint)
  }

  def getFullState(args)
  {
    withRemoteAgent(args.fabric, args.id) { Agent agent ->
      agent.getFullState(args)
    }
  }

  boolean waitForState(String fabric, String agentName, def mountPoint, String state, def timeout)
  {
    trackerService.waitForState(fabric, agentName, mountPoint, state, timeout)
  }

  boolean waitForState(Fabric fabric, String agentName, def mountPoint, String state, def timeout)
  {
    trackerService.waitForState(fabric, agentName, mountPoint, state, timeout)
  }

  def clearError(args)
  {
    withRemoteAgent(args.fabric, args.id) { Agent agent ->
      agent.clearError(args)
    }
  }

  def uninstallScript(args)
  {
    withRemoteAgent(args.fabric, args.id) { Agent agent ->
      moveToState(agent, args.mountPoint, StateMachine.NONE, args.timeout)
      agent.uninstallScript(args)
    }
  }

  def forceUninstallScript(args)
  {
    withRemoteAgent(args.fabric, args.id) { Agent agent ->
      agent.uninstallScript(*:args, force: true)
    }
  }

  def interruptAction(args)
  {
    withRemoteAgent(args.fabric, args.id) { Agent agent ->
      agent.interruptAction(args)
      agent.waitForState(mountPoint: args.mountPoint,
                         state: args.state,
                         timeout: args.timeout)
    }
  }

  def ps(args)
  {
    withRemoteAgent(args.fabric, args.id) { Agent agent ->
      agent.ps()
    }
  }

  def sync(args)
  {
    withRemoteAgent(args.fabric, args.id) { Agent agent ->
      agent.sync()
    }
  }

  def kill(args)
  {
    withRemoteAgent(args.fabric, args.id) { Agent agent ->
      agent.kill(args.pid as long, args.signal as int)
    }
  }

  void tailLog(args, Closure closure)
  {
    withRemoteAgent(args.fabric, args.id) { Agent agent ->
      closure(agent.tailAgentLog(args))
    }
  }

  void streamFileContent(args, Closure closure)
  {
    pluginService.executeMethod(AgentsService, "pre_streamFileContent", args)

    withRemoteAgent(args.fabric, args.id) { Agent agent ->
      def res = agent.getFileContent(args)

      def pluginResult =
        pluginService.executeMethod(AgentsService, "post_streamFileContent", [*:args,
                                                                              serviceResult: res])
      
      // let the plugin customize res
      if(pluginResult != null)
        res = pluginResult

      closure(res)
    }
  }

  /**
   * Builds the current system model based on the live data from ZooKeeper
   */
  SystemModel getCurrentSystemModel(Fabric fabric)
  {
    def allInfosAndAccuracy = getAllInfosWithAccuracy(fabric)
    def agents = allInfosAndAccuracy.allInfos
    def accuracy = allInfosAndAccuracy.accuracy

    SystemModel systemModel = new SystemModel(fabric: fabric.name)

    // 1. add the agent tags
    agents.values().each { agent ->
      def agentName = agent.info.agentName
      def agentTags = agent.info.tags
      
      if(agentTags)
        systemModel.addAgentTags(agentName, agentTags)
    }

    // 2. add entries
    agents.values().each { agent ->
      def agentName = agent.info.agentName

      if(agent.mountPoints)
      {
        agent.mountPoints.values().each { MountPointInfo mp ->
          def entry = createSystemEntry(agentName, mp)
          if(entry)
            systemModel.addEntry(entry)
        }
      }
      else
      {
        // empty agent
        SystemEntry emptyAgentEntry = new SystemEntry(agent: agentName)
        emptyAgentEntry.metadata.emptyAgent = true
        emptyAgentEntry.metadata.currentState = 'NA'
        systemModel.addEntry(emptyAgentEntry)
      }
    }

    systemModel.metadata.accuracy = accuracy

    return systemModel
  }

  @Override
  def executeShellCommand(Fabric fabric, String agentName, def args)
  {
    withRemoteAgent(fabric, agentName) { Agent agent ->
      agent.executeShellCommand(args)
    }
  }

  @Override
  def waitForCommand(Fabric fabric, String agentName, def args)
  {
    withRecoverableRemoteAgent(fabric, agentName) { Agent agent ->
      agent.waitForCommand(args)
    }
  }

  @Override
  boolean waitForCommandNoTimeOutException(Fabric fabric, String agentName, def args)
  {
    try
    {
      waitForCommand(fabric, agentName, args)
      return true
    }
    catch(TimeOutException e)
    {
      return false
    }
  }

  @Override
  def streamCommandResults(Fabric fabric, String agentName, def args, Closure commandResultProcessor)
  {
    withRecoverableRemoteAgent(fabric, agentName) { Agent agent ->
      commandResultProcessor(agent.streamCommandResults(args))
    }
  }

  @Override
  boolean interruptCommand(Fabric fabric, String agentName, def args)
  {
    withRemoteAgent(fabric, agentName) { Agent agent ->
      agent.interruptCommand(args)
    } as boolean
  }

  /**
   * Create the system entry for the given agent and mountPoint.
   */
  protected SystemEntry createSystemEntry(agentName, MountPointInfo mp)
  {
    GluGroovyLangUtils.noExceptionWithValueOnException(null) {
      try
      {
        createSystemEntryWithException(agentName, mp)
      }
      catch(Throwable error)
      {
        createSystemEntryWithException(agentName, mp.invalidate(error))
      }
    }
  }

  /**
   * Create the system entry for the given agent and mountPoint.
   */
  protected SystemEntry createSystemEntryWithException(agentName, MountPointInfo mp)
  {
    SystemEntry se = new SystemEntry()

    se.agent = agentName
    se.mountPoint = mp.mountPoint.toString()

    se.parent = mp.parent
    Map data = LangUtils.deepClone(mp.data)
    def scriptFactory = data?.scriptDefinition?.scriptFactory
    se.script = scriptFactory?.location
    if(!se.script && scriptFactory?.className)
    {
      def uri = URLBuilder.createFromPath("/${scriptFactory.className}")
      uri.scheme = "class"
      scriptFactory?.classPath?.each {
        uri.addQueryParameter("cp", it)
      }
      se.script = uri.getURL()
    }

    // all the necessary values are stored in the init parameters
    data?.scriptDefinition?.initParameters?.each { k, v ->
      if(v != null)
      {
        switch(k)
        {
          case 'metadata':
            se.metadata = v
            break

          case 'tags':
            se.setTags(v)
            break

          default:
            se.initParameters[k] = v
            break
        }
      }
    }

    se.metadata.currentState = mp.currentState
    se.entryState = mp.currentState
    if(mp.transitionState)
      se.metadata.transitionState = mp.transitionState
    if(mp.error)
      se.metadata.error = mp.error
    se.metadata.modifiedTime = mp.modifiedTime
    if(data?.scriptState)
    {
      se.metadata.scriptState = data.scriptState
    }
    return se
  }

  protected def moveToState(agent, mountPoint, toState, timeout)
  {
    def state = agent.getState(mountPoint: mountPoint)

    if(state.error)
    {
      agent.clearError(mountPoint: mountPoint)
    }

    def path = stateMachine.findShortestPath(state.currentState, toState)

    path.each { transition ->
      agent.executeAction(mountPoint: mountPoint,
                          action: transition.action)
      agent.waitForState(mountPoint: mountPoint,
                         state: transition.to,
                         timeout: timeout)
    }
  }

  protected def withRemoteAgent(Fabric fabric, String agentName, Closure closure)
  {
    AgentInfo info = getAgentInfo(fabric, agentName)

    if(!info)
      throw new NoSuchAgentException(agentName)
    
    agentFactory.withRemoteAgent(info.getURI()) { Agent agent ->
      closure(agent)
    }
  }

  /**
   * Compute the agent URI and call the closure with the {@link Agent}.
   */
  private def withRecoverableRemoteAgent(Fabric fabric, String agentName, Closure closure)
  {
    withRemoteAgent(fabric, agentName) { Agent agent ->
      def agentProxy = new RecoverableAgent(agent, agentRecoveryNumRetries, agentRecoveryTimeout)
      agent = ObjectProxyBuilder.createProxy(agentProxy, Agent.class)
      closure(agent)
    }
  }


}
