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

package org.linkedin.glu.agent.impl.script

import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.Callable
import java.util.concurrent.RunnableFuture
import org.linkedin.glu.agent.api.FutureExecution
import org.linkedin.util.clock.Timespan

/**
 * @author ypujante@linkedin.com */

abstract class FutureExecutionImpl implements RunnableFuture, FutureExecution, Comparable<FutureExecutionImpl>
{
  /**
   * Unique id of the execution */
  final String id = UUID.randomUUID().toString()

  /**
   * id in the queue */
  int queueId

  /**
   * The callback to call on cancel
   */
  def cancelCallback

  /**
   * when the execution should start (0 means start now) */
  synchronized long futureExecutionTime = 0L

  /**
   * when the execution started */
  synchronized long startTime = 0L

  /**
   * when the execution completes */
  synchronized long completionTime = 0L

  /**
   * Internally use a future task
   */
  private final FutureTask _futureTask

  FutureExecutionImpl()
  {
    _futureTask = new FutureTask({ execute() } as Callable)
  }

  abstract def execute()

  boolean cancel(boolean mayInterruptIfRunning)
  {
    def res = _futureTask.cancel(mayInterruptIfRunning)
    cancelCallback(this)
    return res
  }

  void run()
  {
    _futureTask.run()
  }

  boolean isCancelled()
  {
    return _futureTask.isCancelled();
  }

  boolean isDone()
  {
    return _futureTask.isDone();
  }

  Object get()
  {
    return _futureTask.get();
  }

  Object get(long l, TimeUnit timeUnit)
  {
    return _futureTask.get(l, timeUnit);
  }

  Object get(timeout) throws InterruptedException, ExecutionException, TimeoutException
  {
    timeout = Timespan.parse(timeout?.toString())
    if(!timeout)
      get()
    else
      get(timeout.durationInMilliseconds, TimeUnit.MILLISECONDS)
  }

  int compareTo(FutureExecutionImpl o)
  {
    int diff = futureExecutionTime.compareTo(o.futureExecutionTime)
    if(diff == 0)
    {
      diff = queueId - o.queueId
    }
    return diff
  }

  def String toString()
  {
    StringBuilder sb = new StringBuilder("class=${this.getClass().simpleName}")

    sb << ", id=${id}"
    sb << ", queueId=${queueId}"
    if(futureExecutionTime)
      sb << ", futureExecutionTime=${new Date(futureExecutionTime)}(${futureExecutionTime})"
    if(startTime)
      sb << ", startTime=${new Date(startTime)}"
    if(completionTime)
      sb << ", completionTime=${new Date(completionTime)}"

    return sb.toString();
  }


}