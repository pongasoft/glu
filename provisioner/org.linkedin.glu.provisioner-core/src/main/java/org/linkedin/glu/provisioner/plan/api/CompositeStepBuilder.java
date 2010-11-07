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

package org.linkedin.glu.provisioner.plan.api;

import java.util.Collection;
import java.util.ArrayList;

/**
 * @author ypujante@linkedin.com
 */
public abstract class CompositeStepBuilder<T> extends AbstractStepBuilder<T> implements ICompositeStepBuilder<T>
{
  private final Collection<Object> _steps = new ArrayList<Object>();

  /**
   * Constructor
   */
  public CompositeStepBuilder()
  {
  }

  @Override
  public void addLeafStep(LeafStep<T> leafStep)
  {
    _steps.add(leafStep);
  }

  @Override
  public void removeLeafStep(LeafStep<T> leafStep)
  {
    _steps.remove(leafStep);
  }

  @Override
  public ICompositeStepBuilder<T> addSequentialSteps()
  {
    SequentialStepBuilder<T> stepBuilder = new SequentialStepBuilder<T>();
    _steps.add(stepBuilder);
    return stepBuilder;
  }

  @Override
  public ICompositeStepBuilder<T> addParallelSteps()
  {
    ParallelStepBuilder<T> stepBuilder = new ParallelStepBuilder<T>();
    _steps.add(stepBuilder);
    return stepBuilder;
  }

  @Override
  public ICompositeStepBuilder<T> addCompositeSteps(IStep.Type type)
  {
    switch(type)
    {
      case PARALLEL:
        return addParallelSteps();

      case SEQUENTIAL:
        return addSequentialSteps();

      default:
        throw new IllegalArgumentException(type + " not supported");
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public IStep<T> toStep()
  {
    Collection<IStep<T>> steps = new ArrayList<IStep<T>>();

    for(Object step : _steps)
    {
      if(step instanceof IStep)
      {
        steps.add((IStep<T>) step);
      }
      else
      {
        IStepBuilder<T> builder = (IStepBuilder<T>) step;
        IStep<T> childStep = builder.toStep();
        if(childStep instanceof CompositeStep)
        {
          CompositeStep compositeStep = (CompositeStep) childStep;
          if(compositeStep.getSteps().size() > 0)
            steps.add(childStep);
        }
        else
          steps.add(childStep);
      }
    }

    return createStep(steps);
  }

  protected abstract IStep<T> createStep(Collection<IStep<T>> steps);
}