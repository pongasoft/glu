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

package test.provisioner.core.environment

import org.linkedin.glu.provisioner.core.environment.Installation
import org.linkedin.glu.provisioner.core.environment.Environment

/**
 * Tests for the {@link org.linkedin.glu.provisioner.core.environment.Environment} class
 *
 * author:  Riccardo Ferretti
 * created: Aug 18, 2009
 */
public class TestEnvironment extends GroovyTestCase
{
  private static Installation leoServer1 =
    new Installation(hostname: 'mes01.prod', mount: '/leo-server', name: 'leo-server',
                     gluScript: 'ivy:/com.linkedin.glu.scripts/tomcat-server/2.0',
                     props: [skeleton:'ivy:/com.linkedin.servers/leo-server/1.0',
                             debug:false, port:8080])
  private static Installation leoServer2 =
    new Installation(hostname: 'localhost', mount: '/leo-server', name: 'leo-server',
                     gluScript: 'ivy:/com.linkedin.glu.scripts/tomcat-server/2.0',
                     props: [skeleton:'ivy:/com.linkedin.servers/leo-server/1.0',
                             debug:false, port:8080])

  private static Installation container =
    new Installation(hostname: 'mes01.prod', mount: '/core', name: 'core',
                     gluScript: 'ivy:/com.linkedin.glu.scripts/jetty-container/2.3',
                     props: [skeleton:'ivy:/com.linkedin.containers/jetty-container/1.2',
                             debug:false, port:9996, conf:2.3])
  private static Installation mediaBackend =
    new Installation(hostname: 'mes01.prod', mount: '/media-backend', name: 'media-backend',
                     gluScript: 'ivy:/com.linkedin.glu.scripts/generic-service/1.0',
                     props: [skeleton:'ivy:/com.linkedin.media/media-backend/1.0'],
                             parent: container)
  private static Installation mediaAdmin =
    new Installation(hostname: 'mes01.prod', mount: '/media-admin', name: 'media-admin',
                     gluScript: 'ivy:/com.linkedin.glu.scripts/generic-service/1.0',
                     props: [skeleton:'ivy:/com.linkedin.media/media-admin/1.0'],
                             parent: container)

  private static Installation captchaServer =
    new Installation(hostname: 'localhost', mount: '/captcha-server', name: 'captcha-server',
                     gluScript: 'ivy:/com.linkedin.glu.scripts/tomcat-container/2.0',
                     props: [skeleton:'ivy:/com.linkedin.captcha/captcha-server/0.20',
                             debug:false, port:8080])
  private static Installation activemq =
    new Installation(hostname: 'localhost', mount: '/activemq', name: 'activemq',
                     gluScript: 'ivy:/com.linkedin.glu.scripts/generic-server/1.0',
                     props: [artifact: 'ivy:com.linkedin.activemq/activemq/2.0'])

  private static Environment env = new Environment ('test', [leoServer1, leoServer2,
                            activemq, captchaServer,
                            container, mediaAdmin, mediaBackend])



  /**
   * Test filtering the environment by application host.
   */
  public void testFilterByHost()
  {
    // for installations that do not have dependencies
    Environment filtered = env.filterByHost('newEnv', ['localhost'])

    assertEquals(3, filtered.installations.size())
    filtered.installations.each { Installation inst ->
      assertTrue(inst == leoServer2 || inst == captchaServer || inst == activemq)
    }

    // for installations that have children
    Environment f2 = env.filterByHost('newEnv', ['mes01.prod'])

    assertEquals(4, f2.installations.size())
    f2.installations.each { Installation inst ->
      switch (inst.name) {
        case 'core':
          assertEquals(2, inst.children.size())
          assertEquals(container, inst)
          break
        case 'media-backend':
          assertEquals(mediaBackend, inst)
          break
        case 'media-admin':
          assertEquals(mediaAdmin, inst)
          break
        case 'leo-server':
          assertEquals(leoServer1, inst)
          break
        default:
          fail ('unexpected installation')
      }
    }

  }


  /**
   * Test filtering the environment by application name
   */
  public void testFilterByAppname()
  {
    // for installations that do not have dependencies
    Environment f1 = env.filterByApplicationName('newEnv', 'leo-server')

    assertEquals(2, f1.installations.size())
    f1.installations.each { Installation inst ->
      assertTrue(inst == leoServer1 || inst == leoServer2)
    }

    // for installations that have children
    Environment f2 = env.filterByApplicationName('newEnv', 'core')

    assertEquals(1, f2.installations.size())
    f2.installations.each { Installation inst ->
      assertEquals(inst, container)
      assertEquals(0, inst.children.size())
    }

    // for installations that have parent
    Environment f3 = env.filterByApplicationName('newEnv', 'media-admin')

    assertEquals(2, f3.installations.size())
    f3.installations.each { Installation inst ->
      switch (inst.name) {
        case 'core':
          assertEquals(1, inst.children.size())
          assertEquals(container, inst)
          break
        case 'media-admin':
          // assertEquals(mediaAdmin, inst)   this test would fail b/c of the parent
          assertEquals(mediaAdmin.props, inst.props)
          assertEquals(mediaAdmin.gluScript, inst.gluScript)
          assertEquals(mediaAdmin.mount, inst.mount)
          assertEquals(mediaAdmin.hostname, inst.hostname)
          break
        default:
          fail ('unexpected installation')
      }
    }

    // check that we properly deduplicate entries
    // this call should return exactly the same environment as f3
    Environment f4 = env.filterByApplicationName('newEnv', 'media-admin', 'core')

    assertEquals(2, f4.installations.size())
    f4.installations.each { Installation inst ->
      switch (inst.name) {
        case 'core':
          assertEquals(1, inst.children.size())
          assertEquals(container, inst)
          break
        case 'media-admin':
          // assertEquals(mediaAdmin, inst)   this test would fail b/c of the parent
          assertEquals(mediaAdmin.props, inst.props)
          assertEquals(mediaAdmin.gluScript, inst.gluScript)
          assertEquals(mediaAdmin.mount, inst.mount)
          assertEquals(mediaAdmin.hostname, inst.hostname)
          break
        default:
          fail ('unexpected installation')
      }
    }


    // check that we properly deduplicate entries
    Environment f5 = env.filterByApplicationName('newEnv', 'media-admin', 'media-backend')

    assertEquals(3, f5.installations.size())
    f5.installations.each { Installation inst ->
      switch (inst.name) {
        case 'core':
          assertEquals(2, inst.children.size())
          assertEquals(container, inst)
          break
        case 'media-admin':
          // assertEquals(mediaAdmin, inst)   this test would fail b/c of the parent
          assertEquals(mediaAdmin.props, inst.props)
          assertEquals(mediaAdmin.gluScript, inst.gluScript)
          assertEquals(mediaAdmin.mount, inst.mount)
          assertEquals(mediaAdmin.hostname, inst.hostname)
          break
        case 'media-backend':
          // assertEquals(mediaBackend, inst)   this test would fail b/c of the parent
          assertEquals(mediaBackend.props, inst.props)
          assertEquals(mediaBackend.gluScript, inst.gluScript)
          assertEquals(mediaBackend.mount, inst.mount)
          assertEquals(mediaBackend.hostname, inst.hostname)
          break
        default:
          fail ('unexpected installation')
      }
    }
  }


}