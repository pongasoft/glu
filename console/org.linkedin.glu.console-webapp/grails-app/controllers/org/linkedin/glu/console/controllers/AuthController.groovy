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

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authc.AuthenticationException
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.web.util.WebUtils

class AuthController extends ControllerBase
{
  def index = { redirect(action: "login", params: params) }

  def login = {
    return [username: params.username, rememberMe: (params.rememberMe != null), targetUri: params.targetUri]
  }

  def signIn = {
    def authToken = new UsernamePasswordToken(params.username, params.password)

    // Support for "remember me"
    if(params.rememberMe)
    {
      authToken.rememberMe = true
    }

    // If a controller redirected to this page, redirect back
    // to it. Otherwise redirect to the root URI.
    def targetUri = params.targetUri ?: "/"

    // Handle requests saved by Shiro filters.
    def savedRequest = WebUtils.getSavedRequest(request)
    if(savedRequest)
    {
      targetUri = savedRequest.requestURI - request.contextPath
      if(savedRequest.queryString) targetUri = targetUri + '?' + savedRequest.queryString
    }

    try
    {
      // Perform the actual login. An AuthenticationException
      // will be thrown if the username is unrecognised or the
      // password is incorrect.
      SecurityUtils.subject.login(authToken)

      audit('login')

      redirect(uri: targetUri)
    }
    catch (AuthenticationException ex)
    {
      // Authentication failed, so display the appropriate message
      // on the login page.
      log.info "Authentication failure for user '${params.username}'."
      flash.error = message(code: "login.failed")

      // Keep the username and "remember me" setting so that the
      // user doesn't have to enter them again.
      def m = [username: params.username]
      if(params.rememberMe)
      {
        m["rememberMe"] = true
      }

      // Remember the target URI too.
      if(params.targetUri)
      {
        m["targetUri"] = params.targetUri
      }

      // Now redirect back to the login page.
      redirect(action: "login", params: m)
    }
  }

  def signOut = {
    audit('logout')
    
    // Log the user out of the application.
    SecurityUtils.subject?.logout()

    // For now, redirect back to the home page.
    redirect(uri: "/")
  }

  def unauthorized = {
    render "You do not have permission to access this page."
  }
}
