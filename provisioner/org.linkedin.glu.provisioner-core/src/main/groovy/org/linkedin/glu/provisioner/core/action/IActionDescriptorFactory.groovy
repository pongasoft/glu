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

package org.linkedin.glu.provisioner.core.action

import org.linkedin.glu.provisioner.core.environment.Installation

/**
 * A factory of action descriptors
 */
public interface IActionDescriptorFactory
{
  /**
   * Return the ID of the touchpoint
   */
  String getId()

  /**
   * Return the action descriptor for the given action and installation
   */
  ActionDescriptor getActionDescriptor(String action, Installation i)

  /**
   * Return the action descriptor for the given action and installation
   */
  ActionDescriptor getActionDescriptor(String action,
                                       Installation i,
                                       IDescriptionProvider descriptionProvider)

  /**
   * Return the list of available actions for this factory
   */
  String[] getAvailableActions()

}