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

import java.net.URI;
import java.util.Map;

/**
 * @author yan@pongasoft.com
 */
public class ScriptTransitionActionDescriptor extends MountPointActionDescriptor
{
  private final String _action;
  private final String _endState;
  private final Map _actionArgs;

  /**
   * Constructor
   */
  public ScriptTransitionActionDescriptor(URI agentURI,
                                          String mountPoint,
                                          String action,
                                          String endState,
                                          Map actionArgs,
                                          String description)
  {
    super(agentURI, mountPoint, description);
    _action = action;
    _endState = endState;
    _actionArgs = actionArgs;
  }

  public String getAction()
  {
    return _action;
  }

  public String getEndState()
  {
    return _endState;
  }

  public Map getActionArgs()
  {
    return _actionArgs;
  }
}
