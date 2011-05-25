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

import org.linkedin.glu.orchestration.engine.delta.DeltaMgr;
import org.linkedin.glu.orchestration.engine.delta.SystemEntryDelta;
import org.linkedin.glu.orchestration.engine.delta.SystemModelDelta;
import org.linkedin.glu.provisioner.core.model.SystemModel;
import org.linkedin.glu.provisioner.plan.api.ICompositeStepBuilder;
import org.linkedin.glu.provisioner.plan.api.IStep;
import org.linkedin.glu.provisioner.plan.api.LeafStep;
import org.linkedin.glu.provisioner.plan.api.Plan;
import org.linkedin.glu.provisioner.plan.api.PlanBuilder;
import org.linkedin.groovy.util.state.StateMachine;
import org.linkedin.util.annotations.Initializer;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author yan@pongasoft.com
 */
public class PlannerImpl implements Planner
{
  private DeltaMgr _deltaMgr;

  /**
   * Constructor
   */
  public PlannerImpl()
  {
  }

  @Override
  public Plan<ActionDescriptor> computeDeploymentPlan(IStep.Type type,
                                    SystemModel expectedModel,
                                    SystemModel currentModel)
  {
    SystemModelDelta systemModelDelta = _deltaMgr.computeDelta(expectedModel, currentModel);

    if(systemModelDelta == null || !systemModelDelta.hasDelta())
      return null;

    PlanBuilder<ActionDescriptor> builder = new PlanBuilder<ActionDescriptor>();

    ICompositeStepBuilder<ActionDescriptor> stepBuilder = builder.addCompositeSteps(type);

    for(String key : systemModelDelta.getKeys())
    {
      processEntryDelta(stepBuilder, systemModelDelta.findEntryDelta(key));
    }

    return builder.toPlan();
  }

  public DeltaMgr getDeltaMgr()
  {
    return _deltaMgr;
  }

  @Initializer(required = true)
  public void setDeltaMgr(DeltaMgr deltaMgr)
  {
    _deltaMgr = deltaMgr;
  }

  protected void processEntryDelta(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                   SystemEntryDelta entryDelta)
  {
    if(!entryDelta.hasDelta())
      return;

    if(entryDelta.findCurrentValue("metadata.transitionState") != null)
    {
      NoOpActionDescriptor actionDescriptor =
        new NoOpActionDescriptor("already in transition: " +
                                 entryDelta.findCurrentValue("metadata.transitionState"));

      addLeafStep(stepBuilder, entryDelta, actionDescriptor);

      return;
    }

    // not deployed => need to deploy it
    if(entryDelta.getCurrentEntry() == null)
    {
      processDeploy(stepBuilder.addCompositeSteps(IStep.Type.SEQUENTIAL), entryDelta);
      return;
    }

    // deployed but not expected => need to undeploy
    if(entryDelta.getExpectedEntry() == null)
    {
      processUndeploy(stepBuilder.addCompositeSteps(IStep.Type.SEQUENTIAL), entryDelta);
      return;
    }
  }

  protected void processDeploy(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                               SystemEntryDelta entryDelta)
  {
    addLifecycleStep(stepBuilder, entryDelta, ScriptLifecycle.INSTALL_SCRIPT);
    addTransitionSteps(stepBuilder,
                       entryDelta,
                       StateMachine.NONE,
                       entryDelta.getExpectedEntry().getEntryState());
  }

  protected void processUndeploy(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                 SystemEntryDelta entryDelta)
  {
    addTransitionSteps(stepBuilder,
                       entryDelta,
                       entryDelta.getCurrentEntry().getEntryState(),
                       StateMachine.NONE);
    addLifecycleStep(stepBuilder, entryDelta, ScriptLifecycle.UNINSTALL_SCRIPT);
  }

  protected void addLifecycleStep(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                  SystemEntryDelta entryDelta,
                                  ScriptLifecycle scriptLifecycle)
  {
    // TODO HIGH YP:  handle agentURI
    URI agentURI = null;

    ScriptLifecycleActionDescriptor actionDescriptor =
      new ScriptLifecycleActionDescriptor(agentURI,
                                          entryDelta.getMountPoint(),
                                          scriptLifecycle,
                                          scriptLifecycle == ScriptLifecycle.INSTALL_SCRIPT ?
                                            (Map) entryDelta.getCurrentEntry().getInitParameters() : null,
                                          "TODO script lifecycle: " + scriptLifecycle);

    addLeafStep(stepBuilder, entryDelta, actionDescriptor);
  }

  protected void addTransitionSteps(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                    SystemEntryDelta entryDelta,
                                    Object fromState,
                                    Object toState)
  {
    @SuppressWarnings("unchecked")
    Collection<Map<String,String>> path =
      (Collection<Map<String,String>>) entryDelta.getStateMachine().findShortestPath(fromState,
                                                                                     toState);
    for(Map<String, String> p : path)
    {
      addTransitionStep(stepBuilder, entryDelta, p.get("action"), p.get("to"));
    }
  }

  protected void addTransitionStep(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                                   SystemEntryDelta entryDelta,
                                   String action,
                                   String endState)
  {
    // TODO HIGH YP:  handle actionArgs
    Map actionArgs = null;

    // TODO HIGH YP:  handle agentURI
    URI agentURI = null;

    ScriptTransitionActionDescriptor actionDescriptor =
      new ScriptTransitionActionDescriptor(agentURI,
                                           entryDelta.getMountPoint(),
                                           action,
                                           endState,
                                           actionArgs,
                                           "TODO script action: " + action);

    addLeafStep(stepBuilder, entryDelta, actionDescriptor);
  }

  protected void addLeafStep(ICompositeStepBuilder<ActionDescriptor> stepBuilder,
                             SystemEntryDelta entryDelta, ActionDescriptor actionDescriptor)
  {
    stepBuilder.addLeafStep(new LeafStep<ActionDescriptor>(null,
                                                           computeMetadata(entryDelta),
                                                           actionDescriptor));
  }

  protected Map<String, Object> computeMetadata(SystemEntryDelta entryDelta)
  {
    Map<String, Object> metadata = new HashMap<String, Object>();
    metadata.put("agent", entryDelta.getAgent());
    return metadata;
  }
}
