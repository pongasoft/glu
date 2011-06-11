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
import org.linkedin.glu.orchestration.engine.action.descriptor.InternalActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.NoOpActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptLifecycleInstallActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptLifecycleUninstallActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptTransitionActionDescriptor;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemEntryDelta;
import org.linkedin.glu.provisioner.core.model.SystemEntry;
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;

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
  private final String _toState;

  /**
   * Constructor
   */
  public SingleStepTransition(TransitionPlan transitionPlan,
                              String key,
                              String entryKey,
                              String action,
                              String toState)
  {
    super(transitionPlan, key, entryKey);
    _action = action;
    _toState = toState;
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

  public String getToState()
  {
    return _toState;
  }

  public MultiStepsSingleEntryTransition convertToMultiSteps()
  {
    if(!isRoot())
      return null;

    Collection<Transition> linearTransitions = new ArrayList<Transition>();

    linearTransitions = collectLinearTransitions(getEntryKey(), linearTransitions);

    if(linearTransitions == null)
      return null;

    return new MultiStepsSingleEntryTransition(getTransitionPlan(), getKey(), getEntryKey(), linearTransitions);
  }

  @Override
  protected Collection<Transition> collectLinearTransitions(String entryKey,
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
      Transition transition = findSingleExecuteBefore();
      if(transition != null)
      {
        Transition executeAfter = transition.findSingleExecuteAfter();
        if(executeAfter != null && getKey().equals(executeAfter.getKey()))
        {
          linearTransitions.add(this);
          return transition.collectLinearTransitions(entryKey, linearTransitions);
        }
      }
    }

    return null;
  }

  @Override
  public void addSteps(ICompositeStepBuilder<ActionDescriptor> builder)
  {
    InternalSystemEntryDelta entryDelta = getSystemEntryDelta();

    if(entryDelta == null)
      return;

    InternalActionDescriptor actionDescriptor;

    if(INSTALL_SCRIPT_ACTION.equals(getAction()))
    {
      ScriptLifecycleInstallActionDescriptor ad = new ScriptLifecycleInstallActionDescriptor();
      SystemEntry expectedEntry = entryDelta.getExpectedEntry();
      if(!expectedEntry.isDefaultParent())
        ad.setParent(expectedEntry.getParent());
      ad.setScript(expectedEntry.getScript());
      ad.setInitParameters((Map) expectedEntry.getInitParameters());
      actionDescriptor = ad;
    }
    else
    {
      if(UNINSTALL_SCRIPT_ACTION.equals(getAction()))
      {
        actionDescriptor = new ScriptLifecycleUninstallActionDescriptor();

      }
      else
      {
        // TODO HIGH YP:  handle actionArgs
        Map actionArgs = null;

        ScriptTransitionActionDescriptor ad = new ScriptTransitionActionDescriptor();
        ad.setAction(getAction());
        ad.setToState(getToState());
        ad.setActionArgs(actionArgs);
        actionDescriptor = ad;
      }
    }

    actionDescriptor = populateActionDescriptor(actionDescriptor);

    SkippableTransition skipRootCause = getSkipRootCause();
    if(skipRootCause != null)
    {
      NoOpActionDescriptor noopActionDescriptor = skipRootCause.computeActionDescriptor();
      actionDescriptor = adjustForNoOp(actionDescriptor,  noopActionDescriptor);
    }

    builder.addLeafStep(buildStep(actionDescriptor));
  }

  private NoOpActionDescriptor adjustForNoOp(InternalActionDescriptor actionDescriptor,
                                             NoOpActionDescriptor noopActionDescriptor)
  {
    for(Map.Entry<String, Object> entry : actionDescriptor.getValues().entrySet())
    {
      String key = entry.getKey();
      Object value = noopActionDescriptor.findValue(key);
      if(value == null)
      {
        noopActionDescriptor.setValue(key, value);
      }
      else
      {
        if(!value.equals(entry.getValue()))
        {
          noopActionDescriptor.setValue(key, entry.getValue());
          noopActionDescriptor.setValue(key + "RootCause", value);
        }
      }
    }
    return noopActionDescriptor;
  }
}
