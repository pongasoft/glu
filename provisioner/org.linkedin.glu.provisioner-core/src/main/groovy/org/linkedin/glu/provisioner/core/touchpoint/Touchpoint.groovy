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

package org.linkedin.glu.provisioner.core.touchpoint

import org.linkedin.glu.provisioner.core.action.Action
import org.linkedin.glu.provisioner.core.action.ActionDescriptor

/**
 * A touchpoint (similar to p2 touchpoint) is the entity that provides a context
 * for an action to be created/executed
 *
 * author:  Riccardo Ferretti
 * created: Aug 4, 2009
 */
public interface Touchpoint
{
  /**
   * Return the ID of the touchpoint
   */
  String getId()

  /**
   * Get the action for the given phase and installation
   */
  Action getAction(ActionDescriptor ad)

}
