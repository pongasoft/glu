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

package org.linkedin.glu.orchestration.engine.deployment

import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor
import org.linkedin.glu.orchestration.engine.authorization.AuthorizationService
import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.plan.api.IPlanExecutionProgressTracker
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.util.annotations.Initializable
import org.linkedin.util.clock.Clock
import org.linkedin.util.clock.SystemClock
import org.linkedin.util.clock.Timespan
import org.linkedin.util.lifecycle.Startable
import org.linkedin.util.lifecycle.Destroyable
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.linkedin.groovy.util.clock.ClosureTimerTask
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus
import org.linkedin.glu.groovy.utils.plugins.PluginService

/**
 * System service.
 *
 * @author ypujante@linkedin.com */
class DeploymentServiceImpl implements DeploymentService, Startable, Destroyable
{
  public static final String MODULE =  DeploymentServiceImpl.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  // we keep entries in the plan cache no more than 5m
  @Initializable
  Timespan planCacheTimeout = Timespan.parse('5m')

  /**
   * How often to run the auto archive timer
   */
  @Initializable
  Timespan autoArchiveTimerFrequency = Timespan.parse('1m')

  // we keep entries (<code>CurrentDeployment</code>) in memory no longer than the provided
  // time (set it to <code>null</code> (or 0) if you do not want to auto archive)
  @Initializable
  Timespan autoArchiveTimeout = Timespan.parse('30m')

  @Initializable
  Clock clock = SystemClock.INSTANCE

  @Initializable(required = true)
  Deployer deployer

  @Initializable(required = true)
  DeploymentStorage deploymentStorage

  @Initializable
  AuthorizationService authorizationService

  @Initializable
  PluginService pluginService

  private Map<String, CurrentDeployment> _deployments = [:]
  private Map<String, Plan> _plans = [:]

  private Timer _autoArchiveTimer = null

  @Override
  void destroy()
  {
    _autoArchiveTimer?.cancel()
  }

  @Override
  void start()
  {
    if(autoArchiveTimeout?.durationInMilliseconds > 0)
    {
      log.info("Starting auto-archive thread")
      _autoArchiveTimer = new Timer("DeploymentServiceTimer", true)
      _autoArchiveTimer.scheduleAtFixedRate(new ClosureTimerTask(autoArchiveClosure),
                                            autoArchiveTimerFrequency.durationInMilliseconds,
                                            autoArchiveTimerFrequency.durationInMilliseconds)
    }
    else
    {
      log.info("Auto-archive thread not started")
    }
  }

  Plan<ActionDescriptor> getPlan(String id)
  {
    synchronized(_plans)
    {
      return _plans[id]
    }
  }

  void savePlan(Plan<ActionDescriptor> plan)
  {
    synchronized(_plans)
    {
      // first we remove all old entries
      def cutoffTime = planCacheTimeout.pastTimeMillis(clock)
      _plans = _plans.findAll { k,v -> v.metadata.savedTime > cutoffTime }

      plan.metadata.savedTime = clock.currentTimeMillis()
      _plans[plan.id] = plan
    }
  }

  Collection<Plan<ActionDescriptor>> getPlans(String fabric)
  {
    synchronized(_plans)
    {
      _plans.values().findAll { Plan<ActionDescriptor> plan ->
        plan.metadata.fabric == fabric
      }
    }
  }

  Collection<CurrentDeployment> getDeployments(String fabric)
  {
    synchronized(_deployments)
    {
      return _deployments.values().findAll { it.fabric == fabric }
    }
  }

  /**
   * Returns all the deployments matching the closure
   */
  Collection<CurrentDeployment> getDeployments(String fabric, Closure closure)
  {
    synchronized(_deployments)
    {
      _deployments.values().findAll { it.fabric == fabric && closure(it) }
    }
  }

  @Override
  Collection<CurrentDeployment> getDeployments(String fabric, String planId)
  {
    getDeployments(fabric) { CurrentDeployment deployment ->
      deployment.planExecution?.plan?.id == planId
    }
  }

  boolean archiveDeployment(String id)
  {
    synchronized(_deployments)
    {
      def deployment = _deployments[id]
      if(deployment)
      {
        if(deployment.planExecution.isCompleted())
        {
          _deployments.remove(id)
          return true
        }
        else
        {
          throw new IllegalStateException("cannot archive a running deployment")
        }
      }
      else
      {
        return false
      }
    }
  }

  /**
   * run on a regular basis to automatically archive 'old' deployments. YP implementation note: the
   * check on 'old' is based on when the deployment was completed!
   */
  def autoArchiveClosure = {
    synchronized(_deployments)
    {
      def cutoffTime = autoArchiveTimeout.pastTimeMillis(clock)

      def deploymentsToArchive = _deployments.values().findAll { CurrentDeployment deployment ->
        IStepCompletionStatus status = deployment.planExecution.completionStatus

        // completed more than 'cutoff' time ago
        return status != null && status.endTime <= cutoffTime
      }

      deploymentsToArchive.each {
        _deployments.remove(it.id)
      }

      if(deploymentsToArchive.size())
        log.info "Auto-archived ${deploymentsToArchive.size()} deployments"
    }
  }

  /**
   * Archive all deployments (that are completed of course)
   * @return the number of archived deployments
   */
  int archiveAllDeployments(String fabric)
  {
    synchronized(_deployments)
    {
      def deploymentsToArchive = getDeployments(fabric) {
        it.planExecution.isCompleted()
      }

      deploymentsToArchive.each { _deployments.remove(it.id) }

      return deploymentsToArchive.size()
    }
  }
  
  CurrentDeployment getDeployment(String id)
  {
    synchronized(_deployments)
    {
      _deployments[id]
    }
  }

  ArchivedDeployment getArchivedDeployment(String id)
  {
    deploymentStorage.getArchivedDeployment(id)
  }

  @Override
  Map getArchivedDeployments(String fabric,
                             boolean includeDetails,
                             params)
  {
    deploymentStorage.getArchivedDeployments(fabric, includeDetails, params)
  }

  @Override
  int getArchivedDeploymentsCount(String fabric)
  {
    deploymentStorage.getArchivedDeploymentsCount(fabric)
  }

  @Override
  Deployment getCurrentOrArchivedDeployment(String id)
  {
    getDeployment(id) ?: getArchivedDeployment(id)
  }

  boolean isExecutingDeploymentPlan(String fabric)
  {
    synchronized(_deployments)
    {
      return _deployments.values().any { it.fabric == fabric && !it.planExecution.isCompleted() }
    }
  }

  CurrentDeployment executeDeploymentPlan(SystemModel system, Plan plan)
  {
    executeDeploymentPlan(system, plan, plan.name, null)
  }

  CurrentDeployment executeDeploymentPlan(SystemModel model,
                                          Plan plan,
                                          String description,
                                          IPlanExecutionProgressTracker progressTracker)
  {
    def pluginArgs = [model: model, plan: plan, description: description]

    pluginService?.executeMethod(DeploymentService,
                                 "pre_executeDeploymentPlan",
                                 pluginArgs)

    (model, plan, description) = [pluginArgs.model, pluginArgs.plan, pluginArgs.description]

    synchronized(_deployments)
    {
      String username = authorizationService?.getExecutingPrincipal()

      ArchivedDeployment deployment =
        deploymentStorage.startDeployment(description,
                                          model.fabric,
                                          username,
                                          plan.toXml())

      def id = deployment.id

      def tracker = new ProgressTracker(deploymentStorage,
                                        pluginService,
                                        progressTracker,
                                        id,
                                        model,
                                        description)

      def planExecution = deployer.executePlan(plan, tracker)

      CurrentDeployment currentDeployment = new CurrentDeployment(id: id,
                                                                  username: username,
                                                                  fabric: model.fabric,
                                                                  systemId: model.id,
                                                                  planExecution: planExecution,
                                                                  description: description,
                                                                  progressTracker: progressTracker)
      _deployments[id] = currentDeployment

      return currentDeployment
    }
  }
}

