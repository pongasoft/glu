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

package org.linkedin.glu.orchestration.engine.delta;

import org.linkedin.glu.provisioner.core.model.SystemModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author yan@pongasoft.com
 */
public class SystemModelDeltaImpl implements SystemModelDelta
{
  private final Map<String, SystemEntryDelta> _deltas = new HashMap<String, SystemEntryDelta>();

  private final SystemModel _expectedSystemModel;
  private final SystemModel _currentSystemModel;

  /**
   * Constructor
   */
  public SystemModelDeltaImpl(SystemModel expectedSystemModel, SystemModel currentSystemModel)
  {
    if(!expectedSystemModel.getFabric().equals(currentSystemModel.getFabric()))
      throw new IllegalArgumentException("mismatch fabric");

    _expectedSystemModel = expectedSystemModel;
    _currentSystemModel = currentSystemModel;
  }

  @Override
  public String getFabric()
  {
    return _expectedSystemModel.getFabric();
  }

  public SystemModel getExpectedSystemModel()
  {
    return _expectedSystemModel;
  }

  public SystemModel getCurrentSystemModel()
  {
    return _currentSystemModel;
  }

  public Map<String, SystemEntryDelta> getDeltas()
  {
    return _deltas;
  }

  @Override
  public Set<String> getKeys()
  {
    return _deltas.keySet();
  }

  @Override
  public SystemEntryDelta findEntryDelta(String key)
  {
    return _deltas.get(key);
  }

  public void setEntryDelta(SystemEntryDelta delta)
  {
    if(delta != null)
      _deltas.put(delta.getKey(), delta);
  }

  @Override
  public boolean hasDelta()
  {
    for(SystemEntryDelta delta : _deltas.values())
    {
      if(delta.hasDelta())
        return true;
    }
    
    return false;
  }
}
