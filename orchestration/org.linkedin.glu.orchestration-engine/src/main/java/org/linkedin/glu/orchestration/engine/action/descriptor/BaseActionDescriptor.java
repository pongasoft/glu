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

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author yan@pongasoft.com
 */
public class BaseActionDescriptor implements InternalActionDescriptor
{
  private Map<String, Object> _values = new HashMap<String, Object>();

  @Override
  @SuppressWarnings("unchecked")
  public <T> T findValue(String name)
  {
    return (T) _values.get(name);
  }

  @Override
  public void setValue(String name, Object value)
  {
    if(value == null)
      _values.remove(name);
    else
      _values.put(name, value);
  }

  @Override
  public Map<String, Object> getValues()
  {
    return _values;
  }

  @Override
  public String getName()
  {
    return findValue("name");
  }

  public void setName(String name)
  {
    setValue("name", name);
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
    metadata.putAll(_values);
  }
}
