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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;

import org.linkedin.glu.provisioner.plan.api.ILeafStepExecutor;
import org.linkedin.glu.provisioner.plan.api.LeafStep;
import org.linkedin.glu.provisioner.plan.api.IPlanExecutionProgressTracker;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.SequentialStep;
import org.linkedin.glu.provisioner.plan.api.ParallelStep;
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.glu.provisioner.plan.api.IStepExecution;
import org.linkedin.glu.provisioner.plan.api.NoOpPlanExecutionProgressTracker;
import org.linkedin.glu.provisioner.plan.api.IPlanExecution;
import org.linkedin.util.clock.Clock;
import org.linkedin.util.reflect.ObjectProxyBuilder;

/**
 * @author ypujante@linkedin.com
 */
public class StepExecutionContext<T> implements IPlanExecutionProgressTracker<T>
{
  public static final String MODULE = StepExecutionContext.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private final ExecutorService _executorService;
  private final ILeafStepExecutor<T> _leafStepExecutor;
  private final IPlanExecutionProgressTracker<T> _originalTracker;
  private final Clock _clock;

  private IPlanExecutionProgressTracker<T> _tracker;
  private AsyncPlanExecutionProgressTrackerProxy<T> _trackerProxy;

  /**
   * Constructor
   */
  public StepExecutionContext(ExecutorService executorService,
                              ILeafStepExecutor<T> leafStepExecutor,
                              IPlanExecutionProgressTracker<T> tracker,
                              Clock clock)
  {
    _executorService = executorService;
    _leafStepExecutor = leafStepExecutor;
    _originalTracker = tracker;
    _clock = clock;
  }

  public Clock getClock()
  {
    return _clock;
  }

  public ExecutorService getExecutorService()
  {
    return _executorService;
  }

  public <V> Future<V> submit(Callable<V> callable)
  {
    return _executorService.submit(callable);
  }

  public ILeafStepExecutor<T> getLeafStepExecutor()
  {
    return _leafStepExecutor;
  }

  public void executeLeafStep(LeafStep<T> leafStep) throws Exception
  {
    _leafStepExecutor.executeLeafStep(leafStep);
  }

  public long currentTimeMillis()
  {
    return _clock.currentTimeMillis();
  }

  public IStepExecutor<T> createExecutor(IStep<T> step)
  {
    switch(step.getType())
    {
      case LEAF:
        return new LeafStepExecutor<T>((LeafStep<T>) step, this);

      case SEQUENTIAL:
        return new SequentialStepExecutor<T>((SequentialStep<T>) step, this);

      case PARALLEL:
        return new ParallelStepExecutor<T>((ParallelStep<T>) step, this);

      default:
        throw new RuntimeException("not reached");
    }
  }

  public IPlanExecution<T> executePlan(Plan<T> plan)
  {
    final IStepExecutor<T> stepExecutor = createExecutor(plan.getStep());

    PlanExecution<T> planExecution = new PlanExecution<T>(plan, stepExecutor);

    onPlanStart(planExecution);

    stepExecutor.execute();

    _executorService.submit(new Callable<Void>()
    {
      @Override
      public Void call() throws Exception
      {
        // we wait for the plan to complete
        IStepCompletionStatus<T> planExecutionStatus = stepExecutor.waitForCompletion();
        onPlanEnd(planExecutionStatus);
        return null;
      }
    });

    return planExecution;
  }

  @Override
  public void onCancelled(IStep<T> tiStep)
  {
    _tracker.onCancelled(tiStep);
  }

  @Override
  public void onPause(IStep<T> tiStep)
  {
    _tracker.onPause(tiStep);
  }

  @Override
  public void onPlanEnd(IStepCompletionStatus<T> tiStepCompletionStatus)
  {
    _tracker.onPlanEnd(tiStepCompletionStatus);
    _trackerProxy.shutdown();
    try
    {
      _trackerProxy.waitForShutdown(1000);
    }
    catch(Exception e)
    {
      if(log.isDebugEnabled())
        log.debug("ignored exception", e);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void onPlanStart(IPlanExecution<T> tPlan)
  {
    IPlanExecutionProgressTracker<T> tracker = _originalTracker;

    if(tracker == null)
      tracker = NoOpPlanExecutionProgressTracker.instance();

    _trackerProxy = new AsyncPlanExecutionProgressTrackerProxy<T>(tracker);
    _tracker = ObjectProxyBuilder.createProxy(_trackerProxy, IPlanExecutionProgressTracker.class);

    _tracker.onPlanStart(tPlan);
  }

  @Override
  public void onResume(IStep<T> tiStep)
  {
    _tracker.onResume(tiStep);
  }

  @Override
  public void onStepEnd(IStepCompletionStatus<T> tiStepCompletionStatus)
  {
    _tracker.onStepEnd(tiStepCompletionStatus);
  }

  @Override
  public void onStepStart(IStepExecution<T> stepExecution)
  {
    _tracker.onStepStart(stepExecution);
  }
}
