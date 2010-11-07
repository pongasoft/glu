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

package org.linkedin.glu.provisioner.deployment.impl

import org.linkedin.glu.provisioner.core.action.IActionFactory
import org.linkedin.glu.provisioner.plan.api.ILeafStepExecutor
import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.plan.api.LeafStep

/**
 * @author ypujante@linkedin.com */
public class ActionDescriptorStepExecutor implements ILeafStepExecutor<ActionDescriptor>
{
  IActionFactory actionFactory

  public Object executeLeafStep(LeafStep<ActionDescriptor> leafStep)
  {
    def action = actionFactory.createAction(leafStep.action)
    action.execute()
  }
}