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
import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemEntryDelta;
import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemModelDelta;
import org.linkedin.glu.orchestration.engine.planner.TransitionPlan;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.groovy.util.state.StateMachine;
import org.linkedin.util.lang.LangUtils;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * @author yan@pongasoft.com
 */
public class SingleDeltaTransitionPlan implements TransitionPlan<ActionDescriptor>
{
  public enum ActionFromStatus
  {
    NO_ACTION,
    REDEPLOY,
    FIX_MISMATCH_STATE
  }

  public static final String ROOT_PARENT = "/";
  protected final static Collection<String> DELTA_TRANSITIONS = Arrays.asList(null, "<expected>");

  private Deque<String> _entriesToProcess;
  private final InternalSystemModelDelta _systemModelDelta;
  private final AgentURIProvider _agentURIProvider;
  private final ActionDescriptorAdjuster _actionDescriptorAdjuster;
  private final Map<String, Transition> _transitions = new HashMap<String, Transition>();
  private final int _sequenceNumber;

  private TransitionPlan<ActionDescriptor> _transitionPlan;

  /**
   * Constructor
   */
  public SingleDeltaTransitionPlan(InternalSystemModelDelta systemModelDelta,
                                   AgentURIProvider agentURIProvider,
                                   ActionDescriptorAdjuster actionDescriptorAdjuster)
  {
    this(systemModelDelta, agentURIProvider, actionDescriptorAdjuster, 0);
  }

  /**
   * Constructor
   */
  public SingleDeltaTransitionPlan(InternalSystemModelDelta systemModelDelta,
                                   AgentURIProvider agentURIProvider,
                                   ActionDescriptorAdjuster actionDescriptorAdjuster,
                                   int sequenceNumber)
  {
    _systemModelDelta = systemModelDelta;
    _agentURIProvider = agentURIProvider;
    _actionDescriptorAdjuster = actionDescriptorAdjuster;
    _sequenceNumber = sequenceNumber;
  }

  public int getSequenceNumber()
  {
    return _sequenceNumber;
  }

  public InternalSystemModelDelta getSystemModelDelta()
  {
    return _systemModelDelta;
  }

  public AgentURIProvider getAgentURIProvider()
  {
    return _agentURIProvider;
  }

  public ActionDescriptorAdjuster getActionDescriptorAdjuster()
  {
    return _actionDescriptorAdjuster;
  }

  public Map<String, Transition> getTransitions()
  {
    return _transitions;
  }

  public TransitionPlan<ActionDescriptor> getTransitionPlan()
  {
    if(_transitionPlan == null)
      _transitionPlan = new TransitionPlanImpl(new HashSet<Transition>(_transitions.values()));
    return _transitionPlan;
  }

  /**
   * @return a map where the key is entry key and the value is the 'last' transition executed for
   * this entry key
   */
  public Map<String, SingleEntryTransition> getLastTransitions()
  {
    Map<String, SingleEntryTransition> lastTransitions =
      new HashMap<String, SingleEntryTransition>();

    for(Transition transition : _transitions.values())
    {
      if(transition instanceof SingleEntryTransition)
      {
        SingleEntryTransition set = (SingleEntryTransition) transition;
        if(!lastTransitions.containsKey(set.getEntryKey()))
          lastTransitions.put(set.getEntryKey(), set.findLastTransition());
      }
    }

    return lastTransitions;
  }

  /**
   * @return a map where the key is entry key and the value is the 'first' transition executed for
   * this entry key
   */
  public Map<String, SingleEntryTransition> getFirstTransitions()
  {
    Map<String, SingleEntryTransition> firstTransitions =
      new HashMap<String, SingleEntryTransition>();

    for(Transition transition : _transitions.values())
    {
      if(transition instanceof SingleEntryTransition)
      {
        SingleEntryTransition set = (SingleEntryTransition) transition;
        if(!firstTransitions.containsKey(set.getEntryKey()))
          firstTransitions.put(set.getEntryKey(), set.findFirstTransition());
      }
    }

    return firstTransitions;
  }

  public Plan<ActionDescriptor> buildPlan(IStep.Type type)
  {
    return getTransitionPlan().buildPlan(type);
  }

  /**
   * A transition is virtual if it is added by a parent/child relationship but it may not be
   * needed depending on the parent.
   */
  public Transition addTransition(InternalSystemEntryDelta entryDelta,
                                  String action,
                                  String toState,
                                  boolean isVirtual)
  {
    String entryKey = entryDelta.getKey();
    String key = SingleStepTransition.computeTransitionKey(entryKey, action);
    Transition transition = _transitions.get(key);
    if(transition == null)
    {
      transition = createTransition(entryDelta, key, action, toState);
      _transitions.put(key, transition);
      if(isVirtual)
      {
        _entriesToProcess.offer(entryKey);
      }
    }
    if(!isVirtual)
      transition.clearVirtual();
    return transition;
  }

  protected Transition createTransition(InternalSystemEntryDelta entryDelta,
                                        String key,
                                        String action,
                                        String toState)
  {
    Object transitionState = entryDelta.findCurrentValue("metadata.transitionState");
    if(transitionState != null)
    {
      return new AlreadyInTransition(this,
                                     key,
                                     entryDelta.getKey(),
                                     action,
                                     toState,
                                     transitionState);
    }
    if(_agentURIProvider != null)
    {
      if(_agentURIProvider.findAgentURI(getSystemModelDelta().getFabric(),
                                        entryDelta.getAgent()) == null)
      {
        return new MissingAgentTransition(this,
                                          key,
                                          entryDelta.getKey(),
                                          action,
                                          toState);
      }
    }

    return new SingleStepTransition(this, key, entryDelta.getKey(), action, toState);
  }

  protected Transition addTransition(InternalSystemEntryDelta entryDelta,
                                     String action,
                                     String toState,
                                     int distance)
  {
    Transition transition = addTransition(entryDelta, action, toState, false);
    if(distance > 0)
    {
      InternalSystemEntryDelta parentEntryDelta =
        _systemModelDelta.findExpectedParentEntryDelta(entryDelta.getKey());
      if(parentEntryDelta != null)
      {
        Transition parentTransition =
          addTransition(parentEntryDelta, action, toState, true);
        transition.executeAfter(parentTransition);
      }
    }
    else
    {
      Collection<InternalSystemEntryDelta> childrenEntryDelta =
        _systemModelDelta.findCurrentChildrenEntryDelta(entryDelta.getKey());
      for(InternalSystemEntryDelta childDelta : childrenEntryDelta)
      {
        Transition childTransition =
          addTransition(childDelta, action, toState, true);
        transition.executeAfter(childTransition);
      }
    }
    return transition;
  }

  /**
   * Compute the transitions necessary to fix the delta
   */
  public void computeTransitionsToFixDelta()
  {
    _entriesToProcess = new ArrayDeque<String>();

    _systemModelDelta.getKeys(_entriesToProcess);

    while(!_entriesToProcess.isEmpty())
    {
      processEntryDelta(_systemModelDelta.findAnyEntryDelta(_entriesToProcess.poll()));
    }
  }

  /**
   * Processes one entry transition
   */
  protected void processEntryTransition(InternalSystemEntryDelta entryDelta,
                                        Collection<String> toStates)
  {
    Transition lastTransition = null;

    String fromState = entryDelta.getCurrentEntryState();

    for(String toState : toStates)
    {
      if("<expected>".equals(toState))
        toState = entryDelta.getExpectedEntryState();

      lastTransition = processEntryStateMismatch(lastTransition,
                                                 entryDelta,
                                                 fromState,
                                                 toState);

      fromState = toState;
    }
  }

  /**
   * Processes one entry delta (agent/mountPoint)
   */
  protected void processEntryDelta(InternalSystemEntryDelta entryDelta)
  {
    if(entryDelta.getDeltaState() == SystemEntryDelta.DeltaState.ERROR)
    {
      switch(computeActionFromStatus(entryDelta))
      {
        case NO_ACTION:
          break;

        case REDEPLOY:
          processEntryTransition(entryDelta,
                                 DELTA_TRANSITIONS);
          break;

        case FIX_MISMATCH_STATE:
          processEntryStateMismatch(null,
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
   * Process entry state mismatch
   */
  protected Transition processEntryStateMismatch(Transition lastTransition,
                                                 InternalSystemEntryDelta entryDelta)
  {
    return processEntryStateMismatch(lastTransition,
                                     entryDelta,
                                     entryDelta.getCurrentEntryState(),
                                     entryDelta.getExpectedEntryState());
  }

  /**
   * Process entry state mismatch
   */
  protected Transition processEntryStateMismatch(Transition lastTransition,
                                                 InternalSystemEntryDelta entryDelta,
                                                 Object fromState,
                                                 Object toState)
  {
    return addTransition(lastTransition, entryDelta, fromState, toState);
  }



  /**
   * Process entry state mismatch
   */
  protected Transition addTransition(Transition transition,
                                     InternalSystemEntryDelta entryDelta,
                                     Object fromState,
                                     Object toState)
  {
    // nothing to do if both states are equal!
    if(LangUtils.isEqual(fromState, toState))
      return null;

    if(fromState == null)
    {
      Transition installScript =
        addTransition(entryDelta,
                      SingleStepTransition.INSTALL_SCRIPT_ACTION,
                      (String) StateMachine.NONE,
                      1);
      installScript.executeAfter(transition);
      transition = installScript;
    }

    transition = addTransitionSteps(transition,
                                    entryDelta,
                                    fromState,
                                    toState);

    if(toState == null)
    {
      Transition uninstallScript = addTransition(entryDelta,
                                                 SingleStepTransition.UNINSTALL_SCRIPT_ACTION,
                                                 null,
                                                 -1);
      uninstallScript.executeAfter(transition);
      transition = uninstallScript;
    }

    return transition;
  }

  /**
   * Add all transition steps from -> to
   */
  protected Transition addTransitionSteps(Transition transition,
                                          InternalSystemEntryDelta entryDelta,
                                          Object fromState,
                                          Object toState)
  {
    // nothing to do if both states are equal!
    if(LangUtils.isEqual(fromState, toState))
      return transition;

    if(fromState == null)
      fromState = StateMachine.NONE;

    if(toState == null)
      toState = StateMachine.NONE;

    // when no state machine (empty agents) nothing to do...
    StateMachine stateMachine = entryDelta.getStateMachine();
    if(stateMachine == null)
      return transition;

    int distance = stateMachine.getDistance(fromState, toState);

    @SuppressWarnings("unchecked")
    Collection<Map<String,String>> path =
      (Collection<Map<String,String>>) entryDelta.getStateMachine().findShortestPath(fromState,
                                                                                     toState);
    for(Map<String, String> p : path)
    {
      Transition newTransition =
        addTransition(entryDelta, p.get("action"), p.get("to"), distance);
      newTransition.executeAfter(transition);
      transition = newTransition;
    }

    return transition;
  }
}
