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

package org.linkedin.glu.provisioner.plan.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.linkedin.glu.provisioner.plan.api.IPlanExecutionProgressTracker;
import org.linkedin.util.clock.ClockUtils;
import org.linkedin.util.clock.Timespan;
import org.linkedin.util.lifecycle.Shutdownable;
import org.linkedin.util.reflect.ObjectProxyInvocationHandler;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The purpose of this proxy is to execute all calls to the progress tracker asynchronously in a
 * separate thread so as not to block the main thread. Also all exceptions are caught and properly
 * handled.
 * 
 * @author ypujante@linkedin.com
 */
public class AsyncPlanExecutionProgressTrackerProxy<T> extends ObjectProxyInvocationHandler<IPlanExecutionProgressTracker<T>>
  implements Shutdownable
{
  public static final String MODULE = AsyncPlanExecutionProgressTrackerProxy.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  private final ExecutorService _executorService;

  public AsyncPlanExecutionProgressTrackerProxy(IPlanExecutionProgressTracker<T> tracker)
  {
    super(tracker);
    _executorService = Executors.newSingleThreadExecutor();
  }

  @Override
  public synchronized Object invoke(final Object o, final Method method, final Object[] objects) throws Throwable
  {
    _executorService.submit(new Runnable()
    {
      @Override
      public void run()
      {
        doInvoke(o, method, objects);
      }
    });

    return null;
  }

  private void doInvoke(Object o, Method method, Object[] objects)
  {
    try
    {
      super.invoke(o, method, objects);
    }
    catch(Throwable throwable)
    {
      log.warn("Exception in progress tracker (ignored): " + throwable.getMessage());
      if(log.isDebugEnabled())
      {
        log.debug("Exception in progress tracker (ignored)", throwable);
      }
    }
  }

  @Override
  public void shutdown()
  {
    _executorService.shutdown();
  }

  @Override
  public void waitForShutdown() throws InterruptedException, IllegalStateException
  {
    if(!_executorService.isShutdown())
      throw new IllegalStateException("call shutdown first");

    _executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
  }

  @Override
  public void waitForShutdown(Object timeout)
    throws InterruptedException, IllegalStateException, TimeoutException
  {
    if(!_executorService.isShutdown())
      throw new IllegalStateException("call shutdown first");

    Timespan timespan = ClockUtils.toTimespan(timeout);
    if(timespan == null)
      _executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    else
    if(!_executorService.awaitTermination(timespan.getDurationInMilliseconds(), TimeUnit.MILLISECONDS))
      throw new TimeoutException();
  }
}
