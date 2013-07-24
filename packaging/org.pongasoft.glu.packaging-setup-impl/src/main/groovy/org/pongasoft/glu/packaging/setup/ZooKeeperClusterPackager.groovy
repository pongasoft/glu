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

  PackagedArtifacts createPackages()
  {
    def parts = ['zookeeper-cluster']

    if(metaModel.name != 'default')
      parts << metaModel.name

    Resource packagePath =
      configure(outputFolder.createRelative(parts.join('-')))

    PackagedArtifacts zooKeepers = createPackages(packagePath)

    return zooKeepers.addArtifact(new PackagedArtifact(location: packagePath,
                                               metaModel: metaModel))
  }

  PackagedArtifacts createPackages(Resource clusterPackagePath)
  {
    def pas = metaModel.zooKeepers.collect { ZooKeeperMetaModel zk ->
      createPackage(clusterPackagePath, zk)
    }

    new PackagedArtifacts(pas)
  }

  PackagedArtifact<ZooKeeperMetaModel> createPackage(Resource clusterPackagePath,
                                                     ZooKeeperMetaModel zk)
  {
    String packageName = ensureVersion(zk.version)

    String host = zk.host ?: 'localhost'
    int port = zk.clientPort

    def parts = [packageName]

    // include host name only when 'real' cluster
    if(zk.zooKeeperCluster.zooKeepers.size() > 1)
      parts << host

    if(port != ZooKeeperMetaModel.DEFAULT_CLIENT_PORT)
      parts << port

    parts << zk.version

    String newPackageName = parts.join('-')
    Resource packagePath = clusterPackagePath.createRelative(newPackageName)
    if(!dryMode)
    {
      copyInputPackage(packagePath)
      configure(packagePath, zk)
    }
    return new PackagedArtifact<ZooKeeperMetaModel>(location: packagePath,
                                                    host: host,
                                                    port: port,
                                                    metaModel: zk)
  }

  /**
   * Configure the cluster itself
   */
  Resource configure(Resource packagePath)
  {
    if(!dryMode)
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
          tokens[CONFIG_TOKENS_KEY].agent = agentMetaModel.resolvedName

          processConfigs('zookeeper-cluster/agents', tokens, packagePath)
        }
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