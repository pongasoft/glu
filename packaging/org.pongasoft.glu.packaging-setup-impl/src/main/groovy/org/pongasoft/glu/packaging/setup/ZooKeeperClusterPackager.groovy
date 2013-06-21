package org.pongasoft.glu.packaging.setup

import org.linkedin.util.io.resource.Resource
import org.pongasoft.glu.provisioner.core.metamodel.AgentMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.FabricMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperClusterMetaModel
import org.pongasoft.glu.provisioner.core.metamodel.ZooKeeperMetaModel

/**
 * @author yan@pongasoft.com  */
public class ZooKeeperClusterPackager extends BasePackager
{
  ZooKeeperClusterMetaModel metaModel

  def createPackage()
  {
    Resource packagePath =
      configure(outputFolder.createRelative("zookeeper-cluster-${metaModel.name}"))

    def zooKeepers = createPackages(packagePath)

    return [
      zooKeeperCluster: new PackagedArtifact(location: packagePath),
      zooKeepers: zooKeepers
    ]
  }

  Collection<PackagedArtifact> createPackages(Resource clusterPackagePath)
  {
    metaModel.zooKeepers.collect { ZooKeeperMetaModel zk ->
      createPackage(clusterPackagePath, zk)
    }
  }

  PackagedArtifact createPackage(Resource clusterPackagePath, ZooKeeperMetaModel zk)
  {
    ensureVersion(zk.version)

    String host = zk.host ?: 'localhost'
    int port = zk.clientPort

    def parts = [packageName]
    parts << host

    if(port != ZooKeeperMetaModel.DEFAULT_CLIENT_PORT)
      parts << port
    String newPackageName = parts.join('-')
    Resource packagePath = clusterPackagePath.createRelative(newPackageName)
    copyInputPackage(packagePath)
    configure(packagePath, zk)
    return new PackagedArtifact(location: packagePath,
                                host: host,
                                port: port)
  }

  /**
   * Configure the cluster itself
   */
  Resource configure(Resource packagePath)
  {
    metaModel.fabrics.values().each { FabricMetaModel fabricMetaModel ->
      def tokens = [
        zooKeeperClusterMetaModel: metaModel,
        fabricMetaModel: fabricMetaModel,
      ]

      tokens[PACKAGER_CONTEXT_KEY] = packagerContext
      tokens[CONFIG_TOKENS_KEY] = [*:metaModel.configTokens]

      tokens[CONFIG_TOKENS_KEY].zkRoot = metaModel.gluMetaModel.zooKeeperRoot
      tokens[CONFIG_TOKENS_KEY].fabric = fabricMetaModel.name

      processConfigs('zookeeper-cluster/fabrics', tokens, packagePath)

      fabricMetaModel.agents.values().each { AgentMetaModel agentMetaModel ->
        tokens.agentMetaModel = agentMetaModel
        tokens[CONFIG_TOKENS_KEY].agent = agentMetaModel.name

        processConfigs('zookeeper-cluster/agents', tokens, packagePath)
      }
    }

    return packagePath
  }

  /**
   * Configure an individual ZooKeeper server in the cluster
   */
  Resource configure(Resource packagePath, ZooKeeperMetaModel zk)
  {
    def tokens = [
      zooKeeperMetaModel: zk,
    ]

    tokens[PACKAGER_CONTEXT_KEY] = packagerContext
    tokens[CONFIG_TOKENS_KEY] = zk.configTokens

    processConfigs('zookeeper-server', tokens, packagePath)

    return packagePath
  }
}