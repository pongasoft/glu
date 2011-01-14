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

import java.util.Collection;
import java.util.Map;

/**
 * Represent a collection of steps that needs to be executed sequentially (one after the other).
 * 
 * @author ypujante@linkedin.com
 */
public class SequentialStep<T> extends CompositeStep<T>
{
  /**
   * Constructor
   */
  public SequentialStep(String id, Map<String, Object> metadata, Collection<IStep<T>> iSteps)
  {
    super(id, metadata, iSteps);
  }

  /**
   * Visitor pattern. Will be called back for the recursive structure.
   */
  @Override
  public void acceptVisitor(IStepVisitor<T> visitor)
  {
    visitSteps(visitor.visitSequentialStep(this));
  }

  /**
   * @return the type of the step
   */
  @Override
  public Type getType()
  {
    return Type.SEQUENTIAL;
  }
}
