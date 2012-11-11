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

package org.linkedin.glu.utils.concurrent;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;

/**
 * @author yan@pongasoft.com
 */
public class OneThreadPerTaskSubmitter implements Submitter
{
  public static final OneThreadPerTaskSubmitter DEFAULT = new OneThreadPerTaskSubmitter();

  private final ThreadFactory _threadFactory;

  /**
   * Constructor
   */
  public OneThreadPerTaskSubmitter()
  {
    this(null);
  }

  /**
   * Constructor
   */
  public OneThreadPerTaskSubmitter(ThreadFactory threadFactory)
  {
    _threadFactory = threadFactory;
  }

  @Override
  public Future<?> submit(Runnable task) throws RejectedExecutionException, NullPointerException
  {
    RunnableFuture future;

    if(task instanceof RunnableFuture)
    {
      future = (RunnableFuture) task;
    }
    else
    {
      future = new FutureTask<Object>(task, null);
    }

    newThread(future).start();

    return future;
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> Future<T> submit(Callable<T> task)
    throws RejectedExecutionException, NullPointerException
  {
    RunnableFuture<T> future;

    if(task instanceof RunnableFuture)
    {
      future = (RunnableFuture) task;
    }
    else
    {
      future = new FutureTask<T>(task);
    }

    newThread(future).start();

    return future;
  }

  @Override
  public <V, T extends RunnableFuture<V>> T submitFuture(T task)
    throws RejectedExecutionException, NullPointerException
  {
    newThread(task).start();
    return task;
  }

  @Override
  public <T> Future<T> submit(Runnable task, T result)
  {
    return submit(Executors.callable(task, result));
  }

  protected Thread newThread(RunnableFuture future)
  {
    if(_threadFactory == null)
      return new Thread(future);
    else
      return _threadFactory.newThread(future);
  }
}
