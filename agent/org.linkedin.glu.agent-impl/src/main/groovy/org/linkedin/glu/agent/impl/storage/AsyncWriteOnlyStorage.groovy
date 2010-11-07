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

package org.linkedin.glu.agent.impl.storage

import org.linkedin.glu.agent.api.MountPoint
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import org.linkedin.util.lifecycle.Shutdownable
import org.linkedin.util.clock.ClockUtils
import org.linkedin.util.clock.Timespan

/**
 * Asynchronous implementation of write only storage
 *
 * @author ypujante@linkedin.com */
class AsyncWriteOnlyStorage implements WriteOnlyStorage, Shutdownable
{
  public static final String MODULE = AsyncWriteOnlyStorage.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  private final WriteOnlyStorage _wos
  private final ExecutorService _executorService

  AsyncWriteOnlyStorage(WriteOnlyStorage wos, ExecutorService executorService)
  {
    _wos = wos
    _executorService = executorService;
  }

  public void clearState(MountPoint mountPoint)
  {
    execute("clearState") {
      _wos.clearState(mountPoint)
    }
  }

  public void storeState(MountPoint mountPoint, Object state)
  {
    execute("storeState") {
      _wos.storeState(mountPoint, value)
    }
  }

  public void clearAllStates()
  {
    execute("clearAllStates") {
      _wos.clearAllStates()
    }
  }

  private def execute(String name, Closure closure)
  {
    def runnable = {
      try
      {
        closure()
      }
      catch(Throwable th)
      {
        log.warn("${name}: ignored exception", e)
      }
    }

    _executorService.submit(runnable as Runnable)
  }

  public void shutdown()
  {
    _executorService.shutdown()
  }

  public void waitForShutdown()
  {
    waitForShutdown(Long.MAX_VALUE)
  }

  void waitForShutdown(Object timeout)
  {
    timeout = ClockUtils.toTimespan(timeout) ?: new Timespan(Long.MAX_VALUE)
    _executorService.awaitTermination(timeout.durationInMilliseconds, TimeUnit.MILLISECONDS)
  }


}
