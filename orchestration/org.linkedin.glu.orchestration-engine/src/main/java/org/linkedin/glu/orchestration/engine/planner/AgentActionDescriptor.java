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

/**
 * @author yan@pongasoft.com
 */
public class AgentActionDescriptor extends BaseActionDescriptor
{
  private final URI _agentURI;

  /**
   * Constructor
   */
  public AgentActionDescriptor(URI agentURI,
                               String description)
  {
    super(description);
    _agentURI = agentURI;
  }

  public URI getAgentURI()
  {
    return _agentURI;
  }
}
