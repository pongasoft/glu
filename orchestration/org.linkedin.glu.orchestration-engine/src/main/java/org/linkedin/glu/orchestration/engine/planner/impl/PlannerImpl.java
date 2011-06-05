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
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptLifecycleInstallActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptLifecycleUninstallActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptTransitionActionDescriptor;
import org.linkedin.glu.orchestration.engine.agents.NoSuchAgentException;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta;
import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemEntryDelta;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemModelDelta;
import org.linkedin.glu.orchestration.engine.planner.Planner;
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.LeafStep;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.groovy.util.state.StateMachine;
import org.linkedin.util.annotations.Initializer;
import org.linkedin.util.lang.LangUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class PlannerImpl implements Planner
{
  public enum ActionFromStatus
  {
    NO_ACTION,
    REDEPLOY,
    FIX_MISMATCH_STATE
  }

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

    Set<String> keys = systemModelDelta.getKeys(new HashSet<String>());

    Transitions transitions = new Transitions((InternalSystemModelDelta) systemModelDelta);

    for(String key : keys)
    {
      processEntryDelta(transitions, transitions.getSystemModelDelta().findAnyEntryDelta(key));
    }

    return transitions.buildPlan(type);
  }

  @Override
  public Plan<ActionDescriptor> computeTransitionPlan(IStep.Type type,
                                                      SystemModelDelta systemModelDelta,
                                                      Collection<String> toStates)
  {
    if(systemModelDelta == null)
      return null;

    return null;
//    InternalSystemModelDelta ismd = (InternalSystemModelDelta) systemModelDelta;
//
//    PlanBuilder<ActionDescriptor> builder = new PlanBuilder<ActionDescriptor>();
//
//    ICompositeStepBuilder<ActionDescriptor> stepBuilder = builder.addCompositeSteps(type);
//
//    Set<String> keys = new TreeSet<String>(systemModelDelta.getKeys());
//
//    for(String key : keys)
//    {
//      processEntryTransition(stepBuilder,
//                             ismd,
//                             ismd.findAnyEntryDelta(key),
//                             toStates);
//    }
//
//    return builder.toPlan();
  }

  /**
   * Processes one entry transition
   */
  protected void processEntryTransition(Transitions transitions,
                                        InternalSystemEntryDelta entryDelta,
                                        Collection<String> toStates)
  {
    // TODO HIGH YP:  handle this
//    if(addNoOpStepOnNotOkToProcess(stepBuilder, systemModelDelta, entryDelta))
//      return;

    Transition lastTransition = null;

    String fromState = entryDelta.getCurrentEntryState();

    for(String toState : toStates)
    {
      if("<expected>".equals(toState))
        toState = entryDelta.getExpectedEntryState();
      
      lastTransition = processEntryStateMismatch(transitions,
                                                 lastTransition,
                                                 entryDelta,
                                                 fromState,
                                                 toState);

      fromState = toState;
    }
  }

  /**
   * Processes one entry delta (agent/mountPoint)
   */
  protected void processEntryDelta(Transitions transitions,
                                   InternalSystemEntryDelta entryDelta)
  {
    // TODO HIGH YP: handle this
//    if(addNoOpStepOnNotOkToProcess(stepBuilder, systemModelDelta, entryDelta))
//      return;

    if(entryDelta.getDeltaState() == SystemEntryDelta.DeltaState.ERROR)
    {
      switch(computeActionFromStatus(entryDelta))
      {
        case NO_ACTION:
          break;

        case REDEPLOY:
          processEntryTransition(transitions,
                                 entryDelta,
                                 DELTA_TRANSITIONS);
          break;

        case FIX_MISMATCH_STATE:
          processEntryStateMismatch(transitions,
                                    null,
                                    entryDelta);
          break;

        default:
          throw new RuntimeException("not reached");
      }
    }
  }

  protected ActionFromStatus computeActionFromStatus(InternalSystemEntryDelta entryDelta)
  {
    String deltaStatus = entryDelta.getDeltaStatus();

    // when expectedState or in error there is nothing to do
    if("expectedState".equals(deltaStatus) ||
       "error".equals(deltaStatus))
    {
      return ActionFromStatus.NO_ACTION;
    }

    // this means that there is a delta => need to undeploy/redeploy
    if("delta".equals(deltaStatus) ||
       "parentDelta".equals(deltaStatus))
    {
      return ActionFromStatus.REDEPLOY;
    }

    // all other states will trigger a fix state mismatch by doing proper action
    return ActionFromStatus.FIX_MISMATCH_STATE;
  }

  /**
   * Check if the entry is ok to process meaning it is not in transition and the agent is available
   * @return <code>true</code> if noop steps were added
   */
  protected boolean addNoOpStepOnNotOkToProcess(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
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
  protected void addNoOpStep(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
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
   * Process entry state mismatch
   */
  protected Transition processEntryStateMismatch(Transitions transitions,
                                                 Transition lastTransition,
                                                 InternalSystemEntryDelta entryDelta)
  {
    return processEntryStateMismatch(transitions,
                                     lastTransition,
                                     entryDelta,
                                     entryDelta.getCurrentEntryState(),
                                     entryDelta.getExpectedEntryState());
  }

  /**
   * Process entry state mismatch
   */
  protected Transition processEntryStateMismatch(Transitions transitions,
                                                 Transition lastTransition,
                                                 InternalSystemEntryDelta entryDelta,
                                                 Object fromState,
                                                 Object toState)
  {
    return transitions.addTransition(lastTransition, entryDelta, fromState, toState);
  }


  /**
   * Add installScript step
   */
  protected void addLifecycleInstallStep(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                         InternalSystemModelDelta systemModelDelta,
                                         SystemEntryDelta entryDelta)
  {
    ScriptLifecycleInstallActionDescriptor actionDescriptor =
      new ScriptLifecycleInstallActionDescriptor("TODO script lifecycle: installScript",
                                                 systemModelDelta.getFabric(),
                                                 entryDelta.getAgent(),
                                                 entryDelta.getMountPoint(),
                                                 entryDelta.getExpectedEntry().getParent(),
                                                 entryDelta.getExpectedEntry().getScript(),
                                                 (Map) entryDelta.getExpectedEntry().getInitParameters());

    addLeafStep(stepBuilder, actionDescriptor);
  }

  /**
   * Add uninstallScript step
   */
  protected void addLifecycleUninstallStep(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                           InternalSystemModelDelta systemModelDelta,
                                           SystemEntryDelta entryDelta)
  {
    ScriptLifecycleUninstallActionDescriptor actionDescriptor =
      new ScriptLifecycleUninstallActionDescriptor("TODO script lifecycle: uninstallScript",
                                                   systemModelDelta.getFabric(),
                                                   entryDelta.getAgent(),
                                                   entryDelta.getMountPoint());

    addLeafStep(stepBuilder, actionDescriptor);
  }

  /**
   * Add all transition steps from -> to
   */
  protected void addTransitionSteps(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                    InternalSystemModelDelta systemModelDelta,
                                    SystemEntryDelta entryDelta,
                                    Object fromState,
                                    Object toState)
  {
    // nothing to do if both states are equal!
    if(LangUtils.isEqual(fromState, toState))
      return;

    if(fromState == null)
      fromState = StateMachine.NONE;

    if(toState == null)
      toState = StateMachine.NONE;

    @SuppressWarnings("unchecked")
    Collection<Map<String,String>> path =
      (Collection<Map<String,String>>) entryDelta.getStateMachine().findShortestPath(fromState,
                                                                                     toState);
    for(Map<String, String> p : path)
    {
      addTransitionStep(stepBuilder, systemModelDelta, entryDelta, p.get("action"), p.get("to"));
    }
  }

  /**
   * Add 1 transition step corresponding to the action
   */
  protected void addTransitionStep(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                   InternalSystemModelDelta systemModelDelta,
                                   SystemEntryDelta entryDelta,
                                   String action,
                                   String endState)
  {
    // TODO HIGH YP:  handle actionArgs
    Map actionArgs = null;

    ScriptTransitionActionDescriptor actionDescriptor =
      new ScriptTransitionActionDescriptor("TODO script action: " + action,
                                           systemModelDelta.getFabric(),
                                           entryDelta.getAgent(),
                                           entryDelta.getMountPoint(),
                                           action,
                                           endState,
                                           actionArgs);

    addLeafStep(stepBuilder, actionDescriptor);
  }

  /**
   * Add a leaf step with the metadata coming from the action descriptor
   */
  protected void addLeafStep(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                             ActionDescriptor actionDescriptor)
  {
    stepBuilder.addLeafStep(new LeafStep<ActionDescriptor>(null,
                                                           actionDescriptor.toMetadata(),
                                                           actionDescriptor));
  }
}
