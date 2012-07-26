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
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus;
import org.linkedin.glu.provisioner.plan.api.LeafStep;
import org.linkedin.glu.provisioner.plan.api.LeafStepCompletionStatus;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @author ypujante@linkedin.com
 */
public class LeafStepExecutor<T> extends AbstractStepExecutor<T> implements IStepExecutor<T>
{
  public static final String MODULE = LeafStepExecutor.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  /**
   * Constructor
   */
  public LeafStepExecutor(LeafStep<T> step, StepExecutionContext<T> context)
  {
    super(step, context);
  }

  /**
   * Submits the job to the appropriate service executor.
   */
  @Override
  public <V> Future<V> submit(Callable<V> callable)
  {
    return _context.getLeafStepExecutorService().submit(callable);
  }

  @Override
  protected IStepCompletionStatus<T> doExecute()
  {
    IStepCompletionStatus.Status status = IStepCompletionStatus.Status.COMPLETED;
    Throwable throwable = null;

    LeafStep<T> step = (LeafStep<T>) getStep();

    if(log.isDebugEnabled())
      debug("leaf execution");

    try
    {
      getContext().executeLeafStep(step);
    }
    catch(InterruptedException e)
    {
      status = IStepCompletionStatus.Status.CANCELLED;
    }
    catch(Throwable th)
    {
      throwable = th;
      status = IStepCompletionStatus.Status.FAILED;
    }

    return createCompletionStatus(status, throwable);
  }

  @Override
  protected IStepCompletionStatus<T> doCancel(boolean started)
  {
    IStepCompletionStatus.Status status = 
      started ? IStepCompletionStatus.Status.CANCELLED : IStepCompletionStatus.Status.SKIPPED;
    return createCompletionStatus(status, null);
  }

  @Override
  protected IStepCompletionStatus<T> createCompletionStatus(IStepCompletionStatus.Status status,
                                                            Throwable throwable)
  {

    LeafStepCompletionStatus<T> executionStatus;
    long endTime = status == IStepCompletionStatus.Status.SKIPPED ? getStartTime() : getContext().currentTimeMillis();
    executionStatus = new LeafStepCompletionStatus<T>((LeafStep<T>) getStep(),
                                                      getStartTime(),
                                                      endTime,
                                                      status,
                                                      throwable);
    return executionStatus;
  }
}
