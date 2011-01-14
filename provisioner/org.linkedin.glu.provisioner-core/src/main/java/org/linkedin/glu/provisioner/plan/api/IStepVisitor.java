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

/**
 * Visitor pattern for visiting the steps.
 *
 * @author ypujante@linkedin.com
 */
public interface IStepVisitor<T>
{
  /**
   * Called on visit start
   */
  void startVisit();

  /**
   * Visits a leaf step.
   */
  void visitLeafStep(LeafStep<T> leafStep);

  /**
   * Visit a sequential step.
   *
   * @return <code>null</code> if you want to stop the recursion, otherwise another visitor
   */
  IStepVisitor<T> visitSequentialStep(SequentialStep<T> sequentialStep);

  /**
   * Visit a parallel step.
   *
   * @return <code>null</code> if you want to stop the recursion, otherwise another visitor
   */
  IStepVisitor<T> visitParallelStep(ParallelStep<T> parallelStep);

  /**
   * Called on visit end
   */
  void endVisit();
}