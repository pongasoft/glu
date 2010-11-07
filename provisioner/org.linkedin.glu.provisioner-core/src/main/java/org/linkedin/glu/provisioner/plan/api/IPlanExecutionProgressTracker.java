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

/**
 * This interface will be called by multiple threads and as such implementation should be thread 
 * safe!
 * 
 * @author ypujante@linkedin.com
 */
public interface IPlanExecutionProgressTracker<T>
{
  /**
   * Called at the beginning of the execution
   */
  void onPlanStart(IPlanExecution<T> planExecution);

  /**
   * Called at the end of the execution
   */
  void onPlanEnd(IStepCompletionStatus<T> stepCompletionStatus);

  /**
   * Called when execution is paused
   */
  void onPause(IStep<T> step);

  /**
   * Called when execution is resumed
   */
  void onResume(IStep<T> step);

  /**
   * Called when execution is cancelled
   */
  void onCancelled(IStep<T> step);

  /**
   * Called when the execution of the step starts
   *
   * @param stepExecution step which starts
   */
  void onStepStart(IStepExecution<T> stepExecution);

  /**
   * Called when the execution of the step ends
   *
   * @param stepCompletionStatus status of the execution
   */
  void onStepEnd(IStepCompletionStatus<T> stepCompletionStatus);
}
