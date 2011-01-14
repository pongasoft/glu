/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.provisioner.deployment.impl

import org.linkedin.glu.provisioner.deployment.api.IDeploymentManager
import org.linkedin.glu.provisioner.plan.api.Plan
import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.core.environment.Environment
import org.linkedin.glu.provisioner.plan.api.IPlanExecution
import org.linkedin.glu.provisioner.plan.api.IPlanExecutionProgressTracker
import org.linkedin.glu.provisioner.api.planner.IPlanner
import org.linkedin.glu.provisioner.plan.api.IPlanExecutor
import org.linkedin.glu.provisioner.core.graph.DepFirstVisitor
import org.linkedin.glu.provisioner.plan.api.LeafStep
import org.linkedin.glu.provisioner.plan.api.PlanBuilder
import org.linkedin.glu.provisioner.core.action.IDescriptionProvider

/**
 * @author ypujante@linkedin.com */
public class DeploymentManager implements IDeploymentManager
{
  /**
   * The planner used to seed the deployment plan
   */
  IPlanner planner

  /**
   * The plan executor
   */
  IPlanExecutor planExecutor

  public Plan<ActionDescriptor> createPlan(String name,
                                           Environment from,
                                           Environment to,
                                           IDescriptionProvider descriptionProvider)
  {
    createPlan(name, from, to, descriptionProvider) {
      return true
    }
  }

  public Plan<ActionDescriptor> createPlan(String name,
                                           Environment from,
                                           Environment to,
                                           IDescriptionProvider descriptionProvider,
                                           Closure filter)
  {
    def graph = planner.createPlan(from, to, descriptionProvider).graph

    def planBuilder = new PlanBuilder()
    planBuilder.name = name
    def stepsBuilder = planBuilder.addSequentialSteps()

    new DepFirstVisitor(graph).accept { node ->
      def step = new LeafStep<ActionDescriptor>(null, [name: node.value.description], node.value)
      if(filter(step))
        stepsBuilder.addLeafStep(step)
    }

    return planBuilder.toPlan()
  }

  public IPlanExecution<ActionDescriptor> executePlan(Plan<ActionDescriptor> plan,
                                                      IPlanExecutionProgressTracker<ActionDescriptor> tracker)
  {
    return planExecutor.executePlan(plan, tracker)
  }
}