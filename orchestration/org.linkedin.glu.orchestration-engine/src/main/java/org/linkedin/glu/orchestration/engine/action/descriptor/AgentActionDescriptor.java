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

package org.linkedin.glu.orchestration.engine.action.descriptor;

import java.util.Map;

/**
 * @author yan@pongasoft.com
 */
public class AgentActionDescriptor extends BaseActionDescriptor
{
  private final String _agent;
  private final String _fabric;

  /**
   * Constructor
   */
  public AgentActionDescriptor(String name, String fabric, String agent)
  {
    super(name);
    _fabric = fabric;
    _agent = agent;
  }

  public String getAgent()
  {
    return _agent;
  }

  public String getFabric()
  {
    return _fabric;
  }

  @Override
  public void toMetadata(Map<String, Object> metadata)
  {
    super.toMetadata(metadata);
    metadata.put("agent", _agent);
    metadata.put("fabric", _fabric);
  }
}
