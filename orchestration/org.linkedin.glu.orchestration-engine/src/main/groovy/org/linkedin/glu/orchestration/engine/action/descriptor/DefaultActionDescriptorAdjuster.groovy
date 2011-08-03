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

package org.linkedin.glu.orchestration.engine.action.descriptor

import org.linkedin.glu.orchestration.engine.delta.impl.InternalSystemModelDelta

/**
 * @author yan@pongasoft.com */
public class DefaultActionDescriptorAdjuster implements ActionDescriptorAdjuster
{
  public static final DefaultActionDescriptorAdjuster INSTANCE =
    new DefaultActionDescriptorAdjuster();

  @Override
  InternalActionDescriptor adjustDescriptor(InternalSystemModelDelta systemModelDelta,
                                            InternalActionDescriptor actionDescriptor)
  {
    // uses groovy dynamic method dispatching to call the appropriate method
    adjust(actionDescriptor)
  }

  /**
   * Default one when no other type found: nothing to do
   */
  protected InternalActionDescriptor adjust(InternalActionDescriptor actionDescriptor)
  {
    return actionDescriptor
  }

  /**
   * For noop
   */
  protected InternalActionDescriptor adjust(NoOpActionDescriptor ad)
  {
    String mountPoint = ad.findValue("mountPoint")
    String mountPointRootCause = ad.findValue("mountPointRootCause")
    String agent = ad.findValue("agent")

    String prefix = mountPoint ? "[${mountPoint}] on [${agent}]:" : "[${agent}]"

    switch(ad.reason)
    {
      case "alreadyInTransition":
        ad.name = "${prefix}: ${mountPointRootCause ? '[' + mountPointRootCause + ']' : ''} Currently in transition [${ad.findValue("transitionState")}] => noop".toString()
        break;

      case "missingAgent":
        ad.name = "${prefix}: Missing agent [${agent}] => noop".toString()
        break;

      default:
        ad.name = "${prefix}: noop"
    }

    return ad
  }

  /**
   * For installScript
   */
  protected InternalActionDescriptor adjust(ScriptLifecycleInstallActionDescriptor ad)
  {
    ad.name = "Install script for [${ad.mountPoint}] on [${ad.agent}]".toString()
    return ad
  }

  /**
   * For transition: action->toState
   */
  protected InternalActionDescriptor adjust(ScriptTransitionActionDescriptor ad)
  {
    ad.name = "Run [${ad.action}] phase for [${ad.mountPoint}] on [${ad.agent}]".toString()
    return ad
  }

  /**
   * For uninstallScript.
   */
  protected InternalActionDescriptor adjust(ScriptLifecycleUninstallActionDescriptor ad)
  {
    ad.name = "Uninstall script for [${ad.mountPoint}] on [${ad.agent}]".toString()
    return ad
  }

}