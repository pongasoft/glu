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

package org.linkedin.glu.provisioner.core.action

/**
 * Abstract {@link org.linkedin.glu.provisioner.core.action.Action} to implement basic boiler-plate code
 *
 * author:  Riccardo Ferretti
 * created: Jul 27, 2009
 */
public abstract class AbstractAction implements Action
{
  protected final String _id
  protected final String _name
  protected final Map<String, String> _params
  protected final String _description
  protected final ActionDescriptor _ad

  AbstractAction(String id, ActionDescriptor ad, String name, String description, Map<String, String> params)
  {
    _id = id
    _ad = ad
    _name = name
    _description = description
    _params = Collections.unmodifiableMap(params)
  }

  public ActionDescriptor getActionDescriptor()
  {
    return _ad
  }
  
  public String getId()
  {
    return _id
  }

  public String getName()
  {
    return _name
  }

  public Map<String, String> getParams()
  {
    return _params
  }

  public String getDescription()
  {
    return _description
  }
}