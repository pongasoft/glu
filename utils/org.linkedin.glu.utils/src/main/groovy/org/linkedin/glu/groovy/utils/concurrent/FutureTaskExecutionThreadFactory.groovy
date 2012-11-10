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

import java.util.concurrent.ThreadFactory
import org.linkedin.util.annotations.Initializable

/**
 * @author yan@pongasoft.com */
public class FutureTaskExecutionThreadFactory implements ThreadFactory
{
  @Initializable
  boolean createDaemonThreads = false

  @Override
  Thread newThread(Runnable runnable)
  {
    Thread res

    if(runnable instanceof FutureTaskExecution)
    {
      FutureTaskExecution fte = runnable as FutureTaskExecution
      res = new Thread(runnable, "[${fte.id}]${fte.description ? ' => ' + fte.description : ''}")
    }
    else
      res = new Thread(runnable)
    res.daemon = createDaemonThreads

    return res
  }
}