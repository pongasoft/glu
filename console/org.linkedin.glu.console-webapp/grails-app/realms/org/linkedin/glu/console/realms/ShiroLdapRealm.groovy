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

package org.linkedin.glu.console.realms

import javax.naming.AuthenticationException
import javax.naming.Context
import javax.naming.NamingException
import javax.naming.directory.InitialDirContext

import org.apache.shiro.authc.AccountException
import org.apache.shiro.authc.CredentialsException
import org.apache.shiro.authc.IncorrectCredentialsException
import org.apache.shiro.authc.UnknownAccountException
import javax.naming.directory.SearchControls
import org.linkedin.glu.console.domain.User
import org.linkedin.glu.console.domain.RoleName
import org.linkedin.glu.console.domain.AuditLog
import org.linkedin.glu.console.domain.DbUserCredentials
import org.linkedin.glu.groovy.utils.plugins.PluginService
import org.linkedin.glu.orchestration.engine.user.UserService

/**
 * Simple realm that authenticates users against an LDAP server.
 */
class ShiroLdapRealm
{
  static authTokenClass = org.apache.shiro.authc.UsernamePasswordToken

  PluginService pluginService
  def grailsApplication

  def authenticate(authToken)
  {
    pluginService.executePrePostMethods(UserService,
                                        "authenticate",
                                        [authToken: authToken]) { args ->

      authToken = args.authToken
      def username = args.pluginResult
      if(username)
      {
        return createUser(username)
      }

      username = authToken.username
      log.debug "Attempting to authenticate ${username} in LDAP realm..."
      def password = new String(authToken.password)

      // Get LDAP config for application. Use defaults when no config
      // is provided.
      def appConfig = grailsApplication.config
      def ldapUrls = appConfig.ldap.server.url ?: ["ldap://localhost:389/"]
      def searchBase = appConfig.ldap.search.base ?: ""
      def searchUser = appConfig.ldap.search.user ?: ""
      def searchPass = appConfig.ldap.search.pass ?: ""
      def usernameAttribute = appConfig.ldap.username.attribute ?: "uid"

      // Null username is invalid
      if(username == null)
      {
        throw new AccountException("Null usernames are not allowed by this realm.")
      }

      // Empty username is invalid
      if(username == "")
      {
        throw new AccountException("Empty usernames are not allowed by this realm.")
      }

      // Null password is invalid
      if(password == null)
      {
        throw new CredentialsException("Null password are not allowed by this realm.")
      }

      // empty password is invalid
      if(password == "")
      {
        throw new CredentialsException("Empty passwords are not allowed by this realm.")
      }

      // try to see if there is a glu specific password
      def credentials = DbUserCredentials.findByUsername(username)
      if(credentials?.validatePassword(password))
      {
        return username
      }

      // Accept strings and GStrings for convenience, but convert to
      // a list.
      if(ldapUrls && !(ldapUrls instanceof Collection))
      {
        ldapUrls = [ldapUrls]
      }

      // Set up the configuration for the LDAP search we are about
      // to do.
      def env = new Hashtable()
      env[Context.INITIAL_CONTEXT_FACTORY] = "com.sun.jndi.ldap.LdapCtxFactory"
      if(searchUser)
      {
        // Non-anonymous access for the search.
        env[Context.SECURITY_AUTHENTICATION] = "simple"
        env[Context.SECURITY_PRINCIPAL] = searchUser
        env[Context.SECURITY_CREDENTIALS] = searchPass
      }

      // Find an LDAP server that we can connect to.
      InitialDirContext ctx
      def urlUsed = ldapUrls.find {url ->
        log.debug "Trying LDAP server ${url} ..."
        env[Context.PROVIDER_URL] = url

        // If an exception occurs, log it.
        try
        {
          ctx = new InitialDirContext(env)
          return true
        }
        catch (NamingException e)
        {
          log.error "Could not connect to ${url}: ${e}"
          return false
        }
      }

      if(!urlUsed)
      {
        def msg = 'No LDAP server available.'
        log.error msg
        throw new AuthenticationException(msg)
      }

      SearchControls ctrl = new SearchControls()
      ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE)

      def result = ctx.search(searchBase, "${usernameAttribute}=${username}", ctrl)
      if(!result.hasMore())
      {
        AuditLog.audit(username: username, type: 'login.failed', details: 'no account')
        throw new UnknownAccountException("No account found for user [${username}]")
      }

      // Now connect to the LDAP server again, but this time use
      // authentication with the principal associated with the given
      // username.
      def searchResult = result.next()
      env[Context.SECURITY_AUTHENTICATION] = "simple"
      env[Context.SECURITY_PRINCIPAL] = searchResult.nameInNamespace
      env[Context.SECURITY_CREDENTIALS] = password

      try
      {
        new InitialDirContext(env)
        return createUser(username)
      }
      catch (AuthenticationException ex)
      {
        AuditLog.audit(username: username, type: 'login.failed', details: 'invalid password')
        throw new IncorrectCredentialsException("Invalid password for user '${username}'")
      }
    }
  }

  private def createUser(username)
  {
    User.withTransaction { status ->
      if(!User.findByUsername(username))
      {
        User user = new User(username: username)
        user.setRoles([RoleName.USER])
        if(!user.save())
        {
          def msg = "Could not create user ${username} => ${user.errors.toString()}".toString()
          log.error msg
          throw new AuthenticationException(msg)
        }

        AuditLog.audit(username: '__console__', type: 'user.create', details: "username: ${username}, roles: ${user.roles}")
        log.info "Created user ${username} with roles ${user.roles}"
      }
    }

    return username
  }

  def hasRole(principal, roleName)
  {
    log.debug "hasRole(${principal}, ${roleName})"
    def user = User.findByUsername(principal)

    // when the user is restricted, nothing is authorized
    if(user.hasRole(RoleName.RESTRICTED))
      return false

    def hasRole = user.impliesRole(roleName)

    log.debug "hasRole(${principal}, ${roleName}): ${hasRole}"

    return hasRole
  }
}
