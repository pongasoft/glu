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
 * An exception occurred during the creation of an action
 *
 * User: Riccardo Ferretti
 * Date: Oct 30, 2009
 */
public class ActionCreationException extends Exception
{

  private static final long serialVersionUID = 1L;

  public ActionCreationException()
  {
    super()
  }

  public ActionCreationException(String s)
  {
    super(s);
  }

  public ActionCreationException(String s, Throwable throwable)
  {
    super(s, throwable);
  }

  public ActionCreationException(Throwable throwable)
  {
    super(throwable);
  }

}