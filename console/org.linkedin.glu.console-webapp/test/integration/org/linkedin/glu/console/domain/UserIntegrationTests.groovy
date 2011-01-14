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

/**
 * @author ypujante@linkedin.com */
class UserIntegrationTests extends GroovyTestCase
{
  public void testRoles()
  {
    User user = new User(username: 'user1')
    assertNotNull(user.save())

    user = User.findByUsername('user1')
    assertEquals('user1', user.username)
    assertNull("no roles", user.roles)

    RoleName.values().each { role ->
      assertFalseUsingGroovyTruth(user.impliesRole(role))
    }

    // add the release role
    user.addRole(RoleName.RELEASE)
    assertNotNull(user.save())

    user = User.findByUsername('user1')
    assertEquals('user1', user.username)
    assertEquals(1, user.roles.size())

    // should be user/release but not admin
    assertFalseUsingGroovyTruth(user.impliesRole(RoleName.ADMIN))
    assertTrueUsingGroovyTruth(user.impliesRole(RoleName.USER))
    assertTrueUsingGroovyTruth(user.impliesRole(RoleName.RELEASE))
    assertFalseUsingGroovyTruth(user.impliesRole(RoleName.RESTRICTED))

    // add the user role
    user.addRole(RoleName.USER)
    assertNotNull(user.save())

    user = User.findByUsername('user1')
    assertEquals('user1', user.username)
    assertEquals(2, user.roles.size())

    // should be user/release but not admin
    assertFalseUsingGroovyTruth(user.impliesRole(RoleName.ADMIN))
    assertTrueUsingGroovyTruth(user.impliesRole(RoleName.USER))
    assertTrueUsingGroovyTruth(user.impliesRole(RoleName.RELEASE))
    assertFalseUsingGroovyTruth(user.impliesRole(RoleName.RESTRICTED))

    // now we remove the release role
    def releaseRole = user.removeRole(RoleName.RELEASE)
    assertNotNull(user.save())

    assertEquals('user1', user.username)
    assertEquals(1, user.roles.size())

    // should be user only again
    assertFalseUsingGroovyTruth(user.impliesRole(RoleName.ADMIN))
    assertTrueUsingGroovyTruth(user.impliesRole(RoleName.USER))
    assertFalseUsingGroovyTruth(user.impliesRole(RoleName.RELEASE))
    assertFalseUsingGroovyTruth(user.impliesRole(RoleName.RESTRICTED))

    // we test the setRoles method
    user.setRoles([RoleName.USER, RoleName.ADMIN, RoleName.RELEASE])
    assertNotNull(user.save())

    assertEquals('user1', user.username)
    assertEquals(3, user.roles.size())

    // should be user/release/admin
    assertTrueUsingGroovyTruth(user.impliesRole(RoleName.ADMIN))
    assertTrueUsingGroovyTruth(user.impliesRole(RoleName.USER))
    assertTrueUsingGroovyTruth(user.impliesRole(RoleName.RELEASE))
    assertFalseUsingGroovyTruth(user.impliesRole(RoleName.RESTRICTED))

    // we test the setRoles method
    user.setRoles([RoleName.USER, RoleName.RESTRICTED])
    assertNotNull(user.save())

    assertEquals('user1', user.username)
    assertEquals(2, user.roles.size())

    // should be user + restricted
    assertFalseUsingGroovyTruth(user.impliesRole(RoleName.ADMIN))
    assertTrueUsingGroovyTruth(user.impliesRole(RoleName.USER))
    assertFalseUsingGroovyTruth(user.impliesRole(RoleName.RELEASE))
    assertTrueUsingGroovyTruth(user.impliesRole(RoleName.RESTRICTED))

  }

  private void assertTrueUsingGroovyTruth(b)
  {
    assertTrue(toGroovyTruth(b))
  }

  private void assertTrueUsingGroovyTruth(String message, b)
  {
    assertTrue(message, toGroovyTruth(b))
  }

  private void assertFalseUsingGroovyTruth(b)
  {
    assertFalse(toGroovyTruth(b))
  }

  private void assertFalseUsingGroovyTruth(String message, b)
  {
    assertFalse(message, toGroovyTruth(b))
  }

  private boolean toGroovyTruth(b)
  {
    !(!b)
  }
}
