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

package org.linkedin.glu.provisioner.plan.api;

import org.linkedin.util.clock.Timespan;

/**
 * @author ypujante@linkedin.com
 */
public interface IStepCompletionStatus<T>
{
  public enum Status
  {
    COMPLETED,
    PARTIAL, // case when some steps completed and some were skipped
    CANCELLED,
    FAILED,
    SKIPPED
  }

  IStep<T> getStep();

  long getStartTime();

  long getEndTime();

  Timespan getDuration();

  Status getStatus();

  Throwable getThrowable();

  /**
   * Visitor pattern. Will be called back for the recursive structure.
   */
  void acceptVisitor(IStepCompletionStatusVisitor<T> visitor);
}