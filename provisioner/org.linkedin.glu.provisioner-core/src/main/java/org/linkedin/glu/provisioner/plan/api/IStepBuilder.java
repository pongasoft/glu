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

import java.util.Map;

/**
 * @author ypujante@linkedin.com
 */
public interface IStepBuilder<T>
{
  String getName();

  void setName(String name);

  String getId();

  void setId(String id);

  IStep<T> toStep();

  void setMetadata(String name, Object value);

  Map<String, Object> getMetadata();

  void setMetadata(Map<String, Object> metadata);
}