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
 * Decorator pattern.
 *
 * @author ypujante@linkedin.com
 */
public class FilteredPlanExecutionProgressTracker<T> implements IPlanExecutionProgressTracker<T>
{
  private final IPlanExecutionProgressTracker<T> _tracker;

  /**
   * Constructor
   */
  public FilteredPlanExecutionProgressTracker(IPlanExecutionProgressTracker<T> tracker)
  {
    _tracker = tracker;
  }

  @Override
  public void onCancelled(IStep<T> tiStep)
  {
    if(_tracker != null)
      _tracker.onCancelled(tiStep);
  }

  @Override
  public void onPause(IStep<T> tiStep)
  {
    if(_tracker != null)
      _tracker.onPause(tiStep);
  }

  @Override
  public void onPlanEnd(IStepCompletionStatus<T> tiStepCompletionStatus)
  {
    if(_tracker != null)
      _tracker.onPlanEnd(tiStepCompletionStatus);
  }

  @Override
  public void onPlanStart(IPlanExecution<T> planExecution)
  {
    if(_tracker != null)
      _tracker.onPlanStart(planExecution);
  }

  @Override
  public void onResume(IStep<T> tiStep)
  {
    if(_tracker != null)
      _tracker.onResume(tiStep);
  }

  @Override
  public void onStepEnd(IStepCompletionStatus<T> tiStepCompletionStatus)
  {
    if(_tracker != null)
      _tracker.onStepEnd(tiStepCompletionStatus);
  }

  @Override
  public void onStepStart(IStepExecution<T> stepExecution)
  {
    if(_tracker != null)
      _tracker.onStepStart(stepExecution);
  }
}
