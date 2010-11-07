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

package org.linkedin.glu.provisioner.core.plan.impl

import org.linkedin.glu.provisioner.plan.api.IStepBuilder
import org.linkedin.glu.provisioner.plan.api.LeafStep
import org.linkedin.glu.provisioner.plan.api.SequentialStepBuilder
import org.linkedin.glu.provisioner.plan.api.ParallelStepBuilder

/**
 * @author ypujante@linkedin.com  */
public class StepBuilder extends BuilderSupport
{
  protected void setParent(Object parent, Object child)
  {
  }

  protected Object createNode(Object name)
  {
    return new Node(getCurrentNode(), name, new ArrayList());
  }

  protected Object createNode(Object name, Object value)
  {
    return new Node(getCurrentNode(), name, value);
  }

  protected Object createNode(Object name, Map attributes)
  {
    def stepBuilder = getCurrentStepBuilder()

    switch(name)
    {
      case 'sequential':
        if(stepBuilder != null)
        {
          stepBuilder = stepBuilder.addSequentialSteps()
        }
        else
        {
          stepBuilder = new SequentialStepBuilder()
        }
        stepBuilder.id = attributes.remove('id')
        stepBuilder.metadata = attributes
        return stepBuilder

      case 'parallel':
      if(stepBuilder != null)
      {
        stepBuilder = stepBuilder.addParallelSteps()
      }
      else
      {
        stepBuilder = new ParallelStepBuilder()
      }
      stepBuilder.id = attributes.remove('id')
      stepBuilder.metadata = attributes
      return stepBuilder

      case 'leaf':
        def action = attributes.remove('action')
        String id = attributes.remove('id')
        stepBuilder.addLeafStep(new LeafStep(id, attributes, action))
        return null
      
      default:
       throw new IllegalArgumentException("only leaf/sequential/parallel allowed")
    }
  }

  protected Object createNode(Object name, Map attributes, Object value)
  {
    return new Node(getCurrentNode(), name, attributes, value);
  }

  protected IStepBuilder getCurrentStepBuilder()
  {
    return (IStepBuilder) getCurrent();
  }
}