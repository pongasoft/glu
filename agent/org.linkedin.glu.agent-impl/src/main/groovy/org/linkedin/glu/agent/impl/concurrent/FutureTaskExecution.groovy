/*
 * Copyright (c) 2012 Yan Pujante
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

package org.linkedin.glu.agent.impl.concurrent

import org.linkedin.glu.agent.api.FutureExecution
import java.util.concurrent.TimeUnit
import java.util.concurrent.Callable
import org.linkedin.util.clock.Clock
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import org.linkedin.util.clock.Timespan

import org.linkedin.glu.groovy.utils.concurrent.GluGroovyConcurrentUtils
import java.util.concurrent.ExecutorService

import java.util.concurrent.FutureTask

/**
 * @author yan@pongasoft.com */
public class FutureTaskExecution<T> implements FutureExecution, Callable<T>
{
  /**
   * Unique id of the execution */
  private String _id

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
   * The clock
   */
  Clock clock

  /**
   * The actual future task on which all the calls will be delegated to implement the
   * <code>Future</code> interface
   */
  protected final FutureTask<T> _future = new FutureTask<T>(this)

  /**
   * The callable assigned in the constructor (if any)
   */
  protected final Callable<T> _callable

  /**
   * Callback to be called once the future ends
   */
  Closure onCompletionCallback

  /**
   * In this case, the {@link #execute} method will be called
   */
  FutureTaskExecution()
  {
    _callable = null
  }

  /**
   * execute the callable
   */
  FutureTaskExecution(Callable<T> callable)
  {
    _callable = callable
  }

  /**
   * execute the closure
   */
  FutureTaskExecution(Closure closure)
  {
    this(GluGroovyConcurrentUtils.asCallable(closure))
  }

  /**
   * Runs asynchronously. Uses the executor service to run asynchronously. Returns right away.
   * @return <code>this</code> for convenience
   */
  FutureTaskExecution<T> runAsync(ExecutorService executorService)
  {
    executorService.submit(_future)
    return this
  }

  @Override
  synchronized String getId()
  {
    if(_id == null)
      _id = "${Long.toHexString(clock.currentTimeMillis())}-${UUID.randomUUID().toString()}"
    return _id
  }

  synchronized void setId(String id)
  {
    _id = id
  }

  void run()
  {
    _future.run()
  }

  /**
   * Runs synchronously. (blocks until completed)
   */
  T runSync()
  {
    _future.run()
    try
    {
      _future.get()
    }
    catch(ExecutionException e)
    {
      throw e.cause
    }
  }

  @Override
  final T call()
  {
    try
    {
      startTime = clock.currentTimeMillis()

      try
      {
        def res

        if(_callable != null)
          res = _callable.call()
        else
          res = execute()

        return res
      }
      finally
      {
        completionTime = clock.currentTimeMillis()
      }
    }
    finally
    {
      if(onCompletionCallback)
        onCompletionCallback()
    }
  }

  protected T execute() throws Exception
  {
    return null
  }

  Object get(timeout) throws InterruptedException, ExecutionException, TimeoutException
  {
    timeout = Timespan.parse(timeout?.toString())
    if(!timeout?.durationInMilliseconds)
      get()
    else
      get(timeout.durationInMilliseconds, TimeUnit.MILLISECONDS)
  }

  @Override
  boolean cancel(boolean mayInterruptIfRunning)
  {
    return _future.cancel(mayInterruptIfRunning)
  }

  @Override
  boolean isCancelled()
  {
    return _future.isCancelled()
  }

  @Override
  boolean isDone()
  {
    return _future.isDone()
  }

  @Override
  T get()
  {
    return _future.get()
  }

  @Override
  T get(long timeout, TimeUnit unit)
  {
    return _future.get(timeout, unit)
  }
}