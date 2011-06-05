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
import org.linkedin.glu.provisioner.plan.api.LeafStep;
import org.linkedin.groovy.util.state.StateMachine;
import org.linkedin.util.lang.LangUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author yan@pongasoft.com
 */
public class Transitions
{
  public static final String ROOT_PARENT = "/";
  
  private final InternalSystemModelDelta _systemModelDelta;
  private final Map<String, Transition> _transitions = new LinkedHashMap<String, Transition>();

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

//  public Map<Transition, Collection<Transition>> getReverseTransitions()
//  {
//    Map<Transition, Collection<Transition>> reverseTransitions =
//      new LinkedHashMap<Transition, Collection<Transition>>();
//
//    for(Map.Entry<Transition, Collection<Transition>> entry : _transitions.entrySet())
//    {
//      for(Transition transition : entry.getValue())
//      {
//        Collection<Transition> rts = reverseTransitions.get(transition);
//        if(rts == null)
//        {
//          rts = new HashSet<Transition>();
//          reverseTransitions.put(transition, rts);
//        }
//        rts.add(entry.getKey());
//      }
//    }
//
//    return reverseTransitions;
//  }


  public Transition addRealTransition(String entryKey, String action, String to, int distance)
  {
    String key = Transition.computeTransitionKey(entryKey, action, to, distance);
    Transition transition = _transitions.get(key);
    if(transition == null)
    {
      transition = new Transition(key, entryKey, action, to, distance);
      _transitions.put(key, transition);
    }
    transition.setVirtual(false);
    return transition;
  }

  public Transition addVirtualTransition(String entryKey, String action, String to, int distance)
  {
    String key = Transition.computeTransitionKey(entryKey, action, to, distance);
    Transition transition = _transitions.get(key);
    if(transition == null)
    {
      transition = new Transition(key, entryKey, action, to, distance);
      _transitions.put(key, transition);
      transition.setVirtual(true);
    }
    return transition;
  }

  public void filterVirtual()
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

  protected Transition addTransition(InternalSystemEntryDelta entryDelta,
                                     String action,
                                     String to,
                                     int distance)
  {
    Transition transition = addRealTransition(entryDelta.getKey(), action, to, distance);
    if(distance > 0)
    {
      InternalSystemEntryDelta parentEntryDelta =
        _systemModelDelta.findExpectedParentEntryDelta(entryDelta.getKey());
      if(parentEntryDelta != null)
      {
        Transition parentTransition =
          addVirtualTransition(parentEntryDelta.getKey(), action, to, distance);
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
          addVirtualTransition(childDelta.getKey(), action, to, distance);
        transition.executeAfter(childTransition);
      }
    }
    return transition;
  }

  /**
   * Process entry state mismatch
   */
  public Transition processEntryStateMismatch(Transition transition,
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
        addRealTransition(entryDelta.getKey(),
                          Transition.INSTALL_SCRIPT_ACTION,
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
      Transition uninstallScript = addRealTransition(entryDelta.getKey(),
                                                     Transition.UNINSTALL_SCRIPT_ACTION,
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
        addTransition(entryDelta, p.get("action"), p.get("to"), distance > 0 ? 1 : -1);
      newTransition.executeAfter(transition);
      transition = newTransition;
    }

    return transition;
  }

  /**
   * Builds a leaf step from a transition
   */
  protected LeafStep<ActionDescriptor> buildStep(Transition transition)
  {
    if(transition == null)
      return null;

    InternalSystemEntryDelta entryDelta =
      _systemModelDelta.findAnyEntryDelta(transition.getEntryKey());

    if(entryDelta == null)
      return null;

    ActionDescriptor actionDescriptor;

    if(Transition.INSTALL_SCRIPT_ACTION.equals(transition.getAction()))
    {
      actionDescriptor =
        new ScriptLifecycleInstallActionDescriptor("TODO script lifecycle: installScript",
                                                   _systemModelDelta.getFabric(),
                                                   entryDelta.getAgent(),
                                                   entryDelta.getMountPoint(),
                                                   entryDelta.getExpectedEntry().getParent(),
                                                   entryDelta.getExpectedEntry().getScript(),
                                                   (Map) entryDelta.getExpectedEntry().getInitParameters());

    }
    else
    {
      if(Transition.UNINSTALL_SCRIPT_ACTION.equals(transition.getAction()))
      {
        actionDescriptor =
          new ScriptLifecycleUninstallActionDescriptor("TODO script lifecycle: uninstallScript",
                                                       _systemModelDelta.getFabric(),
                                                       entryDelta.getAgent(),
                                                       entryDelta.getMountPoint());

      }
      else
      {
        // TODO HIGH YP:  handle actionArgs
        Map actionArgs = null;

        actionDescriptor =
          new ScriptTransitionActionDescriptor("TODO script action: " + transition.getAction(),
                                               _systemModelDelta.getFabric(),
                                               entryDelta.getAgent(),
                                               entryDelta.getMountPoint(),
                                               transition.getAction(),
                                               transition.getTo(),
                                               actionArgs);

      }
    }

    return buildStep(actionDescriptor);
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
