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
  Collection<Resource> configsRoots

  boolean dryMode = false

  protected def _sha1s = [:].withDefault { Resource r -> shell.sha1(r) }

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

    Resource location = shell.cp(inputPackage, parent)

    if(location != destination)
      shell.mv(location, destination)

    resolveLinks(destination)

    return destination
  }

  void processConfigs(String fromFolder, Map tokens, Resource toFolder)
  {
    configsRoots.each { configsRoot ->
      processConfigs(configsRoot.createRelative(fromFolder), tokens, toFolder)
    }
  }

  void resolveLinks(Resource destination)
  {
    def out = []
    GroovyIOUtils.eachChildRecurse(inputPackage) { Resource file ->
      if(file.filename.endsWith('.jar.lnk'))
        resolveLink(destination, file, out)
    }

    if(out)
    {
      // optimization => cp forks and makes the process slow => forking once...
      def content = """#!/bin/bash
${out.join('\n')}
"""

      shell.withTempFile { Resource f ->
        shell.saveContent(f, content)
        shell.exec(f)
      }
    }
  }

  void resolveLink(Resource destination, Resource link, def out)
  {
    Resource linkFolder = link.parentResource

    def (linkSourceRelativePath, sha1) = link.file.readLines()

    def linkSource = linkFolder.createRelative(linkSourceRelativePath)

    if(sha1 && sha1 != _sha1s[linkSource])
      throw new IllegalArgumentException("mismatch sha1 for ${linkSource} (expecting ${sha1}, got ${_sha1s[linkSource]})")

    String destinationPath =
      inputPackage.file.toPath().relativize(linkFolder.file.toPath()).toString()

    //shell.cp(linkSource, destination.createRelative(destinationPath))
    out << "cp -R ${linkSource.file.canonicalPath} ${destination.createRelative(destinationPath).file.canonicalPath}"

    //shell.rm(destination.createRelative(destinationPath).createRelative(link.filename))
    out << "rm ${destination.createRelative(destinationPath).createRelative(link.filename).file.canonicalPath}"
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