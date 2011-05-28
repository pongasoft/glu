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

package org.linkedin.glu.orchestration.engine.planner;

import org.linkedin.glu.orchestration.engine.action.descriptor.ActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.AgentURIProvider;
import org.linkedin.glu.orchestration.engine.action.descriptor.NoOpActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptLifecycleInstallActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptLifecycleUninstallActionDescriptor;
import org.linkedin.glu.orchestration.engine.action.descriptor.ScriptTransitionActionDescriptor;
import org.linkedin.glu.orchestration.engine.agents.NoSuchAgentException;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta;
import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta;
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.LeafStep;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.glu.provisioner.plan.api.PlanBuilder;
import org.linkedin.groovy.util.state.StateMachine;
import org.linkedin.util.annotations.Initializer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yan@pongasoft.com
 */
public class PlannerImpl implements Planner
{
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
    
    PlanBuilder<ActionDescriptor> builder = new PlanBuilder<ActionDescriptor>();

    ICompositeStepBuilder<ActionDescriptor> stepBuilder = builder.addCompositeSteps(type);

    for(String key : systemModelDelta.getKeys())
    {
      processEntryDelta(stepBuilder, systemModelDelta, systemModelDelta.findEntryDelta(key));
    }

    return builder.toPlan();
  }

  /**
   * Processes one entry delta (agent/mountPoint)
   */
  protected void processEntryDelta(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                   SystemModelDelta systemModelDelta,
                                   SystemEntryDelta entryDelta)
  {
    if(!entryDelta.hasDelta())
      return;

    if(entryDelta.findCurrentValue("metadata.transitionState") != null)
    {
      addNoOpStep(stepBuilder,
                  entryDelta,
                  "already in transition: " +
                  entryDelta.findCurrentValue("metadata.transitionState"));
      return;
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
        return;
      }
    }

    // not deployed => need to deploy it or deployed but not expected => need to undeploy
    if(entryDelta.getCurrentEntry() == null || entryDelta.getExpectedEntry() == null)
    {
      ICompositeStepBuilder<ActionDescriptor> entryStepsBuilder =
        stepBuilder.addCompositeSteps(IStep.Type.SEQUENTIAL);

      entryStepsBuilder.setMetadata("agent", entryDelta.getAgent());
      entryStepsBuilder.setMetadata("mountPoint", entryDelta.getMountPoint());

      processEntryStateMismatch(entryStepsBuilder,
                                systemModelDelta,
                                entryDelta);
      return;
    }
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
  protected void processEntryStateMismatch(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                           SystemModelDelta systemModelDelta,
                                           SystemEntryDelta entryDelta)
  {
    processEntryStateMismatch(stepBuilder,
                              systemModelDelta,
                              entryDelta,
                              entryDelta.getCurrentEntryState(),
                              entryDelta.getExpectedEntryState());
  }

  /**
   * Process entry state mismatch
   */
  protected void processEntryStateMismatch(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                           SystemModelDelta systemModelDelta,
                                           SystemEntryDelta entryDelta,
                                           Object fromState,
                                           Object toState)
  {
    if(fromState == null)
      addLifecycleInstallStep(stepBuilder, systemModelDelta, entryDelta);

    addTransitionSteps(stepBuilder,
                       systemModelDelta,
                       entryDelta,
                       fromState,
                       toState);

    if(toState == null)
      addLifecycleUninstallStep(stepBuilder, systemModelDelta, entryDelta);
  }


  /**
   * Add installScript step
   */
  protected void addLifecycleInstallStep(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                         SystemModelDelta systemModelDelta,
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
                                           SystemModelDelta systemModelDelta,
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
                                    SystemModelDelta systemModelDelta,
                                    SystemEntryDelta entryDelta,
                                    Object fromState,
                                    Object toState)
  {
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
                                   SystemModelDelta systemModelDelta,
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
