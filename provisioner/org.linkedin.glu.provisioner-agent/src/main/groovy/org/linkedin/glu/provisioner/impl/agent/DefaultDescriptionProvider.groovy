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

import org.linkedin.glu.provisioner.core.environment.Installation
import org.linkedin.glu.provisioner.core.action.IDescriptionProvider

/**
 * @author ypujante@linkedin.com */
class DefaultDescriptionProvider implements IDescriptionProvider
{
  static DefaultDescriptionProvider INSTANCE = new DefaultDescriptionProvider()
  
  String computeDescription(String actionId, Installation i)
  {
    switch (actionId)
    {
      case "noop": return i.transitionState ? "Currently in transition [${i.transitionState}] => noop" : 'noop'
      case "installscript": return "Install script for installation " + i.name + " on " + i.hostname
      case "install": return "Install " + i.name + " on " + i.hostname
      case "configure": return "Configure " + i.name + " on " + i.hostname
      case "start": return "Start " + i.name + " on " + i.hostname
      case "stop": return "Stop " + i.name + " on " + i.hostname
      case "unconfigure": return "Unconfigure " + i.name + " on " + i.hostname
      case "uninstall": return "Uninstall " + i.name + " from " + i.hostname
      case "uninstallscript": return "Uninstall script for installation " + i.name + " on " + i.hostname
    }
  }
}
