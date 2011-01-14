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

package org.linkedin.glu.console.domain

import org.linkedin.util.clock.Timespan

/**
 * @author ypujante@linkedin.com */
class DbDeployment
{
  static constraints = {
    startDate(nullable: false)
    endDate(nullable: true)
    username(nullable: true)
    fabric(nullable: false)
    description(nullable: true)
    status(nullable: true)
    details(nullable: true)
  }

  static mapping = {
    columns {
      details type: 'text'
    }
  }

  Date startDate = new Date()
  Date endDate
  String username
  String fabric
  String description
  String status
  String details // xml representation of the plan

  public Timespan getDuration()
  {
    if(endDate)
      return new Timespan(endDate.time - startDate.time)
    else
      return null
  }

  static transients = ['duration']
}
