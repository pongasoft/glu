package org.pongasoft.glu.packaging.setup

import org.linkedin.util.io.resource.Resource

/**
 * @author yan@pongasoft.com  */
public class ZooKeeperPackager extends BasePackager
{
  def zookeperServers = [:]

  Collection<Resource> createPackages()
  {
    int serverIndex = 1
    zookeperServers.collect { String host, post ->
      createPackage(serverIndex++, host, post.toString() as int)
    }
  }

  Resource createPackage(int serverIndex, String host, int port)
  {
    def newPackageName = "${packageName}-${host}-${port}"
    Resource packagePath = outputFolder.createRelative(newPackageName)
    return packagePath
  }

}