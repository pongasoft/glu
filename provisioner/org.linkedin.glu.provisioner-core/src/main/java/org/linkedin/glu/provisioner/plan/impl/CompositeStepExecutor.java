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

import org.linkedin.glu.provisioner.plan.api.CompositeStep;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.IStepCompletionStatus;
import org.linkedin.glu.provisioner.plan.api.CompositeStepCompletionStatus;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.ArrayList;

/**
 * @author ypujante@linkedin.com
 */
public abstract class CompositeStepExecutor<T> extends AbstractStepExecutor<T>
{
  private final CompositeStep<T> _compositeStep;
  private final Map<IStep<T>, IStepExecutor<T>> _childrenExecutors;

  protected CompositeStepExecutor(CompositeStep<T> step, StepExecutionContext<T> context)
  {
    super(step, context);
    _compositeStep = step;
    _childrenExecutors = new LinkedHashMap<IStep<T>, IStepExecutor<T>>(_compositeStep.getSteps().size());
  }

  protected CompositeStep<T> getCompositeStep()
  {
    return _compositeStep;
  }

  protected synchronized IStepExecutor<T> createChildExecutor(IStep<T> childStep)
  {
    IStepExecutor<T> executor = _childrenExecutors.get(childStep);
    if(executor == null)
    {
      executor = getContext().createExecutor(childStep);

      if(isPaused())
        executor.pause();

      if(isCancelled())
        executor.cancel(true);
      
      _childrenExecutors.put(childStep, executor);
    }

    return executor;
  }

  protected Map<IStep<T>, IStepExecutor<T>> getChildrenExecutors()
  {
    return _childrenExecutors;
  }

  @Override
  protected IStepCompletionStatus<T> doCancel(boolean started)
  {
    Collection<IStepCompletionStatus<T>> status = new ArrayList<IStepCompletionStatus<T>>();

    for(IStep<T> step : getCompositeStep().getSteps())
    {
      IStepExecutor<T> executor = createChildExecutor(step);
      executor.cancel(true);

      status.add(executor.getCompletionStatus());
    }

    return new CompositeStepCompletionStatus<T>(getCompositeStep(), status);
  }

  @Override
  protected IStepCompletionStatus<T> createCompletionStatus(IStepCompletionStatus.Status status,
                                                            Throwable throwable)
  {
    CompositeStepCompletionStatus<T> executionStatus;
    executionStatus = new CompositeStepCompletionStatus<T>(getCompositeStep(),
                                                           getStartTime(),
                                                           getStartTime(),
                                                           status,
                                                           throwable);
    return executionStatus;
  }

  @Override
  public synchronized void pause()
  {
    super.pause();
    for(IStepExecutor<T> executor : _childrenExecutors.values())
    {
      executor.pause();
    }
  }

  @Override
  public synchronized void resume()
  {
    super.resume();
    for(IStepExecutor<T> executor : _childrenExecutors.values())
    {
      executor.resume();
    }
  }
}
