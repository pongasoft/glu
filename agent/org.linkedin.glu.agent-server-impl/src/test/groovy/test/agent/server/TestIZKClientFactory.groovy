/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Copyright (c) 2011 Yan Pujante
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

package test.agent.server

import org.linkedin.glu.agent.server.IZKClientFactory
import org.restlet.data.Status
import org.linkedin.groovy.util.io.fs.FileSystemImpl
import org.linkedin.util.clock.Timespan
import org.linkedin.util.codec.Codec
import org.linkedin.util.lifecycle.Configurable
import org.linkedin.util.lifecycle.CannotConfigureException
import org.linkedin.groovy.util.concurrent.GroovyConcurrentUtils
import org.linkedin.util.clock.Clock
import org.linkedin.glu.agent.rest.client.ConfigurableFactoryImpl
import org.linkedin.glu.agent.rest.client.ConfigurableFactory
import org.linkedin.util.codec.Base64Codec
import org.linkedin.util.clock.SystemClock

/**
 * @author ypujante@linkedin.com  */
class TestIZKClientFactory extends GroovyTestCase
{
  public static final String MODULE = TestIZKClientFactory.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  FileSystemImpl fs
  Clock clock = SystemClock.INSTANCE

  protected void setUp()
  {
    super.setUp();
    fs = FileSystemImpl.createTempFileSystem()
  }

  protected void tearDown()
  {
    try
    {
      fs.destroy()
    }
    finally
    {
      super.tearDown();
    }
  }

  /**
   * If <code>null</code> then no zookeeper is created
   */
  void testNoZooKeeper()
  {
    Properties p = new Properties()
    p['glu.agent.zkConnectString'] = 'none'
    def factory = new IZKClientFactory(config: p)
    assertNull(factory.create())
  }

  /**
   * If we provide a connect string, it is used and saved
   */
  void testProvidedZooKeeper()
  {
    def zkPropertiesFile = fs.root.'zk.properties'.file
    Properties p = new Properties()
    p['glu.agent.zkConnectString'] = 'localhost:1111'
    p['glu.agent.zkSessionTimeout'] = '11s'
    p['glu.agent.zkProperties'] = zkPropertiesFile.canonicalPath
    assertFalse(zkPropertiesFile.exists())
    def factory = new IZKClientFactory(config: p)
    def client = factory.create()

    assertEquals('localhost:1111', client.factory.connectString)
    assertEquals(Timespan.parse('11s'), client.factory.sessionTimeout)
    assertFalse(zkPropertiesFile.exists())
  }

  /**
   * If a file exists => it is read
   */
  void testSavedZooKeeper()
  {
    // first we store the file
    def zkPropertiesFile = fs.root.'zk.properties'.file
    Properties p = new Properties()
    p['glu.agent.zkConnectString'] = 'localhost:2222'
    zkPropertiesFile.withWriter { p.store(it, null) }

    p = new Properties()
    p['glu.agent.zkSessionTimeout'] = '22s'
    p['glu.agent.zkProperties'] = zkPropertiesFile.canonicalPath

    def factory = new IZKClientFactory(config: p)
    def client = factory.create()

    assertEquals('localhost:2222', client.factory.connectString)
    assertEquals(Timespan.parse('22s'), client.factory.sessionTimeout)
  }

  /**
   * No file => wait for rest call
   */
  void testRestZooKeeper()
  {
    def zkPropertiesFile = fs.root.'zk.properties'.file

    // first we store the file
    Properties p = new Properties()
    p['glu.agent.zkSessionTimeout'] = '33s'
    p['glu.agent.rest.nonSecure.port'] = '9998'
    p['glu.agent.zkProperties'] = zkPropertiesFile.canonicalPath

    Codec codec = new Base64Codec('abc')
    ConfigurableFactory cf = new ConfigurableFactoryImpl(port: 9998, codec: codec)

    def thread1 = Thread.start {
      GroovyConcurrentUtils.waitForCondition(clock, '10s', null) {
        cf.withRemoteConfigurable('localhost') { Configurable c ->
          try
          {
            c.configure(['glu.agent.zkConnectString': 'localhost:3333'])
            return true
          }
          catch (CannotConfigureException e)
          {
            if(log.isDebugEnabled())
              log.debug('ignored exception', e)
            return false
          }
        }
      }
    }

    def client

    def thread2 = Thread.start {
      def factory = new IZKClientFactory(config: p,
                                         codec: codec)
      client = factory.create()
    }

    // we wait for the 1st thread to finish
    thread1.join()

    // then we leave some time to the other thread to finish
    thread2.join(Timespan.parse('5s').durationInMilliseconds)

    // if not finished we interrupt it
    thread2.interrupt()

    assertNotNull('client is null', client)
    assertEquals('localhost:3333', client.factory.connectString)
    assertEquals(Timespan.parse('33s'), client.factory.sessionTimeout)
    assertFalse(zkPropertiesFile.exists())
  }

  /**
   * No file => wait for rest call (this one make sure that without proper checksum, the call fails
   */
  void testForbiddenRestZooKeeper()
  {
    def zkPropertiesFile = fs.root.'zk.properties'.file

    // first we store the file
    Properties p = new Properties()
    p['glu.agent.zkSessionTimeout'] = '33s'
    p['glu.agent.rest.nonSecure.port'] = '9998'
    p['glu.agent.zkProperties'] = zkPropertiesFile.canonicalPath

    Codec codec = new Base64Codec('abc')
    ConfigurableFactory cf = new ConfigurableFactoryImpl(port: 9998, codec: codec)

    def thread1 = Thread.start {
      GroovyConcurrentUtils.waitForCondition(clock, '10s', null) {
        cf.withRemoteConfigurable('localhost') { Configurable c ->
          try
          {
            c.configure(['glu.agent.zkConnectString': 'localhost:3333'])
            return false
          }
          catch (CannotConfigureException e)
          {
            if(log.isDebugEnabled())
              log.debug('ignored exception', e)
            println "[${Status.CLIENT_ERROR_FORBIDDEN.name}]  / [${e.message}]"
            assertTrue(e.message.contains("${Status.CLIENT_ERROR_FORBIDDEN.code}"))
            return true
          }
        }
      }
    }

    def client

    // providing a different crccodec on purpose so that the checksum mismatches!
    def thread2 = Thread.start {
      def factory = new IZKClientFactory(config: p,
                                         codec: new Base64Codec('def'))
      try
      {
        client = factory.create()
        fail("should not be reached")
      }
      catch (InterruptedException e)
      {
        // expected!
      }
    }

    // we wait for the 1st thread to finish
    thread1.join()

    // we interrupt thread2 which will never exit otherwise
    thread2.interrupt()

    assertNull('client should be null', client)
  }
}
