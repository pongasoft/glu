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
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;

/**
 * The purpose of this class is to encapsulates the "useful" methods of the
 * <code>ExecutorService</code> api... From a client point of view, there is no need to know
 * about <code>shutdown</code> etc...
 *
 * @author yan@pongasoft.com
 */
public interface Submitter
{
  public static final Submitter DEFAULT = OneThreadPerTaskSubmitter.DEFAULT;

  /**
   * @see java.util.concurrent.ExecutorService#submit(Runnable)
   */
  Future<?> submit(Runnable task) throws RejectedExecutionException, NullPointerException;

  /**
   * @see java.util.concurrent.ExecutorService#submit(Callable)
   */
  <T> Future<T> submit(Callable<T> task) throws RejectedExecutionException, NullPointerException;

  /**
   * @see java.util.concurrent.ExecutorService#submit(Runnable, Object)
   */
  public <T> Future<T> submit(Runnable task, T result);

  /**
   * Similar call, but will return the same value provided */
  <V, T extends RunnableFuture<V>> T submitFuture(T task)
    throws RejectedExecutionException, NullPointerException;
}