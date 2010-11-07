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

package test.provisioner.impl.planner

import org.linkedin.glu.provisioner.core.environment.Installation
import org.linkedin.glu.provisioner.impl.planner.InstallationsDelta

/**
 * @author ypujante@linkedin.com */
class TestInstallationsDelta extends GroovyTestCase
{
  public void testSameInstallationWithExclude()
  {
    def i1 = new Installation(hostname: 'mes01.prod', mount: '/media',
                              name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.4',
                              props: [:], parent: null)

    def i2 = new Installation(hostname: 'mes01.prod', mount: '/media',
                              name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.5',
                              props: [:], parent: null)

    assertFalse(new InstallationsDelta(i1, i2).isSameInstallation())
    assertTrue(new InstallationsDelta(i1, i2).isSameInstallation(['gluScript']))
  }

  public void testSamePropsWithExclude()
  {
    def i1 = new Installation(hostname: 'mes01.prod', mount: '/media',
                              name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.5',
                              props: [k1: 1], parent: null)

    def i2 = new Installation(hostname: 'mes01.prod', mount: '/media',
                              name: 'media', gluScript: 'ivy:/com.linkedin.media/media-backend/3.5',
                              props: [k1: 2], parent: null)

    assertFalse(new InstallationsDelta(i1, i2).areSameProps())
    assertTrue(new InstallationsDelta(i1, i2).areSameProps(['k1']))
  }
}
