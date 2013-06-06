/*
 * Copyright (c) 2013 Yan Pujante
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

package org.pongasoft.glu.provisioner.core.metamodel.impl

import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.provisioner.core.metamodel.FabricMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel

/**
 * @author yan@pongasoft.com  */
public class GluMetaModelBuilder
{
  Map<String, FabricMetaModelImpl> fabrics = [:]

  void deserializeFromJsonResource(Resource resource, String fabricName = null)
  {
    if(resource != null)
      deserializeFromJson(GroovyIOUtils.cat(resource), fabricName)
  }

  void deserializeFromJson(String jsonModel, String fabricName = null)
  {
    deserializeFromJson((Map) JsonUtils.fromJSON(jsonModel?.trim()), fabricName)
  }

  void deserializeFromJson(Map jsonModel, String fabricName = null)
  {
    // sanity check... leave it open for future versions
    def metaModelVersion = jsonModel.metaModelVersion ?: GluMetaModelImpl.META_MODEL_VERSION
    if(metaModelVersion != GluMetaModelImpl.META_MODEL_VERSION)
      throw new IllegalArgumentException("unsupported meta model version ${metaModelVersion}")

    // agents
    jsonModel.agents?.each { deserializeAgent(it, fabricName) }

    // consoles
    jsonModel.consoles?.each { deserializeConsole(it, fabricName) }

    // zookeeperClusters
    jsonModel.zooKeeperClusters?.each { deserializeZooKeeperCluster(it, fabricName) }

  }

  /**
   * Deserialize an agent
   */
  void deserializeAgent(Map agentModel, String fabricName = null)
  {
    AgentMetaModelImpl agent =
      deserializeServer(agentModel, new AgentMetaModelImpl(name: agentModel.name))

    if(!agent.name)
      throw new IllegalArgumentException("missing agent name for ${agentModel}")

    def fabric = findOrCreateFabric(agentModel.fabric, fabricName)

    if(fabric.agents.containsKey(agent.name))
      throw new IllegalArgumentException("duplicate agent name [${agent.name}] for fabric [${fabric.name}]")

    // linking the 2
    agent.fabric = fabric
    fabric.agents[agent.name] = agent
  }

  /**
   * Deserialize a console which can handle multiple fabrics
   */
  void deserializeConsole(Map consoleModel, String fabricName = null)
  {
    ConsoleMetaModelImpl console = deserializeServer(consoleModel,
                                                     new ConsoleMetaModelImpl(name: consoleModel.name ?: 'default'))

    // handling fabrics
    Map<String, FabricMetaModel> fabrics = [:]

    def modelFabricNames = consoleModel.fabrics ?: [fabricName]

    modelFabricNames.each { modelFabricName ->
      def fabric = findOrCreateFabric(modelFabricName)

      // linking the 2
      fabrics[modelFabricName] = fabric

      if(fabric.console)
        throw new IllegalArgumentException("console for [${modelFabricName}] already set to [${fabric.console.name}] (trying to set it to [${console.name}])")
      fabric.console = console
    }

    console.fabrics = Collections.unmodifiableMap(fabrics)
  }

  /**
   * ZooKeeper cluster which is made of ZooKeeper servers and represent multiple fabrics
   */
  void deserializeZooKeeperCluster(Map zooKeeperClusterModel, String fabricName = null)
  {
    ZooKeeperClusterMetaModelImpl zooKeeperCluster =
      new ZooKeeperClusterMetaModelImpl(name: zooKeeperClusterModel.name ?: 'default')

    // handle the zookeepers making up the cluster
    def zooKeepers = []
      zooKeeperClusterModel.zooKeepers?.each {
        def zooKeeper = deserializeZooKeeper(it)
        // linking the 2
        zooKeeper.zooKeeperCluster = zooKeeperCluster
        zooKeepers << zooKeeper
      }
    zooKeeperCluster.zooKeepers = Collections.unmodifiableList(zooKeepers)

    // handling the fabrics
    Map<String, FabricMetaModel> fabrics = [:]

    def modelFabricNames = zooKeeperClusterModel.fabrics ?: [fabricName]

    modelFabricNames.each { modelFabricName ->
      def fabric = findOrCreateFabric(modelFabricName)

      // linking the 2
      fabrics[modelFabricName] = fabric

      if(fabric.zooKeeperCluster)
        throw new IllegalArgumentException("zooKeeperCluster for [${modelFabricName}] already set to [${fabric.zooKeeperCluster.name}] (trying to set it to [${zooKeeperCluster.name}])")
      fabric.zooKeeperCluster = zooKeeperCluster
    }

    zooKeeperCluster.fabrics = Collections.unmodifiableMap(fabrics)
  }

  /**
   * A given ZooKeeper server (part of a cluster)
   */
  ZooKeeperMetaModelImpl deserializeZooKeeper(Map zooKeeperModel)
  {
    deserializeServer(zooKeeperModel, new ZooKeeperMetaModelImpl())
  }

  /**
   * @return a fabric (never <code>null</code>)
   */
  private FabricMetaModelImpl findOrCreateFabric(String modelFabricName, String fabricName = null)
  {
    fabricName = computeFabricName(modelFabricName, fabricName)

    FabricMetaModelImpl fabric = fabrics[fabricName]

    if(!fabric)
    {
      fabric = new FabricMetaModelImpl(name: fabricName,
                                       agents: [:])

      fabrics[fabricName] = fabric
    }

    return fabric
  }

  /**
   * Make sure that if a fabric name is provided it is compatible
   */
  public String computeFabricName(String modelFabricName, String fabricName)
  {
    String fn = modelFabricName ?: fabricName

    if(!fn)
      throw new IllegalArgumentException("fabric is required")

    if(fabricName && fn != fabricName)
      throw new IllegalArgumentException("fabric mismatch ${fabricName} != ${fn}")

    return fn
  }

  /**
   * Deserializes the server model piece (super class).
   *
   * @return <code>impl</code>
   */
  private <T extends ServerMetaModelImpl> T deserializeServer(Map serverModel, T impl)
  {
    impl.version = serverModel.version
    impl.host = deserializeHostMetaModel(serverModel.host ?: 'localhost')

    Map<String, Integer> ports = [:]
    if(serverModel.port)
      ports[ServerMetaModelImpl.MAIN_PORT_KEY] = serverModel.port
    if(impl.ports)
      ports.putAll(impl.ports)
    impl.ports = ports

    return impl
  }

  HostMetaModelImpl deserializeHostMetaModel(def host)
  {
    if(host instanceof String || host instanceof GString)
      new PhysicalHostMetaModelImpl(hostAddress: host)
    else
      throw new IllegalArgumentException("unsupported host type [${host}]")
  }

  GluMetaModel toGluMetaModel()
  {
    def newFabrics = fabrics.collectEntries { name, fabric ->
      fabric.agents = Collections.unmodifiableMap(fabric.agents)
      [name, fabric]
    }
    return new GluMetaModelImpl(fabrics: Collections.unmodifiableMap(newFabrics))
  }
}