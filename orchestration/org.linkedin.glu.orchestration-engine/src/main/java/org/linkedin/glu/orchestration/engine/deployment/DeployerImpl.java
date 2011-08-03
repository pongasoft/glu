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

package org.linkedin.glu.orchestration.engine.deployment;

import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor;
import org.linkedin.glu.provisioner.plan.api.IPlanExecution;
import org.linkedin.glu.provisioner.plan.api.IPlanExecutionProgressTracker;
import org.linkedin.glu.provisioner.plan.api.IPlanExecutor;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.util.annotations.Initializer;

/**
 * @author yan@pongasoft.com
 */
public class DeployerImpl implements Deployer
{
  /**
   * The plan executor
   */
  private IPlanExecutor<ActionDescriptor> _planExecutor;

  /**
   * Constructor
   */
  public DeployerImpl()
  {
  }

  public IPlanExecutor<ActionDescriptor> getPlanExecutor()
  {
    return _planExecutor;
  }

  @Initializer(required = true)
  public void setPlanExecutor(IPlanExecutor<ActionDescriptor> planExecutor)
  {
    _planExecutor = planExecutor;
  }

  @Override
  public IPlanExecution<ActionDescriptor> executePlan(Plan<ActionDescriptor> plan,
                                                      IPlanExecutionProgressTracker<ActionDescriptor> tracker)
  {
    return _planExecutor.executePlan(plan, tracker);
  }
}
