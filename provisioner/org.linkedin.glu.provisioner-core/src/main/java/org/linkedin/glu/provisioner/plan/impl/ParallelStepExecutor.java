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

package org.linkedin.glu.provisioner.plan.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.linkedin.glu.provisioner.plan.api.CompositeStepCompletionStatus;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus;
import org.linkedin.glu.provisioner.plan.api.ParallelStep;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author ypujante@linkedin.com
 */
public class ParallelStepExecutor<T> extends CompositeStepExecutor<T> implements IStepExecutor<T>
{
  public static final String MODULE = ParallelStepExecutor.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static int MAX_PARALLEL_DEPLOYMENTS = 0;

  public static void setMaxParallelDeployments(int size)
  {
    MAX_PARALLEL_DEPLOYMENTS = size;
  }

  /**
   * Constructor
   */
  public ParallelStepExecutor(ParallelStep<T> step, StepExecutionContext<T> context)
  {
    super(step, context);
  }

  @Override
  protected IStepCompletionStatus<T> doExecute() throws InterruptedException
  {
    Collection<IStepCompletionStatus<T>> status = new ArrayList<IStepCompletionStatus<T>>();
    Collection<IStep<T>> steps = getCompositeStep().getSteps();
    if (MAX_PARALLEL_DEPLOYMENTS <= 1 || steps.size() <= MAX_PARALLEL_DEPLOYMENTS)
    {
      // Trivial case: either we don't want buckets, or the number of steps fits in one bucket
      doExecuteInParallel(steps, status);
    }
    else
    {
      // First: break the parallel execution into buckets of 'MAX_PARALLEL_DEPLOYMENTS'
      Collection<Collection<IStep<T>>> buckets = stepBuckets(steps, MAX_PARALLEL_DEPLOYMENTS);
      int i = 1;
      for (Collection<IStep<T>> bucket : buckets)
      {
        // Then: run each bucket sequentially (to avoid overwhelming the system with one giant massive parallel deployment)
        if(log.isDebugEnabled())
          debug("executing bucket " + i + "/" + buckets.size());
        doExecuteInParallel(bucket, status);
        i++;
      }
    }
    return new CompositeStepCompletionStatus<T>(getCompositeStep(), status);
  }

  /**
   * Execute given set of jobs in parallel
   * @param bucket: the steps to execute in parallel
   * @param status: status of the executions
   * @throws InterruptedException
   */
  private void doExecuteInParallel(Collection<IStep<T>> bucket, Collection<IStepCompletionStatus<T>> status) throws InterruptedException
  {
    int i = 0;
    for(IStep<T> step : bucket)
    {
      if(log.isDebugEnabled())
        debug("executing step " + i);
      createChildExecutor(step).execute();
      i++;
    }
    i = 0;
    Collection<IStepExecutor<T>> childValues = getChildrenExecutors().values();
    for(IStepExecutor<T> executor : childValues)
    {
      if(log.isDebugEnabled())
        debug("waiting for step " + i);
      status.add(executor.waitForCompletion());
      i++;
    }
  }

  /**
   * 'steps' broken into buckets of 'size'
   * @param steps: the collection of steps to break into buckets of 'size'
   * @param size: size for each set
   * @return
   */
  private Collection<Collection<IStep<T>>> stepBuckets(Collection<IStep<T>> steps, int size)
  {
    assert size > 1;
    assert steps.size() > size;
    int nb = steps.size() / size;
    Collection<Collection<IStep<T>>> buckets = new ArrayList<Collection<IStep<T>>>(nb + 1);
    Collection<IStep<T>> current_bucket = null;
    for (IStep<T> step : steps)
    {
      if (current_bucket == null || current_bucket.size() >= size)
      {
        current_bucket = new ArrayList<IStep<T>>(size);
        buckets.add(current_bucket);
      }
      current_bucket.add(step);
    }
    return buckets;
  }
}
