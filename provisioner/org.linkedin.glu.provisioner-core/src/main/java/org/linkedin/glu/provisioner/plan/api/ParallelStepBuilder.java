/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2014 Yan Pujante
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

/**
 * @author ypujante@linkedin.com
 */
public class ParallelStepBuilder<T> extends CompositeStepBuilder<T>
{
  public ParallelStepBuilder(IPlanBuilder.Config config)
  {
    super(config);
  }

  @Override
  protected IStep<T> createStep(Collection<IStep<T>> steps)
  {
    Integer maxParallelStepsCount = getConfig().maxParallelStepsCount;

    if(maxParallelStepsCount == null ||
       maxParallelStepsCount < 1 ||
       steps.size() <= maxParallelStepsCount)
      return new ParallelStep<T>(getId(), getMetadata(), steps);
    else
    {
      // split the parallel plan into N parallel plans containing no more
      // than _maxParallelStepsCount each, executed sequentially
      SequentialStepBuilder<T> sequentialBuilder = new SequentialStepBuilder<T>(getConfig());
      sequentialBuilder.setId(getId());
      sequentialBuilder.setMetadata(getMetadata());
      sequentialBuilder.setMetadata("maxParallelStepsCount", maxParallelStepsCount);
      sequentialBuilder.setMetadata("parallelStepsCount", steps.size());

      int currentStepsCount = 0;
      int currentIndex = 0;

      ParallelStepBuilder<T> parallelBuilder = null;

      for(IStep<T> step : steps)
      {
        if(currentStepsCount == 0 || currentStepsCount >= maxParallelStepsCount)
        {
          parallelBuilder = sequentialBuilder.addStep(new ParallelStepBuilder<T>(getConfig()));
          parallelBuilder.setId(getId());
          parallelBuilder.setMetadata(getMetadata());
          parallelBuilder.setMetadata("sequentialIndex", currentIndex);
          currentStepsCount = 0;
          currentIndex++;
        }

        parallelBuilder.addStep(step);
        currentStepsCount++;
      }

      return sequentialBuilder.toStep();
    }
  }
}