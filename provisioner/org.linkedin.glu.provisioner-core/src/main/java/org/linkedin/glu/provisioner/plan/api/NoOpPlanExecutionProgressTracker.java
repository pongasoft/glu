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

package org.linkedin.glu.provisioner.plan.api;

/**
 * @author ypujante@linkedin.com
 */
public class NoOpPlanExecutionProgressTracker<T> implements IPlanExecutionProgressTracker<T>
{
  public static final NoOpPlanExecutionProgressTracker INSTANCE =
    new NoOpPlanExecutionProgressTracker();

  @SuppressWarnings("unchecked")
  public static <T> NoOpPlanExecutionProgressTracker<T> instance()
  {
    return INSTANCE;
  }

  /**
   * Constructor
   */
  public NoOpPlanExecutionProgressTracker()
  {
  }

  /**
   * Called when the execution of the step starts
   *
   * @param stepExecution step which starts
   */
  @Override
  public void onStepStart(IStepExecution<T> stepExecution)
  {
  }
  
  /**
   * Called when the execution of the step ends
   *
   * @param stepCompletionStatus status of the execution
   */
  @Override
  public void onStepEnd(IStepCompletionStatus<T> stepCompletionStatus)
  {
  }

  /**
   * Called at the beginning of the execution
   * @param planExecution
   */
  @Override
  public void onPlanStart(IPlanExecution<T> planExecution)
  {
  }

  /**
   * Called at the end of the execution
   */
  @Override
  public void onPlanEnd(IStepCompletionStatus<T> tiStepCompletionStatus)
  {
  }

  /**
   * Called when execution is aborted
   */
  @Override
  public void onCancelled(IStep<T> tiStep)
  {
  }

  /**
   * Called when execution is paused
   */
  @Override
  public void onPause(IStep<T> tiStep)
  {
  }

  /**
   * Called when execution is resumed
   */
  @Override
  public void onResume(IStep<T> tiStep)
  {
  }
}
