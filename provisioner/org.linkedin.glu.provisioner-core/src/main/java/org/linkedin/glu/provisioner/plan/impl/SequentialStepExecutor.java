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
import org.linkedin.glu.provisioner.plan.api.SequentialStep;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author ypujante@linkedin.com
 */
public class SequentialStepExecutor<T> extends CompositeStepExecutor<T> implements IStepExecutor<T>
{
  public static final String MODULE = SequentialStepExecutor.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  /**
   * Constructor
   */
  public SequentialStepExecutor(SequentialStep<T> step, StepExecutionContext<T> context)
  {
    super(step, context);
  }

  @Override
  protected IStepCompletionStatus<T> doExecute() throws InterruptedException
  {
    Collection<IStepCompletionStatus<T>> status = new ArrayList<IStepCompletionStatus<T>>();

    boolean needToSkip = false;
    int i = 0;

    for(IStep<T> step : getCompositeStep().getSteps())
    {
      IStepExecutor<T> stepExecutor = createChildExecutor(step);

      if(needToSkip)
      {
        if(log.isDebugEnabled())
          debug("canceling step " + i);

        stepExecutor.cancel(true);
      }
      else
      {
        if(log.isDebugEnabled())
          debug("executing step " + i);

        stepExecutor.execute();
      }

      IStepCompletionStatus<T> stepStatus = stepExecutor.waitForCompletion();
      if(stepStatus.getStatus() != IStepCompletionStatus.Status.COMPLETED)
      {
        if(log.isDebugEnabled())
          debug("detected " + stepStatus.getStatus() + " in step " + i);
        needToSkip = true;
      }
      status.add(stepStatus);

      i++;
    }

    return new CompositeStepCompletionStatus<T>(getCompositeStep(), status);
  }
}