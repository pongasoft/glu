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

package org.linkedin.glu.provisioner.plan.impl;

import org.linkedin.glu.provisioner.plan.api.IPlanExecutor;
import org.linkedin.glu.provisioner.plan.api.IPlanExecution;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.glu.provisioner.plan.api.IPlanExecutionProgressTracker;
import org.linkedin.glu.provisioner.plan.api.ILeafStepExecutor;
import org.linkedin.util.annotations.Initializer;
import org.linkedin.util.clock.Clock;
import org.linkedin.util.clock.SystemClock;

import java.util.concurrent.ExecutorService;


/**
 * @author ypujante@linkedin.com
 */
public class PlanExecutor<T> implements IPlanExecutor<T>
{
  private Clock _clock = SystemClock.instance();
  private ExecutorService _executorService;
  private ILeafStepExecutor<T> _leafStepExecutor;

  /**
   * Constructor
   */
  public PlanExecutor(ExecutorService executorService,
                      ILeafStepExecutor<T> leafStepExecutor)
  {
    _executorService = executorService;
    _leafStepExecutor = leafStepExecutor;
  }

  /**
   * For dependency injection
   */
  public PlanExecutor()
  {
  }

  public ExecutorService getExecutorService()
  {
    return _executorService;
  }

  @Initializer
  public void setExecutorService(ExecutorService executorService)
  {
    _executorService = executorService;
  }

  public Clock getClock()
  {
    return _clock;
  }

  @Initializer
  public void setClock(Clock clock)
  {
    _clock = clock;
  }

  public ILeafStepExecutor<T> getLeafStepExecutor()
  {
    return _leafStepExecutor;
  }

  @Initializer
  public void setLeafStepExecutor(ILeafStepExecutor<T> leafStepExecutor)
  {
    _leafStepExecutor = leafStepExecutor;
  }

  /**
   * Execute the provided plan. Note that this call is non blocking and will return an execution
   * object with which to interract.
   *
   * @param plan the plane to execute
   * @return the execution
   */
  @Override
  public IPlanExecution<T> executePlan(Plan<T> plan)
  {
    return executePlan(plan, null);
  }

  /**
   * Execute the provided plan. Note that this call is non blocking and will return an execution
   * object with which to interract.
   *
   * @param plan            the plane to execute
   * @param progressTracker to track the progress of the execution
   * @return the execution
   */
  @Override
  public IPlanExecution<T> executePlan(Plan<T> plan,
                                       IPlanExecutionProgressTracker<T> progressTracker)
  {
    StepExecutionContext<T> ctx = new StepExecutionContext<T>(_executorService,
                                                              _leafStepExecutor,
                                                              progressTracker,
                                                              _clock);

    return ctx.executePlan(plan);
  }
}
