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

import grails.test.GrailsUnitTestCase

/**
 * @author ypujante@linkedin.com */
class UserTests extends GrailsUnitTestCase
{
  public void testRoleName()
  {
    assertTrue(RoleName.USER.implies(RoleName.USER))
    assertFalse(RoleName.USER.implies(RoleName.RELEASE))
    assertFalse(RoleName.USER.implies(RoleName.ADMIN))

    assertTrue(RoleName.RELEASE.implies(RoleName.USER))
    assertTrue(RoleName.RELEASE.implies(RoleName.RELEASE))
    assertFalse(RoleName.RELEASE.implies(RoleName.ADMIN))

    assertTrue(RoleName.ADMIN.implies(RoleName.USER))
    assertTrue(RoleName.ADMIN.implies(RoleName.RELEASE))
    assertTrue(RoleName.ADMIN.implies(RoleName.ADMIN))
  }

  public void testPassword()
  {
    def credentials = new DbUserCredentials(username: 'foo')

    credentials.password = "abcd"

    assertTrue(credentials.validatePassword("abcd"))
  }

  public void testPasswordBackwardCompatibility()
  {
    def credentials = new DbUserCredentials(username: 'foo')

    credentials.salt = null
    credentials.oneWayHashPassword = DbUserCredentials.computeOneWayHash("abcd")

    assertTrue(credentials.validatePassword("abcd"))
  }
}
