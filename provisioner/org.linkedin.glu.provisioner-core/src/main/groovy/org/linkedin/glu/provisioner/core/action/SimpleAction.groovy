/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
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
 * A simple action that is wired in with closure to perform its operations
 *
 * author:  Riccardo Ferretti
 * created: Aug 4, 2009
 */
public class SimpleAction extends AbstractAction
{
  private final Closure _execute
  private final Closure _rollback

  public SimpleAction(String id, ActionDescriptor ad,
                      String name, String description, Map<String, String> params,
                      Closure execute, Closure rollback)
  {
    super(id, ad, name, description, params)
    _execute = execute
    _rollback = rollback
  }

  public SimpleAction(ActionDescriptor ad, Closure execute, Closure rollback)
  {
    this (ad.id + ':' + ad.actionName, ad, ad.actionName,
          ad.description, ad.actionParams, execute, rollback)
  }

  public void execute()
  {
    _execute()
  }

  public void rollback()
  {
    _rollback()
  }

  public String toString ( )
  {
    "SimpleAction{name='${_name}', description='${_description}'}"
  }
}