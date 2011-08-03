/*
 * Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.orchestration.engine.action.execution;

import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor;
import org.linkedin.glu.provisioner.plan.api.ILeafStepExecutor;
import org.linkedin.glu.provisioner.plan.api.LeafStep;
import org.linkedin.util.annotations.Initializer;

/**
 * @author yan@pongasoft.com
 */
public class ActionDescriptorStepExecutor implements ILeafStepExecutor<ActionDescriptor>
{
  private ActionExecutionFactory _actionExecutionFactory;

  /**
   * Constructor
   */
  public ActionDescriptorStepExecutor()
  {
  }

  public ActionExecutionFactory getActionExecutionFactory()
  {
    return _actionExecutionFactory;
  }

  @Initializer(required = true)
  public void setActionExecutionFactory(ActionExecutionFactory actionExecutionFactory)
  {
    _actionExecutionFactory = actionExecutionFactory;
  }

  @Override
  public Object executeLeafStep(LeafStep<ActionDescriptor> leafStep)
    throws Exception
  {
    ActionExecution<Object> action = _actionExecutionFactory.createAction(leafStep.getAction());
    return action.call();
  }
}
