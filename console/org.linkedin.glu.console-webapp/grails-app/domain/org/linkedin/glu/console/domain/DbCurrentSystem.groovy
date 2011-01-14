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

class DbCurrentSystem
{
  static constraints = {
    systemModel(nullable: false)
    fabric(unique: true, nullable: false, validator: {val, obj ->
      return val == obj.systemModel.fabric
    })
  }

  Date dateCreated
  Date lastUpdated
  
  static belongsTo = [systemModel: DbSystemModel]

  String fabric

  static transients = ['release']
}
