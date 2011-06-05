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
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptLifecycleInstallActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptLifecycleUninstallActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptTransitionActionDescriptor;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemEntryDelta;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemModelDelta;
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;
import org.linkedin.glu.provisioner.plan.api.LeafStep;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * @author yan@pongasoft.com
 */
public class SingleStepTransition extends SingleEntryTransition
{
  public static final String INSTALL_SCRIPT_ACTION = "installScript";
  public static final String UNINSTALL_SCRIPT_ACTION = "uninstallScript";

  private final String _action;
  private final String _to;

  /**
   * Constructor
   */
  public SingleStepTransition(String key,
                              String entryKey,
                              String action,
                              String to)
  {
    super(key, entryKey);
    _action = action;
    _to = to;
  }

  public static String computeTransitionKey(String entryKey, String action)
  {
    // 'to' serves no purpose in the key...
    StringBuilder sb = new StringBuilder();
    sb.append(entryKey).append('/').append(action);
    return sb.toString();
  }

  public String getAction()
  {
    return _action;
  }

  public String getTo()
  {
    return _to;
  }

  public MultiStepsSingleEntryTransition convertToMultiSteps(Map<String, Transition> transitions)
  {
    if(!isRoot())
      return null;

    Collection<Transition> linearTransitions = new ArrayList<Transition>();

    linearTransitions = collectLinearTransitions(getEntryKey(), transitions, linearTransitions);

    if(linearTransitions == null)
      return null;

    return new MultiStepsSingleEntryTransition(getKey(), getEntryKey(), linearTransitions);
  }

  @Override
  protected Collection<Transition> collectLinearTransitions(String entryKey,
                                                            Map<String, Transition> transitions,
                                                            Collection<Transition> linearTransitions)
  {
    if(!getEntryKey().equals(entryKey))
      return null;

    if(getExecuteBefore().size() == 0)
    {
      linearTransitions.add(this);
      return linearTransitions;
    }
    else
    {
      Transition transition = transitions.get(findSingleExecuteBefore());
      if(transition != null && getKey().equals(transition.findSingleExecuteAfter()))
      {
        linearTransitions.add(this);
        return transition.collectLinearTransitions(entryKey, transitions, linearTransitions);
      }
    }

    return null;
  }

  @Override
  public void addSteps(ICompositeStepBuilder<ActionDescriptor> builder,
                       InternalSystemModelDelta systemModelDelta)
  {
    InternalSystemEntryDelta entryDelta =
      systemModelDelta.findAnyEntryDelta(getEntryKey());

    if(entryDelta == null)
      return;

    ActionDescriptor actionDescriptor;

    if(INSTALL_SCRIPT_ACTION.equals(getAction()))
    {
      actionDescriptor =
        new ScriptLifecycleInstallActionDescriptor("TODO script lifecycle: installScript",
                                                   systemModelDelta.getFabric(),
                                                   entryDelta.getAgent(),
                                                   entryDelta.getMountPoint(),
                                                   entryDelta.getExpectedEntry().getParent(),
                                                   entryDelta.getExpectedEntry().getScript(),
                                                   (Map) entryDelta.getExpectedEntry().getInitParameters());

    }
    else
    {
      if(UNINSTALL_SCRIPT_ACTION.equals(getAction()))
      {
        actionDescriptor =
          new ScriptLifecycleUninstallActionDescriptor("TODO script lifecycle: uninstallScript",
                                                       systemModelDelta.getFabric(),
                                                       entryDelta.getAgent(),
                                                       entryDelta.getMountPoint());

      }
      else
      {
        // TODO HIGH YP:  handle actionArgs
        Map actionArgs = null;

        actionDescriptor =
          new ScriptTransitionActionDescriptor("TODO script action: " + getAction(),
                                               systemModelDelta.getFabric(),
                                               entryDelta.getAgent(),
                                               entryDelta.getMountPoint(),
                                               getAction(),
                                               getTo(),
                                               actionArgs);

      }
    }

    builder.addLeafStep(buildStep(actionDescriptor));
  }

  /**
   * Builds a leaf step
   */
  protected LeafStep<ActionDescriptor> buildStep(ActionDescriptor actionDescriptor)
  {
    return new LeafStep<ActionDescriptor>(null,
                                          actionDescriptor.toMetadata(),
                                          actionDescriptor);
  }
}
