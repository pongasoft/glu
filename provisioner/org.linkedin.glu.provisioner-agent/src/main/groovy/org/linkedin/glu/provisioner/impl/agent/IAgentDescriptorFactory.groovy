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

package org.linkedin.glu.provisioner.impl.agent

/**
 * Factory for agent descriptors
 *
 * author:  Riccardo Ferretti
 * created: Aug 21, 2009
 */
public interface IAgentDescriptorFactory
{
 
  /**
   * Create the agent descriptor for the given host
   */
  IAgentDescriptor create(String host)

  /**
   * Create a descriptor for each agent in the system
   */
  List<IAgentDescriptor> createAll()

}