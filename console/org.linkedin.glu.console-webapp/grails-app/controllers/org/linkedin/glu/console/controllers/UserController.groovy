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

package org.linkedin.glu.console.controllers

import org.linkedin.glu.console.domain.User
import org.linkedin.glu.console.domain.DbUserCredentials
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.SecurityUtils
import org.linkedin.glu.console.domain.RoleName

class UserController extends ControllerBase
{
  def index = { redirect(action: list, params: params) }

  // the delete and update actions only accept POST requests
  static allowedMethods = [delete: 'POST', update: 'POST']

  def list = {
    params.max = Math.min(params.max ? params.max.toInteger() : 10, 100)
    [userInstanceList: User.list(params), userInstanceTotal: User.count()]
  }

  def show = {
    def userInstance = User.get(params.id)

    if(!userInstance)
    {
      flash.message = "User not found with id ${params.id}"
      redirect(action: list)
    }
    else
    { return [userInstance: userInstance] }
  }

  def delete = {
    def userInstance = User.get(params.id)
    if(userInstance)
    {
      try
      {
        userInstance.delete(flush: true)
        audit('user.delete', userInstance.username)
        flash.message = "User ${params.id} deleted"
        redirect(action: list)
      }
      catch (org.springframework.dao.DataIntegrityViolationException e)
      {
        flash.message = "User ${params.id} could not be deleted"
        redirect(action: show, id: params.id)
      }
    }
    else
    {
      flash.message = "User not found with id ${params.id}"
      redirect(action: list)
    }
  }

  def edit = {
    def userInstance = User.get(params.id)

    if(!userInstance)
    {
      flash.message = "User not found with id ${params.id}"
      redirect(action: list)
    }
    else
    {
      return [userInstance: userInstance]
    }
  }

  def update = {
    def role = params.role
    if(role instanceof String)
      role = [role]
    
    def userInstance = User.get(params.id)
    if(userInstance)
    {
      if(params.version)
      {
        def version = params.version.toLong()
        if(userInstance.version > version)
        {
          userInstance.errors.rejectValue("version", "user.optimistic.locking.failure", "Another user has updated this User while you were editing.")
          render(view: 'edit', model: [userInstance: userInstance])
          return
        }
      }
      userInstance.setRoles(role)
      if(!userInstance.hasErrors() && userInstance.save())
      {
        flash.message = "User ${userInstance.username} updated"
        audit('user.updated', userInstance.username, "roles: ${role}")
        redirect(action: show, id: userInstance.id)
      }
      else
      {
        render(view: 'edit', model: [userInstance: userInstance])
      }
    }
    else
    {
      flash.message = "User not found with id ${params.id}"
      redirect(action: list)
    }
  }

  def create = {
    def userInstance = new User()
    userInstance.properties = params
    return [userInstance: userInstance]
  }

  def save = {
    def userInstance = new User(username: params.username)

    if(!params.password)
    {
      flash.error = "Please input a password"
      render(view: 'create', model: [userInstance: userInstance])
      return
    }

    if(params.passwordAgain != params.password)
    {
      flash.error = "password is different from password again"
      render(view: 'create', model: [userInstance: userInstance])
      return
    }

    userInstance.setRoles(RoleName.USER)

    DbUserCredentials ucr = new DbUserCredentials(username: params.username,
                                                  password: params.password)

    if(!userInstance.validate() || !ucr.validate())
    {
      flash.error = "Error while saving user: ${userInstance.errors} / ${ucr.errors}"
      render(view: 'create', model: [userInstance: userInstance])
      return
    }

    if(userInstance.save() && ucr.save())
    {
      flash.message = "User created."
      redirect(action: 'show', id: userInstance.id)
    }
    else
    {
      flash.error = "Error while saving user: ${userInstance.errors} / ${ucr.errors}"
      render(view: 'create', model: [userInstance: userInstance])
      return
    }
  }

  def credentials = {
    def username = request.user?.username
    [credentials: DbUserCredentials.findByUsername(username)]
  }

  def updatePassword = {
    def username = request.user?.username

    def authToken = new UsernamePasswordToken(username, params.currentPassword)

    try
    {
      // Perform the actual login. An AuthenticationException
      // will be thrown if the username is unrecognised or the
      // password is incorrect.
      SecurityUtils.subject.login(authToken)
    }
    catch (AuthenticationException ex)
    {
      flash.error = "Invalid password"
      redirect(action: 'credentials')
      return
    }

    if(!params.newPassword)
    {
      flash.error = "Please input your new password"
      redirect(action: 'credentials')
      return
    }

    if(params.newPassword != params.newPasswordAgain)
    {
      flash.error = "New password is different from New password again"
      redirect(action: 'credentials')
      return
    }

    def ucr = DbUserCredentials.findByUsername(username)

    if(!ucr)
    {
      ucr = new DbUserCredentials(username: username)
    }

    ucr.password = params.newPassword
    if(!ucr.hasErrors() && ucr.save())
    {
      flash.message = "Your password has been changed."
      redirect(action: 'credentials')
      return
    }
    else
    {
      flash.error = "Error while saving new password: ${credentials.errors}"
      redirect(action: 'credentials')
      return
    }
  }
}
