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

import java.util.Collection;
import java.util.EnumMap;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ypujante@linkedin.com
 */
public class CompositeStepCompletionStatus<T> implements IStepCompletionStatus<T>
{
  private final CompositeStep<T> _step;
  private final Collection<IStepCompletionStatus<T>> _statuses;
  private final long _startTime;
  private final long _endTime;
  private final Status _status;
  private final Throwable _throwable;

  public CompositeStepCompletionStatus(CompositeStep<T> step,
                                       Collection<IStepCompletionStatus<T>> statuses)
  {
    _step = step;
    _statuses = statuses;
    _startTime = computeStartTime(statuses);
    _endTime = computeEndTime(statuses);
    _status = computeStatus(statuses);
    _throwable = null;
  }

  public CompositeStepCompletionStatus(CompositeStep<T> step,
                                       long startTime,
                                       long endTime,
                                       Status status,
                                       Throwable throwable)
  {
    _step = step;
    _statuses = Collections.emptyList();
    _startTime = startTime;
    _endTime = endTime;
    _status = status;
    _throwable = throwable;
  }

  private static <T> Status computeStatus(Collection<IStepCompletionStatus<T>> statuses)
  {
    EnumMap<Status, AtomicInteger> map = new EnumMap<Status, AtomicInteger>(Status.class);

    for(Status status : Status.values())
    {
      map.put(status, new AtomicInteger());
    }

    for(IStepCompletionStatus<T> status : statuses)
    {
      map.get(status.getStatus()).incrementAndGet();
    }

    if(map.get(Status.FAILED).get() > 0)
      return Status.FAILED;

    if(map.get(Status.COMPLETED).get() == statuses.size())
      return Status.COMPLETED;

    if(map.get(Status.SKIPPED).get() == statuses.size())
      return Status.SKIPPED;

    return Status.PARTIAL;
  }

  private static <T> long computeStartTime(Collection<IStepCompletionStatus<T>> statuses)
  {
    long startTime = Long.MAX_VALUE;

    for(IStepCompletionStatus status : statuses)
    {
      startTime = Math.min(startTime, status.getStartTime());
    }

    return startTime;
  }

  private static <T> long computeEndTime(Collection<IStepCompletionStatus<T>> statuses)
  {
    long endTime = Long.MIN_VALUE;

    for(IStepCompletionStatus status : statuses)
    {
      endTime = Math.max(endTime, status.getEndTime());
    }

    return endTime;
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

  public Collection<IStepCompletionStatus<T>> getStatuses()
  {
    return _statuses;
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
    IStepCompletionStatusVisitor<T> childrenVisitor = null;

    switch(_step.getType())
    {
      case SEQUENTIAL:
        childrenVisitor = visitor.visitSequentialStepStatus(this);
        break;

      case PARALLEL:
        childrenVisitor = visitor.visitParallelStepStatus(this);
        break;

      default:
        throw new RuntimeException("should not be reached");
    }

    if(visitor != null)
    {
      childrenVisitor.startVisit();
      for(IStepCompletionStatus<T> childStatus : _statuses)
      {
        childStatus.acceptVisitor(childrenVisitor);
      }
      childrenVisitor.endVisit();
    }
  }
}