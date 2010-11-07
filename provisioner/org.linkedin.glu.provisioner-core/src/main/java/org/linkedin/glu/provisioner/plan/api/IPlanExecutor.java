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

package org.linkedin.glu.provisioner.plan.api;

/**
 * @author ypujante@linkedin.com
 */
public interface IPlanExecutor<T>
{
  /**
   * Execute the provided plan. Note that this call is non blocking and will return an execution
   * object with which to interract.
   *
   * @param plan the plane to execute
   * @param progressTracker to track the progress of the execution
   * @return the execution
   */
  IPlanExecution<T> executePlan(Plan<T> plan, IPlanExecutionProgressTracker<T> progressTracker);

  /**
   * Execute the provided plan. Note that this call is non blocking and will return an execution
   * object with which to interract.
   *
   * @param plan the plane to execute
   * @return the execution
   */
  IPlanExecution<T> executePlan(Plan<T> plan);
}