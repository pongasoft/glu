package org.pongasoft.glu.packaging.setup

import org.linkedin.util.io.resource.Resource
import org.linkedin.groovy.util.io.fs.FileSystem

/**
 * @author yan@pongasoft.com  */
public class BasePackager
{
  FileSystem fileSystem
  Resource outputFolder
  Resource inputPackage

  String getPackageName()
  {
    def fileName = inputPackage.filename
    if(fileName.endsWith(".tgz"))
      return fileName[0..-5]
    else
      return fileName
  }

  Resource copyInputPackage(Resource destination)
  {

  }
}