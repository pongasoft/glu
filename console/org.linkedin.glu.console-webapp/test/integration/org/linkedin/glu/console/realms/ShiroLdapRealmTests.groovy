/*
 * Copyright (c) 2014 Yan Pujante
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

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import org.apache.shiro.authc.IncorrectCredentialsException
import org.apache.shiro.authc.UnknownAccountException
import org.linkedin.glu.console.domain.DbUserCredentials
import org.linkedin.glu.console.domain.RoleName
import org.linkedin.glu.console.domain.User

import javax.naming.AuthenticationException
import javax.naming.NamingEnumeration
import javax.naming.directory.DirContext
import javax.naming.directory.SearchControls

/**
 * @author yan@pongasoft.com  */
@TestMixin(IntegrationTestMixin)
public class ShiroLdapRealmTests extends GroovyTestCase
{
  def grailsApplication
  def _shiroLdapRealm

  def getShiroLdapRealm()
  {
    if(!_shiroLdapRealm)
    {
      _shiroLdapRealm =
        grailsApplication.getArtefact('Realm', ShiroLdapRealm.class.name).referenceInstance

      _shiroLdapRealm.pluginService = grailsApplication.mainContext.getBean('pluginService')
      _shiroLdapRealm.grailsApplication = grailsApplication
      _shiroLdapRealm.initialContextFactoryClass = MockInitialDirContextFactory.class.name
    }

    return _shiroLdapRealm
  }

  public void testNoLdap()
  {
    grailsApplication.config.ldap.server.url = []

    shouldFail(UnknownAccountException) { shiroLdapRealm.authenticate(username: 'user1',
                                                                      password: 'pwd1') }

    createUser('user1', 'pwd1', [RoleName.USER])

    shouldFail(IncorrectCredentialsException) { shiroLdapRealm.authenticate(username: 'user1',
                                                                            password: 'xxx') }


    assertEquals('user1', shiroLdapRealm.authenticate(username: 'user1', password: 'pwd1'))
  }

  public void testWithLdap()
  {
    grailsApplication.config.ldap.server.url = 'ldap://ignored'

    boolean search = true
    String password = "pwd1"

    MockInitialDirContextFactory.initialContextFactory = { env ->
      if(search)
      {
        assertEquals([
                       'java.naming.provider.url': 'ldap://ignored',
                       'java.naming.factory.initial': MockInitialDirContextFactory.class.name
                     ],
                     new HashMap(env))
        search = false
        [
          search: { String name,
                    String filter,
                    SearchControls cons ->
            assertEquals("uid=user1", filter.toString())
            [
              hasMore: { true },
              next: { [ nameInNamespace: 'foo' ] }
            ]  as NamingEnumeration
          } ] as DirContext
      }
      else
      {
        assertEquals([
                       'java.naming.provider.url': 'ldap://ignored',
                       'java.naming.factory.initial': MockInitialDirContextFactory.class.name,
                       'java.naming.security.principal': 'foo',
                       'java.naming.security.credentials': password,
                       'java.naming.security.authentication': 'simple'
                     ],
                     new HashMap(env))
        if(password == 'pwd1')
          [:] as DirContext
        else
          throw new AuthenticationException("invalid password")
      }
    }

    shiroLdapRealm.authenticate(username: 'user1', password: 'pwd1')

    search = true
    password = "pwd2"

    shouldFail(IncorrectCredentialsException) {
      shiroLdapRealm.authenticate(username: 'user1', password: 'pwd2')
    }
  }

  private static void createUser(String username, String password, def roles)
  {
    User.withTransaction { status ->
      User user = new User(username: username)
      user.updateRoles(roles)
      if(!user.save())
        throw new RuntimeException("could not create ${username} user")

      DbUserCredentials credentials = new DbUserCredentials(username: username,
                                                            password: password)
      if(!credentials.save())
      {
        status.setRollbackOnly() // rollback the transaction
        throw new RuntimeException("could not create ${username} user")
      }
    }
  }

}