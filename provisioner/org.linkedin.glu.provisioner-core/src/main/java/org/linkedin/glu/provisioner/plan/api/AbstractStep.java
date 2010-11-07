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

package org.linkedin.glu.provisioner.plan.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author ypujante@linkedin.com
 */
public abstract class AbstractStep<T> implements IStep<T>
{
  private final Map<String, Object> _metadata = new LinkedHashMap<String, Object>();
  private final String _id;

  /**
   * Constructor
   */
  public AbstractStep(String id, Map<String, Object> metadata)
  {
    if(metadata != null)
      _metadata.putAll(metadata);
    _id = id == null ? Integer.toHexString(System.identityHashCode(this)) : id;
  }

  @Override
  public String getId()
  {
    return _id;
  }

  @Override
  public String getName()
  {
    return (String) _metadata.get("name");
  }

  @Override
  public Map<String, Object> getMetadata()
  {
    return _metadata;
  }
}
