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
import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptorAdjuster;
import org.linkedin.glu.orchestration.engine.action.descriptor.AgentURIProvider;
import org.linkedin.glu.orchestration.engine.action.descriptor.DefaultActionDescriptorAdjuster;
import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemModelDelta;
import org.linkedin.glu.orchestration.engine.planner.TransitionPlan;
import org.linkedin.glu.orchestration.engine.planner.Planner;
import org.linkedin.glu.provisioner.core.model.SystemModel;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.util.annotations.Initializer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author yan@pongasoft.com
 */
public class PlannerImpl implements Planner
{

  protected final static Collection<String> DELTA_TRANSITIONS = Arrays.asList(null, "<expected>");

  private AgentURIProvider _agentURIProvider;
  private boolean _skipMissingAgents = true;
  private ActionDescriptorAdjuster _actionDescriptorAdjuster =
    DefaultActionDescriptorAdjuster.INSTANCE;

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

  public AgentURIProvider getAgentURIProviderForDeltaTransition()
  {
    return _skipMissingAgents ? _agentURIProvider : null;
  }

  @Initializer(required = false)
  public void setAgentURIProvider(AgentURIProvider agentURIProvider)
  {
    _agentURIProvider = agentURIProvider;
  }

  public ActionDescriptorAdjuster getActionDescriptorAdjuster()
  {
    return _actionDescriptorAdjuster;
  }

  @Initializer(required = false)
  public void setActionDescriptorAdjuster(ActionDescriptorAdjuster actionDescriptorAdjuster)
  {
    _actionDescriptorAdjuster = actionDescriptorAdjuster;
  }

  public boolean isSkipMissingAgents()
  {
    return _skipMissingAgents;
  }

  @Initializer(required = false)
  public void setSkipMissingAgents(boolean skipMissingAgents)
  {
    _skipMissingAgents = skipMissingAgents;
  }

  @Override
  public Plan<ActionDescriptor> computeDeploymentPlan(IStep.Type type,
                                                      SystemModelDelta systemModelDelta)
  {
    TransitionPlan<ActionDescriptor> transitionPlan = computeTransitionPlan(systemModelDelta);

    if(transitionPlan == null)
      return null;

    return transitionPlan.buildPlan(type);
  }

  @Override
  public TransitionPlan<ActionDescriptor> computeTransitionPlan(SystemModelDelta systemModelDelta)
  {
    if(systemModelDelta == null)
      return null;

    SingleDeltaTransitionPlan transitionPlan =
      new SingleDeltaTransitionPlan((InternalSystemModelDelta) systemModelDelta,
                                    getAgentURIProviderForDeltaTransition(),
                                    _actionDescriptorAdjuster);

    transitionPlan.computeTransitionsToFixDelta();

    return transitionPlan;
  }

  @Override
  public TransitionPlan<ActionDescriptor> computeTransitionPlan(Collection<SystemModelDelta> deltas)
  {
    if(deltas == null || deltas.size() == 0)
      return null;

    if(deltas.size() == 1)
      return computeTransitionPlan(deltas.iterator().next());

    // sanity check
    SystemModel sm = null;
    for(SystemModelDelta delta : deltas)
    {
      if(sm == null)
        sm = delta.getExpectedSystemModel();
      else
      {
        if(!sm.equals(delta.getCurrentSystemModel()))
          throw new IllegalArgumentException("previous expected model must be current model");
        else
          sm = delta.getExpectedSystemModel();
      }
    }

    Collection<SingleDeltaTransitionPlan> transitionPlans =
      new ArrayList<SingleDeltaTransitionPlan>(deltas.size());

    int sequenceNumber = 0;

    for(SystemModelDelta delta : deltas)
    {
      SingleDeltaTransitionPlan transitionPlan =
        new SingleDeltaTransitionPlan((InternalSystemModelDelta) delta,
                                      getAgentURIProviderForDeltaTransition(),
                                      _actionDescriptorAdjuster,
                                      sequenceNumber++);
      transitionPlan.computeTransitionsToFixDelta();
      transitionPlans.add(transitionPlan);
    }

    return new MultiDeltaTransitionPlan(transitionPlans);
  }

  @Override
  public Plan<ActionDescriptor> computeTransitionPlan(IStep.Type type,
                                                      SystemModelDelta systemModelDelta,
                                                      Collection<String> toStates)
  {
    throw new RuntimeException("not implemented anymore");
  }
}
