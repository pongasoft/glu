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
import java.util.TreeMap;

/**
 * @author yan@pongasoft.com
 */
public class BaseActionDescriptor implements ActionDescriptor
{
  private final String _description;

  /**
   * Constructor
   */
  public BaseActionDescriptor(String description)
  {
    _description = description;
  }

  @Override
  public String getDescription()
  {
    return _description;
  }

  @Override
  public Map<String, Object> toMetadata()
  {
    Map<String, Object> metadata = new TreeMap<String, Object>();
    toMetadata(metadata);
    return metadata;
  }

  @Override
  public void toMetadata(Map<String, Object> metadata)
  {
    metadata.put("description", _description);
  }
}
