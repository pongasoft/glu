package org.pongasoft.glu.packaging.setup

import org.linkedin.glu.groovy.utils.io.GluGroovyIOUtils
import org.linkedin.glu.groovy.utils.shell.Shell
import org.linkedin.groovy.util.io.GroovyIOUtils
import org.linkedin.util.io.resource.Resource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author yan@pongasoft.com  */
public class BasePackager
{
  public static final String MODULE = BasePackager.class.getName();
  public static final Logger log = LoggerFactory.getLogger(MODULE);

  public static final String CONFIG_TOKENS_KEY = 'configTokens'
  public static final String PACKAGER_CONTEXT_KEY = 'packagerContext'

  PackagerContext packagerContext

  Resource outputFolder
  Resource inputPackage
  Resource configsRoot

  void ensureVersion(String version)
  {
    if(!inputPackage.filename.endsWith("-${version}"))
      throw new IllegalArgumentException("input package [${inputPackage.filename}] mismatch version [${version}]")
  }

  Shell getShell()
  {
    packagerContext.shell
  }

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

    if(location != destination)
      shell.mv(location, destination)

    return destination
  }

  void processConfigs(String fromFolder, Map tokens, Resource toFolder)
  {
    processConfigs(configsRoot.createRelative(fromFolder), tokens, toFolder)
  }

  void processConfigs(Resource fromFolder, Map tokens, Resource toFolder)
  {
    if(fromFolder?.exists())
    {
      GroovyIOUtils.eachChildRecurse(fromFolder.chroot('.')) { Resource templateOrFile ->
        if(!templateOrFile.isDirectory())
        {
          Resource to =
            (toFolder ?: outputFolder).createRelative(templateOrFile.path)

          if(to.path.contains('@'))
            to = shell.toResource(shell.replaceTokens(to.path, tokens))

          switch(GluGroovyIOUtils.getFileExtension(templateOrFile))
          {
            case 'gtmpl':
            case 'xtmpl':
            case 'ctmpl':
              to = to.parentResource.createRelative(to.filename[0..-7])
              log.debug("processing config templateOrFile: ${templateOrFile}")
              // process templateOrFile (token replacement)
              shell.processTemplate(templateOrFile, to, tokens)
              break

            default:
              // not a templateOrFile => simply copy
              log.debug("copying config file: ${templateOrFile}")
              shell.cp(templateOrFile, to)
              break
          }
        }
      }
    }
  }
}