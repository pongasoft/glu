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

package org.linkedin.glu.provisioner.plan.api;

import org.linkedin.util.lang.LangUtils;

/**
 * @author ypujante@linkedin.com
 */
public class PlanBuilder<T> extends AbstractStepBuilder<T> implements IPlanBuilder<T>
{
  private ICompositeStepBuilder<T> _compositeSteps;
  private IStep<T> _singleStep;
  private final Config _config;

  /**
   * Constructor
   */
  public PlanBuilder(Config config)
  {
    _config = config;
  }

  @Override
  public void addLeafStep(LeafStep<T> tLeafStep)
  {
    if(tLeafStep == null)
      return;

    if(_singleStep != null || _compositeSteps != null)
      throw new IllegalStateException("only a single root level step is allowed");

    _singleStep = tLeafStep;
  }

  @Override
  public void removeLeafStep(LeafStep<T> tLeafStep)
  {
    if(LangUtils.isEqual(_singleStep, tLeafStep))
      _singleStep = null;
  }

  @Override
  public ICompositeStepBuilder<T> addCompositeSteps(IStep.Type type)
  {
    if(_singleStep != null || _compositeSteps != null)
      throw new IllegalStateException("only a single root level step is allowed");

    switch(type)
    {
      case PARALLEL:
        _compositeSteps = new ParallelStepBuilder<T>(_config);
        break;

      case SEQUENTIAL:
        _compositeSteps = new SequentialStepBuilder<T>(_config);
        break;

      default:
        throw new RuntimeException("cannot create a builder for " + type);
    }

    return _compositeSteps;
  }

  @Override
  public ICompositeStepBuilder<T> addParallelSteps()
  {
    return addCompositeSteps(IStep.Type.PARALLEL);
  }

  @Override
  public ICompositeStepBuilder<T> addSequentialSteps()
  {
    return addCompositeSteps(IStep.Type.SEQUENTIAL);
  }

  @Override
  public IStep<T> toStep()
  {
    IStep<T> step = _singleStep;

    if(_compositeSteps != null)
      step = _compositeSteps.toStep();

    return step;
  }

  @Override
  public Plan<T> toPlan()
  {
    return new Plan<T>(getMetadata(), toStep());
  }
}
