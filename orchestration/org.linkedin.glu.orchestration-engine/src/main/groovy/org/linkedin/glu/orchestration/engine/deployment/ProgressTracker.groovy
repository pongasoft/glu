/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

import org.linkedin.glu.provisioner.core.model.SystemModel
import org.linkedin.glu.provisioner.plan.api.FilteredPlanExecutionProgressTracker
import org.linkedin.glu.provisioner.plan.api.IPlanExecution
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus
import org.linkedin.glu.groovy.utils.plugins.PluginService

/**
 * @author yan@pongasoft.com */
class ProgressTracker<T> extends FilteredPlanExecutionProgressTracker<T>
{
  final DeploymentStorage _deploymentStorage
  final PluginService _pluginService
  def final _deploymentId
  private IPlanExecution _planExecution
  private final SystemModel _model
  private final String _description

  def ProgressTracker(DeploymentStorage deploymentStorage,
                      PluginService pluginService,
                      tracker,
                      deploymentId,
                      SystemModel model,
                      String description)
  {
    super(tracker)
    _deploymentStorage = deploymentStorage
    _pluginService = pluginService
    _deploymentId = deploymentId
    _model = model
    _description = description
  }

  public void onPlanStart(IPlanExecution<T> planExecution)
  {
    super.onPlanStart(planExecution)
    _planExecution = planExecution
    _pluginService?.executeMethod(DeploymentService,
                                 "onStart_executeDeploymentPlan",
                                 [
                                   model: _model,
                                   plan: _planExecution.plan,
                                   description: _description,
                                   deploymentId: _deploymentId
                                 ])
  }


  public void onPlanEnd(IStepCompletionStatus<T> status)
  {
    super.onPlanEnd(status)
    String details = _planExecution.toXml([fabric: _model.fabric, systemId: _model.id])
    _deploymentStorage.endDeployment(_deploymentId, status, details)
    _pluginService?.executeMethod(DeploymentService,
                                 "post_executeDeploymentPlan",
                                 [
                                   model: _model,
                                   plan: _planExecution.plan,
                                   description: _description,
                                   deploymentId: _deploymentId,

                                   serviceResult: _planExecution
                                 ])
  }
}
