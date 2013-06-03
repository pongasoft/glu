package org.pongasoft.glu.packaging.setup

import org.linkedin.util.io.resource.Resource

/**
 * @author yan@pongasoft.com  */
public class ZooKeeperPackager extends BasePackager
{
  // collection of maps (host, clientPort, quorumPort, leaderElectionPort)
  def zookeperServers = []

  def opts = [
    clientPort: 2181,
  ]

  Collection<Resource> createPackages()
  {
    int serverIndex = 1
    zookeperServers.collect { def zk ->
      createPackage(serverIndex++, zk)
    }
  }

  Resource createPackage(int serverIndex, def zk)
  {
    def newPackageName = "${packageName}-${zk.host ?: 'localhost'}-${zk.clientPort ?: opts.clientPort}"
    Resource packagePath = outputFolder.createRelative(newPackageName)
    copyInputPackage(packagePath)
    configure(packagePath, serverIndex, zk)
    return packagePath
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

  private static String MYID = '${id}'

  private static String ZOO_CFG = '''
# The number of milliseconds of each tick
tickTime=${zk.tickTime ?: opts.tickTime ?: 2000}
# The number of ticks that the initial
# synchronization phase can take
initLimit=${zk.initLimit ?: opts.initLimit ?: 10}
# The number of ticks that can pass between
# sending a request and getting an acknowledgement
syncLimit=${zk.syncLimit ?: opts.syncLimit ?: 5}
# the directory where the snapshot is stored.
dataDir=${zk.dataDir ?: opts.dataDir ?: 'data'}
# the port at which the clients will connect
clientPort=${zk.clientPort ?: opts.clientPort ?: 2181}
<% allServers?.eachWithIndex { server, idx -> %>
server.${idx}=${server.host ?: 'localhost'}:${server.quorumPort ?: opts.quorumPort ?: 2888}:${server.leaderElectionPort ?: opts.leaderElectionPort ?: 3888}
<% } %>
'''
}