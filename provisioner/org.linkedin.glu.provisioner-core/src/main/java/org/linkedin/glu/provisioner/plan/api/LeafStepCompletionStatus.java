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

/**
 * @author ypujante@linkedin.com
 */
public class LeafStepCompletionStatus<T> implements IStepCompletionStatus<T>
{
  private final LeafStep<T> _step;
  
  private final long _startTime;
  private final long _endTime;
  private final Status _status;
  private final Throwable _throwable;

  public LeafStepCompletionStatus(LeafStep<T> step,
                                 long startTime,
                                 long endTime,
                                 Status status,
                                 Throwable throwable)
  {
    if(endTime < startTime)
      throw new IllegalArgumentException("endTime < startTime");

    if(throwable != null && status != Status.FAILED)
      throw new IllegalArgumentException("status should be FAILED");

    if(status == Status.PARTIAL)
      throw new IllegalArgumentException("status cannot be PARTIAL for a leaf step");

    _step = step;
    _startTime = startTime;
    _endTime = endTime;
    _status = status;
    _throwable = throwable;
  }

  @Override
  public IStep<T> getStep()
  {
    return _step;
  }

  @Override
  public long getStartTime()
  {
    return _startTime;
  }

  @Override
  public long getEndTime()
  {
    return _endTime;
  }

  @Override
  public Timespan getDuration()
  {
    return new Timespan(_endTime - _startTime);
  }

  @Override
  public Status getStatus()
  {
    return _status;
  }

  @Override
  public Throwable getThrowable()
  {
    return _throwable;
  }

  /**
   * Visitor pattern. Will be called back for the recursive structure.
   */
  @Override
  public void acceptVisitor(IStepCompletionStatusVisitor<T> visitor)
  {
    visitor.visitLeafStepStatus(this);
  }
}