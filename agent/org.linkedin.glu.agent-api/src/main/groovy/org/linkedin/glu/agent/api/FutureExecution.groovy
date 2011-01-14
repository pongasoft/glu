/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.agent.api

import java.util.concurrent.Future
import java.util.concurrent.TimeoutException
import java.util.concurrent.ExecutionException

/**
 * @author ypujante@linkedin.com */
interface FutureExecution<K> extends Future<K>
{
  /**
   * Unique id of the execution */
  String getId()

  /**
   * when the execution should start (0 means start now) */
  long getFutureExecutionTime()

  /**
   * when the execution started */
  long getStartTime()

  /**
   * when the execution completes */
  long getCompletionTime()

  /**
   * Convenient call which allow a timeout of different types (long, String, Timespan...)
   */
  K get(timeout) throws InterruptedException, ExecutionException, TimeoutException
}
