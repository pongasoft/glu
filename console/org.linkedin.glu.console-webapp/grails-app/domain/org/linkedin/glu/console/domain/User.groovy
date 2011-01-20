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

class User
{
  String username

  // commented out because of issue: http://jira.codehaus.org/browse/GRAILS-7175 as a workaround
  // for https://github.com/linkedin/glu/issues#issue/19
//  static mapping = {
//    roles fetch:'join'
//  }

  static hasMany = [ roles: Role, permissions: String ]

  static constraints = {
    username(nullable: false, blank: false, unique: true)
  }

  def impliesRole(role)
  {
    role = RoleName.valueOf(role.toString())
    roles?.any { it.name.implies(role) }
  }

  def hasRole(role)
  {
    role = RoleName.valueOf(role.toString())
    roles?.any { it.name == role }
  }

  /**
   * add the role (if it does not already have it)
   * @return <code>true</code> if the role was added, <code>false</code> if already present
   */
  boolean addRole(role)
  {
    role = RoleName.valueOf(role.toString())
    if(roles?.any { it.name == role })
    {
      return false
    }
    else
    {
      addToRoles(name: role)
      return true
    }
  }

  /**
   * Removes the provided role
   * @return <code>true</code> if the role was removed or <code>false</code> if it was not present
   */
  boolean removeRole(role)
  {
    role = RoleName.valueOf(role.toString())
    def allRoles = roles.findAll { it.name == role}

    if(allRoles)
    {
      allRoles.each {
        removeFromRoles(it)
      }
      return true
    }
    else
    {
      return false
    }
  }

  /**
   * Set the roles to be exactly what is provided: conceptually remove all the roles and add only
   * the new ones.
   */
  def setRoles(newRoles)
  {
    newRoles = newRoles?.collect { RoleName.valueOf(it.toString()) } ?: []

    def rolesToRemove = roles?.findAll { !newRoles.remove(it.name) }

    // we remove the roles that need to be removed
    rolesToRemove?.each { removeRole(it) }

    // we add the ones that were not already present
    newRoles.each { addRole(it) }
  }
}
