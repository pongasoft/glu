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

import java.util.Map;

/**
 * @author ypujante@linkedin.com
 */
public interface IStep<T>
{
  enum Type
  {
    LEAF,
    SEQUENTIAL,
    PARALLEL
  }

  /**
   * @return the name of the step (usually used for display purpose)
   */
  String getName();

  /**
   * @return the id of the step
   */
  String getId();

  /**
   * @return the metadata associated to this step */
  Map<String, Object> getMetadata();

  /**
   * Visitor pattern. Will be called back for the recursive structure.
   */
  void acceptVisitor(IStepVisitor<T> visitor);

  /**
   * @return the type of the step
   */
  Type getType();
}
