/*
 * Copyright 2010-2010 LinkedIn, Inc
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

package org.linkedin.glu.provisioner.impl.planner

import org.linkedin.glu.provisioner.api.planner.IPlanner
import org.linkedin.glu.provisioner.core.action.IActionDescriptorFactory
import org.linkedin.glu.provisioner.impl.agent.AgentActionDescriptorFactory

/**
 * Contains some skeleton/common code for planners
 *
 * author:  Riccardo Ferretti
 * created: Aug 19, 2009
 */
public abstract class AbstractPlanner implements IPlanner
{
  protected final Map<String, IActionDescriptorFactory> _factories = [:]

  /**
   * Default constructor provides a agent action factory
   */
  protected AbstractPlanner()
  {
    this([new AgentActionDescriptorFactory()])
  }

  /**
   * Constructor that takes a list of factories. It will use the id of the
   * factories to put them in the map
   * NOTE: if two factories have the same ID, it's not deterministic which one
   * will prevail
   */
  protected AbstractPlanner(List<IActionDescriptorFactory> factories)
  {
    _factories = [:]
    factories.each {
      _factories[it.id] = it
    }
  }

  /**
   * Define the touchpoint used by this planner 
   */
  protected AbstractPlanner(Map<String, IActionDescriptorFactory> factories)
  {
    _factories = factories
  }

}