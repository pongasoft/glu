/*
 * Copyright (c) 2010-2010 LinkedIn, Inc
 * Portions Copyright (c) 2011 Yan Pujante
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

package org.linkedin.glu.agent.server

import org.linkedin.glu.agent.rest.resources.AgentConfigResource
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.groovy.util.config.Config
import org.linkedin.util.clock.Timespan
import org.linkedin.util.collections.CollectionsUtils
import org.linkedin.util.lifecycle.Configurable
import org.linkedin.zookeeper.client.IZKClient
import org.linkedin.zookeeper.client.ZKClient
import org.restlet.ext.simple.HttpServerHelper
import org.restlet.routing.Router
import org.restlet.Component
import org.restlet.data.Protocol

/**
 * Factory to create a zk client (check arguments, file and wait for one to be set)
 *
 * @author ypujante@linkedin.com */
class IZKClientFactory implements Configurable
{
  public static final String MODULE = IZKClientFactory.class.getName();
  public static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MODULE);

  public static final String ZK_CONNECT_STRING = 'agent.zkConnectString'
  public static final String ZK_PROPERTIES = 'agent.zkProperties'
  
  def config
  String prefix = 'glu'
  def codec
  String zkConnectString

  private def _restConfig
  private final Object _lock = new Object()

  IZKClient create()
  {
    zkConnectString =
      Config.getOptionalString(config, "${prefix}.${ZK_CONNECT_STRING}".toString(), null)

    if(zkConnectString == 'none')
      return null

    if(!zkConnectString)
    {
      computeZkConnectString()
    }

    if(zkConnectString)
    {
      def zkClient =
        new ZKClient(zkConnectString,
                     Timespan.parse(Config.getOptionalString(config, "${prefix}.agent.zkSessionTimeout".toString(), '5s')),
                     null)

      return zkClient
    }
    else
    {
      return null
    }
  }

  private void computeZkConnectString()
  {
    readFromFile()

    if(!zkConnectString)
    {
      getZkConnectStringFromRest()
    }
  }

  void getZkConnectStringFromRest()
  {
    def server = startRestServer()
    try
    {
      synchronized(_lock)
      {
        while(!_restConfig)
          _lock.wait()
      }
    }
    finally
    {
      server.stop()
    }

    zkConnectString = Config.getRequiredString(_restConfig, "${prefix}.${ZK_CONNECT_STRING}".toString())

    if(zkConnectString)
    {
      log.info "ZooKeeper connection string from rest: ${zkConnectString}"
    }
  }

  def startRestServer()
  {
    def port = Config.getOptionalInt(config, "${prefix}.agent.rest.nonSecure.port".toString(), 12907)

    def component = new Component();
    def server = component.getServers().add(Protocol.HTTP, port);
    def context = component.getContext().createChildContext()
    def router = new Router(context)
    router.attach('/config', AgentConfigResource)
    def attributes = context.getAttributes()
    attributes.put('configurable', this)
    attributes.put('codec', codec)
    component.getDefaultHost().attach(router);
    new HttpServerHelper(server)
    component.start()

    def serverAddress = server.address ?: InetAddress.getLocalHost().canonicalHostName

    log.info "Waiting for ${prefix}.${ZK_CONNECT_STRING} (rest:put:http://${serverAddress}:${server.port})"

    return component
  }

  def void configure(Map config)
  {
    synchronized(_lock)
    {
      _restConfig = config
      _lock.notify()
    }
  }

  /**
   * Starting with 1.7.0 all properties are stored in <code>glu.agent.persistent.properties</code>
   * file. There is no need to have a separate file anymore. We still read it for backward
   * compatibility.
   */
  @Deprecated
  private void readFromFile()
  {
    def zkProperties = Config.getOptionalString(config, "${prefix}.${ZK_PROPERTIES}".toString(), null)
    if(zkProperties)
    {
      zkProperties = new File(zkProperties)
      if(zkProperties.exists())
      {
        def zkConfig = CollectionsUtils.loadProperties(zkProperties)
        zkConnectString = Config.getOptionalString(zkConfig, "${prefix}.${ZK_CONNECT_STRING}".toString(), null)
      }
    }

    if(zkConnectString)
    {
      log.info "ZooKeeper connection string from file: ${zkConnectString}"
    }
  }
}
 