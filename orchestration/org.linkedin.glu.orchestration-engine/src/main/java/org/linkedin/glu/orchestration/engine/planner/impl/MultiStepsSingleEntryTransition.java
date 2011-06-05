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

package org.linkedin.glu.orchestration.engine.planner.impl;

import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemEntryDelta;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemModelDelta;
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;

import java.util.Collection;

/**
 * @author yan@pongasoft.com
 */
public class MultiStepsSingleEntryTransition extends SingleEntryTransition
{
  private final Collection<Transition> _transitions;

  /**
   * Constructor
   */
  public MultiStepsSingleEntryTransition(String key,
                                         String entryKey,
                                         Collection<Transition> transitions)
  {
    super(key, entryKey);
    _transitions = transitions;
  }

  public Collection<Transition> getTransitions()
  {
    return _transitions;
  }

  public void addSteps(ICompositeStepBuilder<ActionDescriptor> builder,
                       InternalSystemModelDelta systemModelDelta)
  {
    builder = builder.addSequentialSteps();

    InternalSystemEntryDelta entryDelta = systemModelDelta.findAnyEntryDelta(getEntryKey());
    builder.setMetadata("agent", entryDelta.getAgent());
    builder.setMetadata("mountPoint", entryDelta.getMountPoint());

    for(Transition transition : _transitions)
    {
      transition.addSteps(builder, systemModelDelta);
    }
  }
}
