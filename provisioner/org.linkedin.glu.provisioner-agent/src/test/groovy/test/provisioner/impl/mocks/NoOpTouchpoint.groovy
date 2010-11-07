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

package test.provisioner.impl.mocks

import org.linkedin.glu.provisioner.core.action.Action
import org.linkedin.glu.provisioner.core.action.ActionDescriptor
import org.linkedin.glu.provisioner.core.action.SimpleAction
import org.linkedin.glu.provisioner.core.touchpoint.Touchpoint

/**
 * A touchpoint that doesn't perform any action
 *
 * User: Riccardo Ferretti
 * Date: Oct 29, 2009
 */
public class NoOpTouchpoint implements Touchpoint
{

  public static final String ID = 'noop'

  public String getId()
  {
    return ID
  }

  public Action getAction(ActionDescriptor ad)
  {
    return new SimpleAction(ad, {}, {})
  }

}