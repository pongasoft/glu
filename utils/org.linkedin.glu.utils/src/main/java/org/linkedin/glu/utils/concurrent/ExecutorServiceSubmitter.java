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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;

/**
 * Simple implementation which delegates the call to an actual executor service
 *
 * @author yan@pongasoft.com
 */
public class ExecutorServiceSubmitter implements Submitter
{
  private final ExecutorService _executorService;

  /**
   * Constructor
   */
  public ExecutorServiceSubmitter(ExecutorService executorService)
  {
    _executorService = executorService;
  }

  @Override
  public <T> Future<T> submit(Callable<T> tCallable)
  {
    return _executorService.submit(tCallable);
  }

  @Override
  public <T> Future<T> submit(Runnable runnable, T t)
  {
    return _executorService.submit(runnable, t);
  }

  @Override
  public Future<?> submit(Runnable runnable)
  {
    return _executorService.submit(runnable);
  }

  @Override
  public <V, T extends RunnableFuture<V>> T submitFuture(T task)
    throws RejectedExecutionException, NullPointerException
  {
    _executorService.submit(task);
    return task;
  }
}
