package org.pongasoft.glu.packaging.setup

import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.util.io.resource.Resource

/**
 * @author yan@pongasoft.com  */
public class BasePackager
{
  Shell shell
  Resource outputFolder
  Resource inputPackage
  Resource templatesRoot

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
    Resource parent = shell.mkdirs(destination.parentResource)

    def fileName = inputPackage.filename
    Resource location
    if(fileName.endsWith(".tgz"))
    {
      location = shell.untar(inputPackage, parent)
      // should contain 1 directory
      location = location.list()[0]
    }
    else
    {
      location = shell.cp(inputPackage, parent)
    }

    shell.mv(location, destination)

    return destination
  }

  Resource processTemplate(String relativeTemplateName, def output, def tokens)
  {
    shell.processTemplate(templatesRoot.createRelative(relativeTemplateName), output, tokens)
  }

  Resource processOptionalTemplate(String relativeTemplateName, def output, def tokens)
  {
    if(templatesRoot.createRelative(relativeTemplateName).exists())
      processTemplate(relativeTemplateName, output, tokens)
    else
      return null
  }
}