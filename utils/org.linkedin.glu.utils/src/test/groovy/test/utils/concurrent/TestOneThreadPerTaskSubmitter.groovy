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

package test.utils.concurrent

import org.linkedin.glu.utils.concurrent.OneThreadPerTaskSubmitter
import org.linkedin.util.concurrent.ThreadControl
import org.linkedin.util.clock.Timespan
import org.linkedin.glu.groovy.utils.concurrent.FutureTaskExecution
import java.util.concurrent.TimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.Callable

/**
 * @author yan@pongasoft.com */
public class TestOneThreadPerTaskSubmitter extends GroovyTestCase
{
  OneThreadPerTaskSubmitter submitter = new OneThreadPerTaskSubmitter()

  public void testSubmitFuture()
  {
    ThreadControl tc = new ThreadControl(Timespan.parse("30s"))

    def activeCount = Thread.activeCount()

    def async = {
      tc.blockWithException("async")
    }

    def future = new FutureTaskExecution(async)

    assertTrue(future.is(submitter.submitFuture(future)))

    shouldFail(TimeoutException) { future.get(Timespan.parse('500')) }

    tc.waitForBlock("async")

    assertEquals(activeCount + 1, Thread.activeCount())

    tc.unblock("async", 3)

    assertEquals(3, future.get())

    assertEquals(activeCount, Thread.activeCount())
  }

  public void testSubmitRunnable()
  {
    ThreadControl tc = new ThreadControl(Timespan.parse("30s"))

    def activeCount = Thread.activeCount()

    def async = {
      tc.blockWithException("async")
    }

    // first we check with a FutureTaskExecution (which is a Runnable)
    def future = new FutureTaskExecution(async)

    assertTrue(future.is(submitter.submit(future)))

    shouldFail(TimeoutException) { future.get(Timespan.parse('500')) }

    tc.waitForBlock("async")

    assertEquals(activeCount + 1, Thread.activeCount())

    tc.unblock("async", 3)

    assertEquals(3, future.get())

    assertEquals(activeCount, Thread.activeCount())

    // second we check with a runnable wrapper
    future = new FutureTaskExecution(async)

    def runnable = new Runnable() {
      public void run()
      {
        future.run()
      }
    }

    def newFuture = submitter.submit(runnable)

    assertFalse(future.is(newFuture))

    shouldFail(TimeoutException) { future.get(Timespan.parse('500')) }
    shouldFail(TimeoutException) { newFuture.get(Timespan.parse('500').durationInMilliseconds, TimeUnit.MILLISECONDS) }

    tc.waitForBlock("async")

    assertEquals(activeCount + 1, Thread.activeCount())

    tc.unblock("async", 3)

    assertEquals(3, future.get())
    assertNull(newFuture.get())

    assertEquals(activeCount, Thread.activeCount())

  }

  public void testSubmitCallable()
  {
    ThreadControl tc = new ThreadControl(Timespan.parse("30s"))

    def activeCount = Thread.activeCount()

    def async = {
      tc.blockWithException("async")
    }

    def future = new FutureTaskExecution(async)

    def callable = new Callable() {
      @Override
      Object call()
      {
        future.runSync()
      }
    }

    def newFuture = submitter.submit(callable)

    assertFalse(future.is(newFuture))

    shouldFail(TimeoutException) { future.get(Timespan.parse('500')) }
    shouldFail(TimeoutException) { newFuture.get(Timespan.parse('500').durationInMilliseconds, TimeUnit.MILLISECONDS) }

    tc.waitForBlock("async")

    assertEquals(activeCount + 1, Thread.activeCount())

    tc.unblock("async", 3)

    assertEquals(3, future.get())
    assertEquals(3, newFuture.get())

    assertEquals(activeCount, Thread.activeCount())

  }
}