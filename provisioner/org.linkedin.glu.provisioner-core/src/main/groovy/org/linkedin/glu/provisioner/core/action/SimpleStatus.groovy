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
 * A simple implementation of the {@link IStatus} interface
 *
 * author:  Riccardo Ferretti
 * created: Aug 26, 2009
 */
public class SimpleStatus implements IStatus
{
  private final ActionDescriptor _action
  private final String _detail
  private final Throwable _th
  private final Status _status

  public SimpleStatus(ActionDescriptor action, Status status)
  {
    this (action, status, '', null)
  }

  public SimpleStatus(ActionDescriptor action, Status status, String detail, Throwable th)
  {
    _action = action
    _status = status
    _detail = detail
    _th = th
  }

  public ActionDescriptor getAction()
  {
    return _action
  }

  public Status getValue()
  {
    return _status
  }

  public String toString ( )
  {
    "Status{name='${_name}', status='${_status}'}"
  }

  public ActionDescriptor getActionDescriptor()
  {
    return _action
  }

  public String getDetail()
  {
    return _detail
  }

  public Throwable getThrowable()
  {
    return _th
  }
}