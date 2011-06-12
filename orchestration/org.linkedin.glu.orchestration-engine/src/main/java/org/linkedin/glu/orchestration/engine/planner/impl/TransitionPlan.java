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
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.glu.provisioner.plan.api.PlanBuilder;
import org.linkedin.groovy.util.state.StateMachine;
import org.linkedin.util.lang.LangUtils;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author yan@pongasoft.com
 */
public class TransitionPlan
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

  private boolean _postProcessed = false;

  /**
   * Constructor
   */
  public TransitionPlan(InternalSystemModelDelta systemModelDelta,
                        AgentURIProvider agentURIProvider,
                        ActionDescriptorAdjuster actionDescriptorAdjuster)
  {
    _systemModelDelta = systemModelDelta;
    _agentURIProvider = agentURIProvider;
    _actionDescriptorAdjuster = actionDescriptorAdjuster;
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

  public Plan<ActionDescriptor> buildPlan(IStep.Type type)
  {
    postProcess();
    
    PlanBuilder<ActionDescriptor> builder = new PlanBuilder<ActionDescriptor>();

    if(isMultiStepsOnly())
    {
      ICompositeStepBuilder<ActionDescriptor> stepBuilder = builder.addCompositeSteps(type);
      for(String key : new TreeSet<String>(_transitions.keySet()))
      {
        Transition transition = _transitions.get(key);
        transition.addSteps(stepBuilder);
      }
    }
    else
    {
      ICompositeStepBuilder<ActionDescriptor> stepBuilder = builder.addSequentialSteps();

      Set<Transition> roots =
        findRoots(new TreeSet<Transition>(Transition.TransitionComparator.INSTANCE));

      addSteps(stepBuilder, type, roots, 0, new HashSet<Transition>());
    }


    return builder.toPlan();
  }

  private boolean isMultiStepsOnly()
  {
    for(Transition transition : _transitions.values())
    {
      if(!(transition instanceof MultiStepsSingleEntryTransition))
        return false;
    }

    return true;
  }

  public Set<Transition> findRoots(Set<Transition> roots)
  {
    Map<String, Transition> transitions = getTransitions();

    for(Transition transition : transitions.values())
    {
      if(transition.isRoot())
      {
        roots.add(transition);
      }
    }

    return roots;
  }

  private void addSteps(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                        IStep.Type type,
                        Set<Transition> transitions,
                        int depth,
                        Set<Transition> alreadyProcessed)
  {
    if(transitions.size() == 0)
      return;

    ICompositeStepBuilder<ActionDescriptor> depthBuilder = stepBuilder.addCompositeSteps(type);
    depthBuilder.setMetadata("depth", depth);

    Set<Transition> nextTransitions =
      new TreeSet<Transition>(Transition.TransitionComparator.INSTANCE);

    for(Transition transition : transitions)
    {
      if(!alreadyProcessed.contains(transition))
      {
        // if transition should be skip then all following transition will be skipped as well
        if(transition.shouldSkip())
        {
          transition.addSteps(depthBuilder);
          alreadyProcessed.add(transition);
        }
        else
        {
          // make sure that all 'before' steps have been completed
          if(checkExecuteBefore(transition, alreadyProcessed))
          {
            alreadyProcessed.add(transition);
            transition.addSteps(depthBuilder);
            for(Transition t : transition.getExecuteBefore())
            {
              nextTransitions.add(t);
            }
          }
          else
          {
            // no they have not => add for next iteration
            nextTransitions.add(transition);
          }
        }
      }
    }

    addSteps(stepBuilder, type, nextTransitions, depth + 1, alreadyProcessed);
  }

  private boolean checkExecuteBefore(Transition transition, Set<Transition> alreadyProcessed)
  {
    for(Transition t : transition.getExecuteAfter())
    {
      if(!alreadyProcessed.contains(t))
        return false;
    }
    return true;
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
    Transition transition = findTransition(entryDelta, key);
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

  protected Transition findTransition(InternalSystemEntryDelta entryDelta, String key)
  {
    Transition transition = _transitions.get(entryDelta.getAgent());
    if(transition != null)
      return transition;

    String entryKey = entryDelta.getKey();

    transition = _transitions.get(entryKey);
    if(transition != null)
      return transition;

    return _transitions.get(key);
  }

  protected void filterVirtual()
  {

    for(Transition transition : _transitions.values())
    {
      Iterator<Transition> iter = transition.getExecuteAfter().iterator();
      while(iter.hasNext())
      {
        if(iter.next().isVirtual())
          iter.remove();
      }

      iter = transition.getExecuteBefore().iterator();
      while(iter.hasNext())
      {
        if(iter.next().isVirtual())
          iter.remove();
      }
    }

    Iterator<Map.Entry<String,Transition>> iter = _transitions.entrySet().iterator();
    while(iter.hasNext())
    {
      Map.Entry<String, Transition> entry = iter.next();
      if(entry.getValue().isVirtual())
        iter.remove();
    }
  }

  protected void optimizeMultiSteps()
  {
    Set<Transition> roots = findRoots(new HashSet<Transition>());

    for(Transition root : roots)
    {
      MultiStepsSingleEntryTransition mset = root.convertToMultiSteps();
      if(mset != null)
      {
        for(Transition transition : mset.getTransitions())
        {
          _transitions.remove(transition.getKey());
        }
        _transitions.put(mset.getKey(), mset);
      }
    }
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

  private void postProcess()
  {
    if(_postProcessed)
      return;

    filterVirtual();
    optimizeMultiSteps();

    _postProcessed = true;
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
   * Computes the transitions to go from state to state
   *
   * @param toStates which states to process
   */
  public void computeTransitions(Collection<String> toStates)
  {
    _entriesToProcess = new ArrayDeque<String>();

    _systemModelDelta.getKeys(_entriesToProcess);

    while(!_entriesToProcess.isEmpty())
    {
      InternalSystemEntryDelta entryDelta =
        _systemModelDelta.findAnyEntryDelta(_entriesToProcess.poll());
      if(!entryDelta.isEmptyAgent())
        processEntryTransition(entryDelta, toStates);
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

    int distance = entryDelta.getStateMachine().getDistance(fromState, toState);

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
