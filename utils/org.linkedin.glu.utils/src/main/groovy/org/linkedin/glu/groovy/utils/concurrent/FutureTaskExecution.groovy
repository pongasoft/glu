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

package org.linkedin.glu.groovy.utils.concurrent

import java.util.concurrent.TimeUnit
import java.util.concurrent.Callable
import org.linkedin.util.clock.Clock
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import org.linkedin.util.clock.Timespan

import java.util.concurrent.FutureTask
import org.linkedin.glu.groovy.utils.GluGroovyLangUtils
import org.linkedin.util.clock.SystemClock
import java.util.concurrent.RunnableFuture
import org.linkedin.glu.utils.concurrent.Submitter
import org.linkedin.glu.utils.concurrent.OneThreadPerTaskSubmitter
import org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils
import org.linkedin.util.lang.LangUtils

/**
 * @author yan@pongasoft.com */
public class FutureTaskExecution<T> implements FutureExecution<T>, RunnableFuture<T>
{
  public static final Submitter DEFAULT_SUBMITTER =
    new OneThreadPerTaskSubmitter(new FutureTaskExecutionThreadFactory())

  /**
   * Unique id of the execution */
  private String _id

  /**
   * An (optional) description for the task
   */
  String description

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
  Clock clock = SystemClock.instance()

  /**
   * The actual future task on which all the calls will be delegated to implement the
   * <code>Future</code> interface
   */
  protected final FutureTask<T> _future

  /**
   * The callable assigned in the constructor (if any)
   */
  protected final Callable<T> _callable

  /**
   * Callback to be called once the future ends
   */
  protected volatile Closure _onCompletionCallback

  /**
   * set when the completion callback has been called
   */
  protected boolean _onCompletionCallbackCalled = false

  /**
   * In this case, the {@link #execute} method will be called
   */
  FutureTaskExecution()
  {
    this((Callable) null)
  }

  /**
   * execute the callable
   */
  FutureTaskExecution(Callable<T> callable)
  {
    _callable = callable
    _future = new FutureTask<T>(GluGroovyConcurrentUtils.asCallable(doCall))
  }

  /**
   * execute the closure
   */
  FutureTaskExecution(Closure closure)
  {
    this(GluGroovyConcurrentUtils.asCallable(closure))
  }

  /**
   * Runs asynchronously. Uses the submitter to run asynchronously. Returns right away.
   * @return <code>this</code> for convenience
   */
  FutureTaskExecution<T> runAsync(Submitter submitter = DEFAULT_SUBMITTER)
  {
    submitter.submitFuture(this)
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

  /**
   * This method ensures that the completion callback is called even if the command has already run
   * and is completed!
   */
  void setOnCompletionCallback(Closure onCompletionCallback)
  {
    def callback = null

    synchronized(this)
    {
      if(_onCompletionCallback != null)
        throw new IllegalStateException("already set")
      _onCompletionCallback = onCompletionCallback
      if(completionTime > 0)
      {
        callback = _onCompletionCallback
        _onCompletionCallbackCalled = true
      }
    }

    // we make sure to call the callback *outside* the synchronized section!
    if(callback)
      callback()
  }

  Closure getOnCompletionCallback()
  {
    return _onCompletionCallback
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

  void waitForStart(def timeout = null)
  {
    GroovyConcurrentUtils.awaitFor(clock, timeout, this) {
      isStarted()
    }
  }

  boolean isStarted()
  {
    return startTime > 0
  }

  private def doCall = {

    synchronized(this)
    {
      startTime = clock.currentTimeMillis()
      this.notifyAll()
    }

    try
    {
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
      def callback = null

      synchronized(this)
      {
        if(!_onCompletionCallbackCalled)
        {
          callback = _onCompletionCallback
          _onCompletionCallbackCalled = true
        }
      }

      // we make sure to call the callback *outside* the synchronized section!
      if(callback)
        GluGroovyLangUtils.noException(callback)
    }
  }

  protected T execute() throws Exception
  {
    return null
  }

  T get(timeout) throws InterruptedException, ExecutionException, TimeoutException
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