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

package org.pongasoft.glu.provisioner.core.metamodel.impl.builder

import com.fasterxml.jackson.core.JsonParseException
import org.codehaus.groovy.control.CompilationFailedException
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.groovy.util.json.JsonUtils
import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.provisioner.core.metamodel.GluMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.KeysMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.impl.AgentMetaModelImpl
import org.pongasoft.glu.provisioner.core.metamodel.impl.ConsoleMetaModelImpl
import org.pongasoft.glu.provisioner.core.metamodel.impl.FabricMetaModelImpl
import org.pongasoft.glu.provisioner.core.metamodel.impl.GluMetaModelImpl
import org.pongasoft.glu.provisioner.core.metamodel.impl.HostMetaModelImpl
import org.pongasoft.glu.provisioner.core.metamodel.impl.KeyStoreMetaModelImpl
import org.pongasoft.glu.provisioner.core.metamodel.impl.KeysMetaModelImpl
import org.pongasoft.glu.provisioner.core.metamodel.impl.PhysicalHostMetaModelImpl
import org.pongasoft.glu.provisioner.core.metamodel.impl.ServerMetaModelImpl
import org.pongasoft.glu.provisioner.core.metamodel.impl.ZooKeeperClusterMetaModelImpl
import org.pongasoft.glu.provisioner.core.metamodel.impl.ZooKeeperMetaModelImpl

/**
 * @author yan@pongasoft.com  */
public class GluMetaModelBuilder
{
  GluMetaModelImpl gluMetaModel = new GluMetaModelImpl()
  Map<String, FabricMetaModelImpl> fabrics = [:]
  Map<String, ConsoleMetaModelImpl> consoles = [:]
  Map<String, ZooKeeperClusterMetaModelImpl> zooKeeperClusters = [:]

  void deserializeFromJsonResource(Resource resource)
  {
    if(resource != null)
      deserializeFromJson(GroovyIOUtils.cat(resource))
  }

  void deserializeFromJson(String jsonModel)
  {
    Map jsonMapModel

    try
    {
      jsonMapModel = (Map) JsonUtils.fromJSON(jsonModel?.trim())
    }
    catch(JsonParseException jpe)
    {
      try
      {
        jsonMapModel = GluMetaModelJsonGroovyDsl.parseJsonGroovy(jsonModel)
      }
      catch(CompilationFailedException cfe)
      {
        def ex = new IllegalArgumentException("cannot parse json model")
        ex.addSuppressed(jpe)
        ex.addSuppressed(cfe)
        throw ex
      }
    }

    deserializeFromJsonMap(jsonMapModel)
  }

  void deserializeFromJsonMap(Map jsonModel)
  {
    // sanity check... leave it open for future versions
    def metaModelVersion = jsonModel.metaModelVersion ?: GluMetaModelImpl.META_MODEL_VERSION
    if(metaModelVersion != GluMetaModelImpl.META_MODEL_VERSION)
      throw new IllegalArgumentException("unsupported meta model version ${metaModelVersion}")

    gluMetaModel.gluVersion = jsonModel.gluVersion

    // agents
    jsonModel.agents?.each { deserializeAgent(it) }

    // consoles
    jsonModel.consoles?.each { deserializeConsole(it) }

    // zookeeperClusters
    jsonModel.zooKeeperClusters?.each { deserializeZooKeeperCluster(it) }

    // zooKeeperRoot ?
    if(jsonModel.zooKeeperRoot)
      gluMetaModel.zooKeeperRoot = jsonModel.zooKeeperRoot

    // fabrics
    jsonModel.fabrics?.each { name, fabricModel -> deserializeFabric(name, fabricModel)}
  }

  /**
   * Deserialize a fabric
   */
  void deserializeFabric(String fabricName, Map fabricModel)
  {
    FabricMetaModelImpl fabric = findOrCreateFabric(fabricName)

    // handling console
    if(fabricModel.console)
    {
      if(fabric.console?.name && (fabricModel.console != fabric.console.name))
        throw new IllegalArgumentException("trying to redefine console for fabric [${fabricName}] ([${fabricModel.console}] != [${fabric.console.name}]")

      fabric.console = consoles[fabricModel.console]
      if(!fabric.console)
        throw new IllegalArgumentException("could not find console [${fabricModel.console}] for fabric [${fabricName}]")

      fabric.console.fabrics[fabricName] = fabric
    }

    // handling zooKeeperCluster
    if(fabricModel.zooKeeperCluster)
    {
      if(fabric.zooKeeperCluster?.name && (fabricModel.zooKeeperCluster != fabric.zooKeeperCluster.name))
        throw new IllegalArgumentException("trying to redefine zooKeeperCluster for fabric [${fabricName}] ([${fabricModel.zooKeeperCluster}] != [${fabric.zooKeeperCluster.name}]")

      fabric.zooKeeperCluster = zooKeeperClusters[fabricModel.zooKeeperCluster]
      if(!fabric.zooKeeperCluster)
        throw new IllegalArgumentException("could not find zooKeeperCluster [${fabricModel.zooKeeperCluster}] for fabric [${fabricName}]")

      fabric.zooKeeperCluster.fabrics[fabricName] = fabric
    }

    fabric.keys = deserializeKeys(fabricModel.keys)
  }

  /**
   * Deserialize an agent
   */
  void deserializeAgent(Map agentModel)
  {
    AgentMetaModelImpl agent =
      deserializeServer(agentModel, new AgentMetaModelImpl(name: agentModel.name))

    if(!agent.resolvedName)
      throw new IllegalArgumentException("missing agent name or host for ${agentModel}")

    def fabric = findOrCreateFabric(agentModel.fabric)

    if(fabric.agents.containsKey(agent.resolvedName))
      throw new IllegalArgumentException("duplicate agent name [${agent.resolvedName}] for fabric [${fabric.name}]")

    // linking the 2
    agent.fabric = fabric
    fabric.agents[agent.resolvedName] = agent
  }

  /**
   * Deserialize a console which can handle multiple fabrics
   */
  void deserializeConsole(Map consoleModel)
  {
    ConsoleMetaModelImpl console =
      deserializeServer(consoleModel,
                        new ConsoleMetaModelImpl(name: consoleModel.name ?: 'default',
                                                 fabrics: [:],
                                                 plugins: consoleModel.plugins ?: [],
                                                 externalHost: consoleModel.externalHost,
                                                 internalPath: consoleModel.internalPath,
                                                 externalPath: consoleModel.externalPath))

    if(consoleModel.dataSourceDriverUri)
      console.dataSourceDriverUri = new URI(consoleModel.dataSourceDriverUri)

    consoles[console.name] = console
  }

  /**
   * ZooKeeper cluster which is made of ZooKeeper servers and represent multiple fabrics
   */
  void deserializeZooKeeperCluster(Map zooKeeperClusterModel)
  {
    ZooKeeperClusterMetaModelImpl zooKeeperCluster =
      new ZooKeeperClusterMetaModelImpl(name: zooKeeperClusterModel.name ?: 'default',
                                        fabrics: [:],
                                        gluMetaModel: gluMetaModel)

    // handle the zookeepers making up the cluster
    def zooKeepers = []
    zooKeeperClusterModel.zooKeepers?.each {
      def zooKeeper = deserializeZooKeeper(it)
      // linking the 2
      zooKeeper.zooKeeperCluster = zooKeeperCluster
      zooKeepers << zooKeeper
    }
    zooKeeperCluster.zooKeepers = zooKeepers
    zooKeeperCluster.configTokens = deserializeConfigTokens(zooKeeperClusterModel.configTokens)

    zooKeeperClusters[zooKeeperCluster.name] = zooKeeperCluster
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
  private FabricMetaModelImpl findOrCreateFabric(String modelFabricName)
  {
    FabricMetaModelImpl fabric = fabrics[modelFabricName]

    if(!fabric)
    {
      fabric = new FabricMetaModelImpl(name: modelFabricName,
                                       agents: [:],
                                       gluMetaModel: gluMetaModel)

      fabrics[modelFabricName] = fabric
    }

    return fabric
  }

  /**
   * Deserializes the server model piece (super class).
   *
   * @return <code>impl</code>
   */
  private <T extends ServerMetaModelImpl> T deserializeServer(Map serverModel, T impl)
  {
    impl.version = serverModel.version
    impl.gluMetaModel = gluMetaModel
    impl.host = deserializeHostMetaModel(serverModel.host ?: 'localhost')

    Map<String, Integer> ports = [:]
    if(serverModel.port)
      ports[ServerMetaModelImpl.MAIN_PORT_KEY] = serverModel.port
    if(serverModel.ports)
      ports.putAll(serverModel.ports)
    impl.ports = Collections.unmodifiableMap(ports)
    impl.configTokens = deserializeConfigTokens(serverModel.configTokens)

    return impl
  }

  private Map<String, String> deserializeConfigTokens(Map configTokens)
  {
    if(configTokens == null)
      configTokens = [:]
    return Collections.unmodifiableMap(configTokens)
  }

  HostMetaModelImpl deserializeHostMetaModel(def host)
  {
    if(host instanceof String || host instanceof GString)
      new PhysicalHostMetaModelImpl(hostAddress: host)
    else
      throw new IllegalArgumentException("unsupported host type [${host}]")
  }

  KeysMetaModelImpl deserializeKeys(Map keysModel)
  {
    if(keysModel == null)
      return null

    KeysMetaModelImpl keysMetaModel = new KeysMetaModelImpl()

    [
      'agentKeyStore',
      'agentTrustStore',
      'consoleKeyStore',
      'consoleTrustStore'
    ].each { storeName ->
      keysMetaModel."${storeName}" = deserializeKeyStore(keysModel[storeName])
    }

    return keysMetaModel
  }

  KeyStoreMetaModelImpl deserializeKeyStore(Map keyStoreModel)
  {
    if(keyStoreModel == null)
      return null

    new KeyStoreMetaModelImpl(uri: new URI(keyStoreModel.uri),
                              checksum: keyStoreModel.checksum,
                              storePassword: keyStoreModel.storePassword,
                              keyPassword: keyStoreModel.keyPassword)
  }

  GluMetaModel toGluMetaModel()
  {
    def newFabrics = fabrics.collectEntries { name, fabric ->
      fabric.agents = Collections.unmodifiableMap(fabric.agents)
      [name, fabric]
    }
    consoles.values().each { console ->
      console.fabrics = Collections.unmodifiableMap(console.fabrics)
      // sanity check (only one set of keys per console... may change in the future...)
      if(console.fabrics.size() > 1)
      {
        Map<String, KeysMetaModel> allKeys = console.fabrics.collectEntries { k, v -> [k, v.keys] }
        def fabricsWithDifferentKeys =
          allKeys.groupBy { k, v -> v }.find { it.value.size() > 1}?.value*.key
        if(fabricsWithDifferentKeys)
          throw new IllegalArgumentException("only one set of keys supported (at this time) in a given console [${console.name}]: those fabrics [${fabricsWithDifferentKeys}] have different keys")
      }
    }
    zooKeeperClusters.values().each { zooKeeperCluster ->
      zooKeeperCluster.fabrics = Collections.unmodifiableMap(zooKeeperCluster.fabrics)
      zooKeeperCluster.zooKeepers = Collections.unmodifiableList(zooKeeperCluster.zooKeepers)
    }
    gluMetaModel.fabrics = Collections.unmodifiableMap(newFabrics)

    return gluMetaModel
  }
}