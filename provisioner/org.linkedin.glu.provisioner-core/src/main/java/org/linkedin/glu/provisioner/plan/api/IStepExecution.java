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

import org.linkedin.util.clock.Timespan;

import java.util.concurrent.TimeoutException;

/**
 * @author ypujante@linkedin.com
 */
public interface IStepExecution<T>
{
  /**
   * @return the step being executed
   */
  IStep<T> getStep();

  /**
   * @return the time the step started
   */
  long getStartTime();

  /**
   * @return how long the execution took (or has been taking so far if not completed) */
  Timespan getDuration();

  /**
   * Pauses the execution of the plan. Note that this method returns right away and all running
   * steps will finish first. All pending steps won't be started until {@link #resume()} is called.
   */
  void pause();

  /**
   * Resumes the execution of the plan
   */
  void resume();

  /**
   * @return <code>true</code> if the execution is currently paused
   */
  boolean isPaused();

  /**
   * Attempts to cancel execution of this step.
   *
   * @param mayInterruptIfRunning true if the thread executing this step should be interrupted;
   *                              otherwise, in-progress steps are allowed to complete
   */
  void cancel(boolean mayInterruptIfRunning);

  /**
   * @return <code>true</code> if interrupted
   */
  boolean isCancelled();

  /**
   * @return <code>true</code> if the execution is completed (or aborted).
   */
  boolean isCompleted();

  /**
   * @return the completion status (or <code>null</code> if not completed)
   */
  IStepCompletionStatus<T> getCompletionStatus();

  /**
   * Wait for the execution to be completed.
   *
   * @return the status
   * @throws InterruptedException if interrupted while waiting
   */
  IStepCompletionStatus<T> waitForCompletion() throws InterruptedException;

  /**
   * Wait for the execution to be completed.
   *
   * @return the status
   * @throws InterruptedException if interrupted while waiting
   * @throws TimeoutException if the timeout gets reached and the execution is not yet completed
   */
  IStepCompletionStatus<T> waitForCompletion(Timespan timeout)
    throws InterruptedException, TimeoutException;
}