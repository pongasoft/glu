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


package test.agent.api

import org.linkedin.glu.agent.api.MountPoint

/**
 * Test for mount point
 *
 * @author ypujante@linkedin.com
 */
def class TestMountPoint extends GroovyTestCase
{
  void testMountPoint()
  {
    // level1
    MountPoint mp = new MountPoint('container')
    assertEquals('/container', mp.path)
    assertEquals(['', 'container'], mp.pathElements)
    assertEquals(mp, MountPoint.fromPath('/container'))
    assertEquals(mp, MountPoint.fromPath('container'))
    assertTrue(mp.parent.is(MountPoint.ROOT)) 

    // level2
    mp = new MountPoint('i001', mp)
    assertEquals('/container/i001', mp.path)
    assertEquals(['', 'container', 'i001'], mp.pathElements)
    assertEquals(mp, MountPoint.fromPath('/container/i001'))

    // root
    mp = MountPoint.fromPath('/')
    assertEquals('/', mp.path)
    assertEquals([''], mp.pathElements)
    assertTrue(mp.is(MountPoint.ROOT))
  }

  void testErrors()
  {
    shouldFail(IllegalArgumentException) {
      new MountPoint('abc/def')
    }

    shouldFail(IllegalArgumentException) {
      new MountPoint('/')
    }

    shouldFail(IllegalArgumentException) {
      new MountPoint(null)
    }

    shouldFail(IllegalArgumentException) {
      new MountPoint('abc', null)
    }
  }
}
