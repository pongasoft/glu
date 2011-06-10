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
import org.linkedin.glu.orchestration.engine.action.descriptor.AgentURIProvider;
import org.linkedin.glu.orchestration.engine.action.descriptor.NoOpActionDescriptor;
import org.linkedin.glu.orchestration.engine.agents.NoSuchAgentException;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta;
import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemModelDelta;
import org.linkedin.glu.orchestration.engine.planner.Planner;
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.LeafStep;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.util.annotations.Initializer;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yan@pongasoft.com
 */
public class PlannerImpl implements Planner
{

  protected final static Collection<String> DELTA_TRANSITIONS = Arrays.asList(null, "<expected>");

  private AgentURIProvider _agentURIProvider;

  /**
   * Constructor
   */
  public PlannerImpl()
  {
  }

  public AgentURIProvider getAgentURIProvider()
  {
    return _agentURIProvider;
  }

  @Initializer(required = false)
  public void setAgentURIProvider(AgentURIProvider agentURIProvider)
  {
    _agentURIProvider = agentURIProvider;
  }

  @Override
  public Plan<ActionDescriptor> computeDeploymentPlan(IStep.Type type,
                                                      SystemModelDelta systemModelDelta)
  {
    if(systemModelDelta == null)
      return null;

    TransitionPlan transitionPlan =
      new TransitionPlan((InternalSystemModelDelta) systemModelDelta, _agentURIProvider);

    transitionPlan.computeTransitionsToFixDelta();

    return transitionPlan.buildPlan(type);
  }

  @Override
  public Plan<ActionDescriptor> computeTransitionPlan(IStep.Type type,
                                                      SystemModelDelta systemModelDelta,
                                                      Collection<String> toStates)
  {
    if(systemModelDelta == null)
      return null;

    TransitionPlan transitionPlan =
      new TransitionPlan((InternalSystemModelDelta) systemModelDelta, _agentURIProvider);

    transitionPlan.computeTransitions(toStates);

    return transitionPlan.buildPlan(type);
  }

  // TODO HIGH YP:  add no step handling
  /**
   * Check if the entry is ok to process meaning it is not in transition and the agent is available
   * @return <code>true</code> if noop steps were added
   */
  private boolean addNoOpStepOnNotOkToProcess(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                                InternalSystemModelDelta systemModelDelta,
                                                SystemEntryDelta entryDelta)
  {
    if(entryDelta.findCurrentValue("metadata.transitionState") != null)
    {
      addNoOpStep(stepBuilder,
                  entryDelta,
                  "already in transition: " +
                  entryDelta.findCurrentValue("metadata.transitionState"));
      return true;
    }

    if(_agentURIProvider != null)
    {
      try
      {
        _agentURIProvider.getAgentURI(systemModelDelta.getFabric(), entryDelta.getAgent());
      }
      catch(NoSuchAgentException e)
      {
        addNoOpStep(stepBuilder,
                    entryDelta,
                    "missing agent: " + entryDelta.getAgent());
        return true;
      }
    }
    return false;
  }

  /**
   * Add a nooperation step
   */
  private void addNoOpStep(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                             SystemEntryDelta entryDelta,
                             String description)
  {
    Map<String, Object> details = new HashMap<String, Object>();
    details.put("agent", entryDelta.getAgent());
    details.put("mountPoint", entryDelta.getMountPoint());
    NoOpActionDescriptor actionDescriptor = new NoOpActionDescriptor(description, details);
    addLeafStep(stepBuilder, actionDescriptor);
  }

  /**
   * Add a leaf step with the metadata coming from the action descriptor
   */
  private void addLeafStep(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                             ActionDescriptor actionDescriptor)
  {
    stepBuilder.addLeafStep(new LeafStep<ActionDescriptor>(null,
                                                           actionDescriptor.toMetadata(),
                                                           actionDescriptor));
  }
}
