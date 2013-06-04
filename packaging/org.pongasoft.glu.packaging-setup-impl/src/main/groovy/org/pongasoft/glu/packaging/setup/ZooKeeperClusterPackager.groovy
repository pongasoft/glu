package org.pongasoft.glu.packaging.setup

import org.linkedin.util.io.resource.Resource

/**
 * @author yan@pongasoft.com  */
public class ZooKeeperClusterPackager extends BasePackager
{
  // collection of maps (host, clientPort, quorumPort, leaderElectionPort)
  def zookeperServers = []

  def opts = [
    clientPort: 2181,
  ]

  Collection<PackagedArtifact> createPackages()
  {
    int serverIndex = 1
    zookeperServers.collect { def zk ->
      createPackage(serverIndex++, zk)
    }
  }

  PackagedArtifact createPackage(int serverIndex, def zk)
  {
    String host = zk.host ?: 'localhost'
    int port = (zk.clientPort ?: opts.clientPort) as int
    def newPackageName = "${packageName}-${host}-${port}"
    Resource packagePath = outputFolder.createRelative(newPackageName)
    copyInputPackage(packagePath)
    configure(packagePath, serverIndex, zk)
    return new PackagedArtifact(location: packagePath,
                                host: host,
                                port: port)
  }

  Resource configure(Resource packagePath, int serverIndex, def zk)
  {
    def allServers = []

    if(zookeperServers.size() > 1)
    {
      // adding the id of the server
      processTemplate("/myid.gtmpl",
                      shell.mkdirs(packagePath.createRelative('data')),
                      [id: serverIndex])
      allServers = zookeperServers
    }

    // adding zoo.cfg
    def tokens = [
      opts: opts,
      zk: zk,
      // if only 1 server => no server section at the bottom
      allServers: allServers
    ]

    processTemplate("/zoo.cfg.gtmpl",
                    shell.mkdirs(packagePath.createRelative('conf')),
                    tokens)

    return packagePath
  }
}