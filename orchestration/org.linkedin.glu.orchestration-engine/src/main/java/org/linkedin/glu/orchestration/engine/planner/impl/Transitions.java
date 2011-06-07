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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author yan@pongasoft.com
 */
public class Transitions
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
  private final Map<String, Transition> _transitions = new LinkedHashMap<String, Transition>();

  private boolean _postProcessed = false;

  /**
   * Constructor
   */
  public Transitions(InternalSystemModelDelta systemModelDelta)
  {
    _systemModelDelta = systemModelDelta;
  }

  public InternalSystemModelDelta getSystemModelDelta()
  {
    return _systemModelDelta;
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
        transition.addSteps(stepBuilder, _systemModelDelta);
      }
    }
    else
    {
      ICompositeStepBuilder<ActionDescriptor> stepBuilder = builder.addSequentialSteps();

      Set<Transition> roots =
        findRoots(new TreeSet<Transition>(Transition.TransitionComparator.INSTANCE));

      addSteps(stepBuilder, type, roots, 0, new HashSet<String>());
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
                        Set<String> alreadyProcessed)
  {
    if(transitions.size() == 0)
      return;

    ICompositeStepBuilder<ActionDescriptor> depthBuilder = stepBuilder.addCompositeSteps(type);
    depthBuilder.setMetadata("depth", depth);

    Set<Transition> nextTransitions =
      new TreeSet<Transition>(Transition.TransitionComparator.INSTANCE);

    for(Transition transition : transitions)
    {
      if(!alreadyProcessed.contains(transition.getKey()))
      {
        // make sure that all 'before' steps have been completed
        if(checkExecuteBefore(transition, alreadyProcessed))
        {
          alreadyProcessed.add(transition.getKey());
          transition.addSteps(depthBuilder, _systemModelDelta);
          for(String key : transition.getExecuteBefore())
          {
            nextTransitions.add(_transitions.get(key));
          }
        }
        else
        {
          // no they have not => add for next iteration
          nextTransitions.add(transition);
        }
      }
    }

    addSteps(stepBuilder, type, nextTransitions, depth + 1, alreadyProcessed);
  }

  private boolean checkExecuteBefore(Transition transition, Set<String> alreadyProcessed)
  {
    for(String key : transition.getExecuteAfter())
    {
      if(!alreadyProcessed.contains(key))
        return false;
    }
    return true;
  }

  public Transition addRealTransition(String entryKey, String action, String to)
  {
    String key = SingleStepTransition.computeTransitionKey(entryKey, action);
    Transition transition = _transitions.get(key);
    if(transition == null)
    {
      transition = new SingleStepTransition(key, entryKey, action, to);
      _transitions.put(key, transition);
    }
    transition.setVirtual(false);
    return transition;
  }

  /**
   * A transition is virtual if it is added by a parent/child relationship but it may not be
   * needed depending on the parent.
   */
  public Transition addVirtualTransition(String entryKey, String action, String to)
  {
    String key = SingleStepTransition.computeTransitionKey(entryKey, action);
    Transition transition = _transitions.get(key);
    if(transition == null)
    {
      transition = new SingleStepTransition(key, entryKey, action, to);
      _transitions.put(key, transition);
      transition.setVirtual(true);
      _entriesToProcess.offer(entryKey);
    }
    return transition;
  }

  private void filterVirtual()
  {

    for(Transition transition : _transitions.values())
    {
      Iterator<String> iter = transition.getExecuteAfter().iterator();
      while(iter.hasNext())
      {
        if(_transitions.get(iter.next()).isVirtual())
          iter.remove();
      }

      iter = transition.getExecuteBefore().iterator();
      while(iter.hasNext())
      {
        if(_transitions.get(iter.next()).isVirtual())
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

  private void optimizeMultiSteps()
  {
    Set<Transition> roots = findRoots(new HashSet<Transition>());

    for(Transition root : roots)
    {
      MultiStepsSingleEntryTransition mset = root.convertToMultiSteps(_transitions);
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
                                     String to,
                                     int distance)
  {
    return addTransition(entryDelta.getKey(), action, to, distance);
  }

  protected Transition addTransition(String entryKey,
                                     String action,
                                     String to,
                                     int distance)
  {
    Transition transition = addRealTransition(entryKey, action, to);
    if(distance > 0)
    {
      InternalSystemEntryDelta parentEntryDelta =
        _systemModelDelta.findExpectedParentEntryDelta(entryKey);
      if(parentEntryDelta != null)
      {
        Transition parentTransition =
          addVirtualTransition(parentEntryDelta.getKey(), action, to);
        transition.executeAfter(parentTransition);
      }
    }
    else
    {
      Collection<InternalSystemEntryDelta> childrenEntryDelta =
        _systemModelDelta.findCurrentChildrenEntryDelta(entryKey);
      for(InternalSystemEntryDelta childDelta : childrenEntryDelta)
      {
        Transition childTransition =
          addVirtualTransition(childDelta.getKey(), action, to);
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
    // TODO HIGH YP:  handle this
//    if(addNoOpStepOnNotOkToProcess(stepBuilder, systemModelDelta, entryDelta))
//      return;

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
        addTransition(entryDelta.getKey(),
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
      Transition uninstallScript = addTransition(entryDelta.getKey(),
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
