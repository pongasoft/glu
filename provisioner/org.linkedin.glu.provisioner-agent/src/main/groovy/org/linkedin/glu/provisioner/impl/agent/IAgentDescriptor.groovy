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

package org.linkedin.glu.provisioner.impl.agent

import org.linkedin.glu.provisioner.core.environment.Installation

/**
 * Provides access to agent information
 *
 * author:  Riccardo Ferretti
 * created: Aug 24, 2009
 */
public interface IAgentDescriptor
{

  /**
   * The host of the agent
   */
  String getHost()


  /**
   * Executes the closure, providing an agent to it 
   */
  void withAgent(Closure cl)


  /**
   * Return the installations present on the agent 
   */
  Map<String, Installation> getInstallations()

}