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
 * @author ypujante@linkedin.com
 */
public abstract class CompositeStep<T> extends AbstractStep<T> implements IStep<T>
{
  private final Collection<IStep<T>> _steps;

  /**
   * Constructor
   */
  public CompositeStep(String id, Map<String, Object> metadata, Collection<IStep<T>> steps)
  {
    super(id, metadata);
    _steps = steps;
  }

  public Collection<IStep<T>> getSteps()
  {
    return _steps;
  }

  /**
   * Visits the composite steps
   */
  protected void visitSteps(IStepVisitor<T> visitor)
  {
    if(visitor != null)
    {
      visitor.startVisit();
      try
      {
        for(IStep<T> step : _steps)
        {
          step.acceptVisitor(visitor);
        }
      }
      finally
      {
        visitor.endVisit();
      }
    }
  }
}
